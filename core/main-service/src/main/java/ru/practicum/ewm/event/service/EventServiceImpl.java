package ru.practicum.ewm.event.service;

import java.util.Objects;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.category.model.Category;
import ru.practicum.ewm.category.repository.CategoryRepository;
import ru.practicum.ewm.event.dto.*;
import ru.practicum.ewm.event.mapper.EventMapper;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.EventAdminFilter;
import ru.practicum.ewm.event.model.EventFilter;
import ru.practicum.ewm.event.model.EventState;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.exception.ConditionNotMetException;
import ru.practicum.ewm.exception.NoAccessException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.location.model.Location;
import ru.practicum.ewm.location.service.LocationService;
import ru.practicum.ewm.participation.model.RequestsCount;
import ru.practicum.ewm.participation.repository.ParticipationRequestRepository;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UserRepository;
import ru.practicum.statsclient.StatsClient;
import ru.practicum.statsclient.StatsClientException;
import ru.practicum.statsdto.StatsDtoOut;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static ru.practicum.ewm.constants.Constants.STATS_EVENTS_URL;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    private static final int MIN_TIME_TO_UNPUBLISHED_EVENT = 2;
    private static final int MIN_TIME_TO_PUBLISHED_EVENT = 1;

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ParticipationRequestRepository requestRepository;

    private final LocationService locationService;

    private final StatsClient statsClient;


    @Override
    @Transactional
    public EventDtoOut add(Long userId, EventCreateDto eventDto) {

        validateEventDate(eventDto.getEventDate(), EventState.PENDING);
        Category category = getCategory(eventDto.getCategoryId());
        User user = getUser(userId);
        Location location = locationService.getOrCreateLocation(eventDto.getLocation());

        Event event = EventMapper.fromDto(eventDto);
        event.setLocation(location);
        event.setCategory(category);
        event.setInitiator(user);

        event = eventRepository.save(event);

        return EventMapper.toDto(event);
    }

    @Override
    @Transactional
    public EventDtoOut update(Long userId, Long eventId, EventUpdateDto eventDto) {

        Event event = getEvent(eventId);

        if (!event.getInitiator().getId().equals(userId)) {
            throw new NoAccessException("Only initiator can edit the event");
        }

        if (event.getState() == EventState.PUBLISHED) {
            throw new ConditionNotMetException("Cannot update published event");
        }

        Optional.ofNullable(eventDto.getTitle()).ifPresent(event::setTitle);
        Optional.ofNullable(eventDto.getAnnotation()).ifPresent(event::setAnnotation);
        Optional.ofNullable(eventDto.getDescription()).ifPresent(event::setDescription);
        Optional.ofNullable(eventDto.getPaid()).ifPresent(event::setPaid);
        Optional.ofNullable(eventDto.getLocation()).ifPresent(loc -> {
            Location location = locationService.getOrCreateLocation(eventDto.getLocation());
            event.setLocation(location);
        });
        Optional.ofNullable(eventDto.getParticipantLimit()).ifPresent(event::setParticipantLimit);
        Optional.ofNullable(eventDto.getRequestModeration()).ifPresent(event::setRequestModeration);

        if (eventDto.getCategoryId() != null
                && !eventDto.getCategoryId().equals(event.getCategory().getId())) {
            Category category = categoryRepository.findById(eventDto.getCategoryId())
                    .orElseThrow(() -> new NotFoundException("Category", eventDto.getCategoryId()));
            event.setCategory(category);
        }

        if (eventDto.getEventDate() != null) {
            validateEventDate(eventDto.getEventDate(), event.getState());
            event.setEventDate(eventDto.getEventDate());
        }

        if (eventDto.getStateAction() != null) {
            switch (eventDto.getStateAction()) {
                case SEND_TO_REVIEW -> event.setState(EventState.PENDING);
                case CANCEL_REVIEW  -> event.setState(EventState.CANCELED);
            }
        }

        Event updated = eventRepository.save(event);
        return EventMapper.toDto(updated);
    }

    @Override
    @Transactional
    public EventDtoOut update(Long eventId, EventUpdateAdminDto eventDto) {

        // дата начала изменяемого события должна быть не ранее чем за час от даты публикации.
        // (Ожидается код ошибки 409)

        Event event = getEvent(eventId);

        Optional.ofNullable(eventDto.getTitle()).ifPresent(event::setTitle);
        Optional.ofNullable(eventDto.getAnnotation()).ifPresent(event::setAnnotation);
        Optional.ofNullable(eventDto.getDescription()).ifPresent(event::setDescription);
        Optional.ofNullable(eventDto.getParticipantLimit()).ifPresent(event::setParticipantLimit);
        Optional.ofNullable(eventDto.getPaid()).ifPresent(event::setPaid);
        Optional.ofNullable(eventDto.getLocation()).ifPresent(loc -> {
            Location location = locationService.getOrCreateLocation(eventDto.getLocation());
            event.setLocation(location);
        });
        Optional.ofNullable(eventDto.getParticipantLimit()).ifPresent(event::setParticipantLimit);
        Optional.ofNullable(eventDto.getRequestModeration()).ifPresent(event::setRequestModeration);

        if (eventDto.getEventDate() != null) {
            validateEventDate(eventDto.getEventDate(), event.getState());
            event.setEventDate(eventDto.getEventDate());
        }

        if (eventDto.getStateAction() != null) {
            switch (eventDto.getStateAction()) {
                case PUBLISH_EVENT -> publishEvent(event);
                case REJECT_EVENT -> rejectEvent(event);
            }
        }

        return EventMapper.toDto(event);
    }

    @Override
    public EventDtoOut findPublished(Long eventId) {

        Event event = eventRepository.findPublishedById(eventId)
                .orElseThrow(() -> new NotFoundException("Event", eventId));

        enrichWithConfirmedRequestsCount(List.of(event));
        enrichWithViewsCount(List.of(event));

        return EventMapper.toDto(event);
    }

    @Override
    public EventDtoOut find(Long userId, Long eventId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User", userId);
        }

        Event event = getEvent(eventId);

        if (!event.getInitiator().getId().equals(userId)) {
            throw new NoAccessException("Only initiator can view this event");
        }

        enrichWithConfirmedRequestsCount(List.of(event));
        enrichWithViewsCount(List.of(event));

        return EventMapper.toDto(event);
    }

    @Override
    public Collection<EventShortDtoOut> findShortEventsBy(EventFilter filter) {
        Specification<Event> spec = buildSpecification(filter);
        return findBy(spec, filter.getPageable()).stream()
                .map(EventMapper::toShortDto)
                .toList();
    }

    @Override
    public Collection<EventDtoOut> findFullEventsBy(EventAdminFilter filter) {
        Specification<Event> spec = buildSpecification(filter);
        return findBy(spec, filter.getPageable()).stream()
                .map(EventMapper::toDto)
                .toList();
    }


    private Collection<Event> findBy(Specification<Event> spec, Pageable pageable) {
        Collection<Event> events = eventRepository.findAll(spec, pageable).getContent();
        enrichWithConfirmedRequestsCount(events);
        enrichWithViewsCount(events);
        return events;
    }

    private Specification<Event> buildSpecification(EventAdminFilter filter) {
        return Stream.of(
                        optionalSpec(EventSpecifications.withUsers(filter.getUsers())),
                        optionalSpec(EventSpecifications.withCategoriesIn(filter.getCategories())),
                        optionalSpec(EventSpecifications.withStatesIn(filter.getStates())),
                        optionalSpec(EventSpecifications.withRangeStart(filter.getRangeStart())),
                        optionalSpec(EventSpecifications.withRangeEnd(filter.getRangeEnd())),
                        optionalSpec(EventSpecifications.withLocationId(filter.getLocationId())),
                        optionalSpec(EventSpecifications.withCoordinates(filter.getZone()))
                )
                .filter(Objects::nonNull)
                .reduce(Specification::and)
                .orElse((root, query, cb) -> cb.conjunction());
    }

    private Specification<Event> buildSpecification(EventFilter filter) {
        return Stream.of(
                        optionalSpec(EventSpecifications.withTextContains(filter.getText())),
                        optionalSpec(EventSpecifications.withCategoriesIn(filter.getCategories())),
                        optionalSpec(EventSpecifications.withPaid(filter.getPaid())),
                        optionalSpec(EventSpecifications.withState(filter.getState())),
                        optionalSpec(EventSpecifications.withLocationId(filter.getLocationId())),
                        optionalSpec(EventSpecifications.withCoordinates(filter.getZone())),
                        optionalSpec(EventSpecifications.withOnlyAvailable(filter.getOnlyAvailable())),
                        optionalSpec(EventSpecifications.withRangeStart(filter.getRangeStart())),
                        optionalSpec(EventSpecifications.withRangeEnd(filter.getRangeEnd()))
                )
                .filter(Objects::nonNull)
                .reduce(Specification::and)
                .orElse((root, query, cb) -> cb.conjunction());
    }

    private static <T> Specification<T> optionalSpec(Specification<T> spec) {
        return spec;
    }

    @Override
    public Collection<EventShortDtoOut> findByInitiator(Long userId, Integer offset, Integer limit) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User", userId);
        }

        Collection<Event> events = eventRepository.findByInitiatorId(userId, offset, limit);
        enrichWithConfirmedRequestsCount(events);
        enrichWithViewsCount(events);

        return events.stream()
                .map(EventMapper::toShortDto)
                .toList();
    }

    private void enrichWithConfirmedRequestsCount(Collection<Event> events) {
        if (events.isEmpty())
            return;

        List<Long> ids = events.stream().map(Event::getId).toList();
        List<RequestsCount> requestsCounts = requestRepository.countConfirmedRequestsForEvents(ids);
        if (requestsCounts.isEmpty())
            return;

        Map<Long, Integer> counts = requestsCounts
                .stream()
                .collect(Collectors.toMap(
                        RequestsCount::getId,
                        RequestsCount::getCount
                ));
        events.forEach(e ->
                e.setConfirmedRequests(counts.getOrDefault(e.getId(), 0))
        );
    }

    private void enrichWithViewsCount(Collection<Event> events) {
        if (events.isEmpty())
            return;

        Map<Long, Integer> hitsMap = getStatistics(events.stream()
                .map(Event::getId)
                .toList());

        if (hitsMap.isEmpty())
            return;

        events.forEach(dto ->
                dto.setViews(hitsMap.getOrDefault(dto.getId(), 0))
        );
    }

    private Map<Long, Integer> getStatistics(Collection<Long> ids) {
        Collection<StatsDtoOut> stats = List.of();
        if (ids.isEmpty())
            return Map.of();

        try {
            stats = statsClient.getStats(
                    LocalDateTime.now().minusYears(10),
                    LocalDateTime.now().plusYears(10),
                    ids.stream().map(id -> STATS_EVENTS_URL + id).toList(),
                    true);
        } catch (StatsClientException ex) {
            log.error(ex.getMessage());
        }

        if (stats.isEmpty())
            return Map.of();

        Map<String, Integer> hits = stats.stream()
                .collect(Collectors.toMap(
                        StatsDtoOut::getUri,
                        StatsDtoOut::getHits
                ));

        return ids.stream()
                .collect(Collectors.toMap(
                        num -> num,
                        num -> hits.getOrDefault(STATS_EVENTS_URL + num, 0)
                ));
    }

    private void validateEventDate(LocalDateTime eventDate, EventState state) {
        if (eventDate == null) {
            throw new IllegalArgumentException("eventDate is null");
        }

        int hours = state == EventState.PUBLISHED
                ? MIN_TIME_TO_PUBLISHED_EVENT
                : MIN_TIME_TO_UNPUBLISHED_EVENT;

        if (eventDate.isBefore(LocalDateTime.now().plusHours(hours))) {
            String message = "The event date must be no earlier than %d hours from the %s time"
                .formatted(hours, state == EventState.PUBLISHED ? "publishing" : "current");
            throw new ConditionNotMetException(message);
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    private Category getCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Category", categoryId));
    }

    @SuppressWarnings("UnusedReturnValue")
    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User", userId));
    }

    @SuppressWarnings("UnusedReturnValue")
    private Event getEvent(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event", eventId));
    }


    private void publishEvent(Event event) {
        if (event.getState() != EventState.PENDING) {
            throw new ConditionNotMetException("Events must be in 'pending' status to be published");
        }

        validateEventDate(event.getEventDate(), EventState.PUBLISHED);

        event.setState(EventState.PUBLISHED);
        event.setPublishedOn(LocalDateTime.now());
    }

    private void rejectEvent(Event event) {
        if (event.getState() == EventState.PUBLISHED) {
            throw new ConditionNotMetException("Published events cannot be rejected");
        }
        event.setState(EventState.CANCELED);
    }
}
