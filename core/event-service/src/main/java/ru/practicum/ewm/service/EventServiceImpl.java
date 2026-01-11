package ru.practicum.ewm.service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import ru.practicum.ewm.client.CategoryClient;
import ru.practicum.ewm.client.LocationClient;
import ru.practicum.ewm.client.RequestClient;
import ru.practicum.ewm.client.UserClient;
import ru.practicum.ewm.dto.CategoryDtoOut;
import ru.practicum.ewm.dto.EventCreateDto;
import ru.practicum.ewm.dto.EventDtoOut;
import ru.practicum.ewm.dto.EventShortDtoOut;
import ru.practicum.ewm.dto.EventUpdateAdminDto;
import ru.practicum.ewm.dto.EventUpdateDto;
import ru.practicum.ewm.dto.LocationAutoRequest;
import ru.practicum.ewm.dto.LocationDto;
import ru.practicum.ewm.dto.LocationFullDtoOut;
import ru.practicum.ewm.dto.LocationState;
import ru.practicum.ewm.dto.RequestsCountDto;
import ru.practicum.ewm.dto.UserDtoOut;
import ru.practicum.ewm.exception.ConditionNotMetException;
import ru.practicum.ewm.exception.NoAccessException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.EventMapper;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.model.EventAdminFilter;
import ru.practicum.ewm.model.EventFilter;
import ru.practicum.ewm.model.EventState;
import ru.practicum.ewm.repository.EventRepository;
import ru.practicum.grpc.ewm.dashboard.message.RecommendedEventProto;
import ru.practicum.statsclient.AnalyzerClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private static final int MIN_TIME_TO_UNPUBLISHED_EVENT = 2;
    private static final int MIN_TIME_TO_PUBLISHED_EVENT = 1;

    private final EventRepository eventRepository;
    private final UserClient userClient;
    private final CategoryClient categoryClient;
    private final RequestClient requestClient;
    private final LocationClient locationClient;
    private final AnalyzerClient analyzerClient;
    private final TransactionTemplate transactionTemplate;
    private final EventMapper eventMapper;

    @Override
    public EventDtoOut add(Long userId, EventCreateDto eventDto) {
        validateEventDate(eventDto.getEventDate(), EventState.PENDING);

        CategoryDtoOut category = getCategoryOrThrow(eventDto.getCategoryId());
        UserDtoOut initiator = getUserOrThrow(userId);
        Long locationId = resolveLocationId(eventDto.getLocation());

        Event event = transactionTemplate.execute(status -> {
            Event e = eventMapper.fromCreateDto(eventDto);
            e.setInitiatorId(userId);
            e.setCategoryId(category.getId());
            e.setLocationId(locationId);
            return eventRepository.save(e);
        });

        if (event == null) {
            throw new IllegalStateException("Failed to save event");
        }

        LocationDto location = loadLocation(event.getLocationId());
        if (location == null && eventDto.getLocation() != null) {
            LocationDto fallback = new LocationDto();
            fallback.setId(event.getLocationId());
            fallback.setLatitude(eventDto.getLocation().getLatitude());
            fallback.setLongitude(eventDto.getLocation().getLongitude());
            location = fallback;
        }

        event.setConfirmedRequests(0);
        event.setRating(0.0);

        return eventMapper.toDto(event, category, initiator, location);
    }

    @Transactional
    @Override
    public EventDtoOut update(Long userId, Long eventId, EventUpdateDto eventDto) {
        CategoryDtoOut newCategory = eventDto.getCategoryId() != null
                ? getCategoryOrThrow(eventDto.getCategoryId())
                : null;

        Event updated = transactionTemplate.execute(status -> {
            Event event = getEvent(eventId);

            if (!event.getInitiatorId().equals(userId)) {
                throw new NoAccessException("Only initiator can edit the event");
            }
            if (event.getState() == EventState.PUBLISHED) {
                throw new ConditionNotMetException("Cannot update published event");
            }

            Optional.ofNullable(eventDto.getTitle()).ifPresent(event::setTitle);
            Optional.ofNullable(eventDto.getAnnotation()).ifPresent(event::setAnnotation);
            Optional.ofNullable(eventDto.getDescription()).ifPresent(event::setDescription);
            Optional.ofNullable(eventDto.getPaid()).ifPresent(event::setPaid);
            Optional.ofNullable(eventDto.getLocationId()).ifPresent(event::setLocationId);
            Optional.ofNullable(eventDto.getParticipantLimit()).ifPresent(event::setParticipantLimit);
            Optional.ofNullable(eventDto.getRequestModeration()).ifPresent(event::setRequestModeration);

            if (newCategory != null) {
                event.setCategoryId(newCategory.getId());
            }

            if (eventDto.getEventDate() != null) {
                validateEventDate(eventDto.getEventDate(), event.getState());
                event.setEventDate(eventDto.getEventDate());
            }

            if (eventDto.getStateAction() != null) {
                switch (eventDto.getStateAction()) {
                    case SEND_TO_REVIEW -> event.setState(EventState.PENDING);
                    case CANCEL_REVIEW -> event.setState(EventState.CANCELED);
                }
            }

            return eventRepository.save(event);
        });

        if (updated == null) {
            throw new IllegalStateException("Transaction returned null event");
        }

        enrichWithConfirmedRequestsCount(List.of(updated));
        enrichWithRating(List.of(updated));

        UserDtoOut initiator = getUserOrThrow(updated.getInitiatorId());
        LocationDto location = loadLocation(updated.getLocationId());
        CategoryDtoOut category = newCategory != null ? newCategory : getCategoryOrThrow(updated.getCategoryId());

        return eventMapper.toDto(updated, category, initiator, location);
    }

    @Transactional
    @Override
    public EventDtoOut update(Long eventId, EventUpdateAdminDto eventDto) {
        CategoryDtoOut newCategory = eventDto.getCategoryId() != null
                ? getCategoryOrThrow(eventDto.getCategoryId())
                : null;

        Event updated = transactionTemplate.execute(status -> {
            Event event = getEvent(eventId);

            Optional.ofNullable(eventDto.getTitle()).ifPresent(event::setTitle);
            Optional.ofNullable(eventDto.getAnnotation()).ifPresent(event::setAnnotation);
            Optional.ofNullable(eventDto.getDescription()).ifPresent(event::setDescription);
            Optional.ofNullable(eventDto.getPaid()).ifPresent(event::setPaid);
            Optional.ofNullable(eventDto.getLocationId()).ifPresent(event::setLocationId);
            Optional.ofNullable(eventDto.getParticipantLimit()).ifPresent(event::setParticipantLimit);
            Optional.ofNullable(eventDto.getRequestModeration()).ifPresent(event::setRequestModeration);

            if (newCategory != null) {
                event.setCategoryId(newCategory.getId());
            }

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

            return eventRepository.save(event);
        });

        if (updated == null) {
            throw new IllegalStateException("Transaction returned null event");
        }

        enrichWithConfirmedRequestsCount(List.of(updated));
        enrichWithRating(List.of(updated));

        UserDtoOut initiator = getUserOrThrow(updated.getInitiatorId());
        LocationDto location = loadLocation(updated.getLocationId());
        CategoryDtoOut category = newCategory != null ? newCategory : getCategoryOrThrow(updated.getCategoryId());

        return eventMapper.toDto(updated, category, initiator, location);
    }

    @Override
    public EventDtoOut findPublished(Long eventId) {
        Event event = eventRepository.findPublishedById(eventId)
                .orElseThrow(() -> new NotFoundException("Event", eventId));

        enrichWithConfirmedRequestsCount(List.of(event));
        enrichWithRating(List.of(event));

        UserDtoOut initiator = getUserOrThrow(event.getInitiatorId());
        LocationDto location = loadLocation(event.getLocationId());
        CategoryDtoOut category = getCategoryOrThrow(event.getCategoryId());

        return eventMapper.toDto(event, category, initiator, location);
    }

    @Override
    public EventDtoOut find(Long userId, Long eventId) {
        getUserOrThrow(userId);

        Event event = getEvent(eventId);
        if (!event.getInitiatorId().equals(userId)) {
            throw new NoAccessException("Only initiator can view this event");
        }

        enrichWithConfirmedRequestsCount(List.of(event));
        enrichWithRating(List.of(event));

        UserDtoOut initiator = getUserOrThrow(event.getInitiatorId());
        LocationDto location = loadLocation(event.getLocationId());

        return eventMapper.toDto(event, initiator, location);
    }

    @Override
    public Collection<EventShortDtoOut> findShortEventsBy(EventFilter filter) {
        Specification<Event> spec = buildSpecification(filter);
        Collection<Event> events = findBy(spec, filter.getPageable());

        Map<Long, UserDtoOut> usersById = loadUsersForEvents(events);
        Map<Long, CategoryDtoOut> categoriesById = loadCategoriesForEvents(events);

        return events.stream()
                .map(event -> {
                    UserDtoOut initiator = usersById.get(event.getInitiatorId());
                    CategoryDtoOut category = categoriesById.get(event.getCategoryId());
                    return eventMapper.toShortDto(event, category, initiator);
                })
                .toList();
    }

    @Override
    public Collection<EventDtoOut> findFullEventsBy(EventAdminFilter filter) {
        Specification<Event> spec = buildSpecification(filter);
        Collection<Event> events = findBy(spec, filter.getPageable());

        Map<Long, UserDtoOut> usersById = loadUsersForEvents(events);
        Map<Long, CategoryDtoOut> categoriesById = loadCategoriesForEvents(events);

        return events.stream()
                .map(event -> {
                    UserDtoOut initiator = usersById.get(event.getInitiatorId());
                    LocationDto location = loadLocation(event.getLocationId());
                    CategoryDtoOut category = categoriesById.get(event.getCategoryId());
                    return eventMapper.toDto(event, category, initiator, location);
                })
                .toList();
    }

    @Override
    public List<EventDtoOut> getRecommendation(Long userId, Long max) {
        log.info("Получен запрос на рекомендации для пользователя");

        List<Long> eventIds = analyzerClient.getRecommendationsForUser(userId, max)
                .map(RecommendedEventProto::getEventId)
                .toList();

        if (eventIds.isEmpty()) {
            return List.of();
        }

        List<Event> events = eventRepository.findAllByIdIn(eventIds);

        enrichWithConfirmedRequestsCount(events);
        enrichWithRating(events);

        Map<Long, Event> byId = events.stream().collect(Collectors.toMap(Event::getId, e -> e));

        List<Event> ordered = eventIds.stream()
                .map(byId::get)
                .filter(Objects::nonNull)
                .toList();

        Map<Long, UserDtoOut> usersById = loadUsersForEvents(ordered);
        Map<Long, CategoryDtoOut> categoriesById = loadCategoriesForEvents(ordered);

        return ordered.stream()
                .map(event -> {
                    UserDtoOut initiator = usersById.get(event.getInitiatorId());
                    CategoryDtoOut category = categoriesById.get(event.getCategoryId());
                    LocationDto location = loadLocation(event.getLocationId());
                    return eventMapper.toDto(event, category, initiator, location);
                })
                .toList();
    }

    @Override
    public Collection<EventShortDtoOut> findByInitiator(Long userId, Integer offset, Integer limit) {
        getUserOrThrow(userId);

        Collection<Event> events = eventRepository.findByInitiatorId(userId, offset, limit);

        enrichWithConfirmedRequestsCount(events);
        enrichWithRating(events);

        Map<Long, UserDtoOut> usersById = loadUsersForEvents(events);

        return events.stream()
                .map(event -> eventMapper.toShortDto(event, usersById.get(event.getInitiatorId())))
                .toList();
    }

    private Collection<Event> findBy(Specification<Event> spec, Pageable pageable) {
        List<Event> events = eventRepository.findAll(spec, pageable).getContent();
        enrichWithConfirmedRequestsCount(events);
        enrichWithRating(events);
        return events;
    }

    private void enrichWithConfirmedRequestsCount(Collection<Event> events) {
        if (events == null || events.isEmpty()) {
            return;
        }

        List<Long> ids = events.stream()
                .map(Event::getId)
                .filter(Objects::nonNull)
                .toList();

        if (ids.isEmpty()) {
            return;
        }

        List<RequestsCountDto> requestsCounts = requestClient.countConfirmedRequestsForEvents(ids);
        if (requestsCounts == null || requestsCounts.isEmpty()) {
            return;
        }

        Map<Long, Integer> counts = requestsCounts.stream()
                .collect(Collectors.toMap(RequestsCountDto::getEventId, RequestsCountDto::getCount));

        events.forEach(e -> e.setConfirmedRequests(counts.getOrDefault(e.getId(), 0)));
    }

    private void enrichWithRating(Collection<Event> events) {
        if (events == null || events.isEmpty()) {
            return;
        }

        List<Long> ids = events.stream()
                .map(Event::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (ids.isEmpty()) {
            return;
        }

        Map<Long, Double> scoreByEventId = analyzerClient.getInteractionsCount(ids)
                .collect(Collectors.toMap(
                        RecommendedEventProto::getEventId,
                        RecommendedEventProto::getScore,
                        (a, b) -> b,
                        HashMap::new
                ));

        events.forEach(e -> e.setRating(scoreByEventId.getOrDefault(e.getId(), 0.0)));
    }

    private Specification<Event> buildSpecification(EventAdminFilter filter) {
        return Stream.of(
                        EventSpecifications.withUsers(filter.getUsers()),
                        EventSpecifications.withCategoriesIn(filter.getCategories()),
                        EventSpecifications.withStatesIn(filter.getStates()),
                        EventSpecifications.withRangeStart(filter.getRangeStart()),
                        EventSpecifications.withRangeEnd(filter.getRangeEnd()),
                        EventSpecifications.withLocationId(filter.getLocationId()),
                        EventSpecifications.withCoordinates(filter.getZone())
                )
                .filter(Objects::nonNull)
                .reduce(Specification::and)
                .orElse((root, query, cb) -> cb.conjunction());
    }

    private Specification<Event> buildSpecification(EventFilter filter) {
        return Stream.of(
                        EventSpecifications.withTextContains(filter.getText()),
                        EventSpecifications.withCategoriesIn(filter.getCategories()),
                        EventSpecifications.withPaid(filter.getPaid()),
                        EventSpecifications.withState(filter.getState()),
                        EventSpecifications.withLocationId(filter.getLocationId()),
                        EventSpecifications.withCoordinates(filter.getZone()),
                        EventSpecifications.withOnlyAvailable(filter.getOnlyAvailable()),
                        EventSpecifications.withRangeStart(filter.getRangeStart()),
                        EventSpecifications.withRangeEnd(filter.getRangeEnd())
                )
                .filter(Objects::nonNull)
                .reduce(Specification::and)
                .orElse((root, query, cb) -> cb.conjunction());
    }

    private void validateEventDate(LocalDateTime eventDate, EventState state) {
        if (eventDate == null) {
            throw new IllegalArgumentException("eventDate is null");
        }

        int hours = state == EventState.PUBLISHED ? MIN_TIME_TO_PUBLISHED_EVENT : MIN_TIME_TO_UNPUBLISHED_EVENT;

        if (eventDate.isBefore(LocalDateTime.now().plusHours(hours))) {
            String message = "The event date must be no earlier than %d hours from the %s time"
                    .formatted(hours, state == EventState.PUBLISHED ? "publishing" : "current");
            throw new ConditionNotMetException(message);
        }
    }

    private CategoryDtoOut getCategoryOrThrow(Long id) {
        try {
            CategoryDtoOut dto = categoryClient.getCategoryById(id);
            if (dto == null) {
                throw new NotFoundException("Category", id);
            }
            return dto;
        } catch (Exception e) {
            throw new NotFoundException("Category", id);
        }
    }

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

    private UserDtoOut getUserOrThrow(Long userId) {
        try {
            UserDtoOut user = userClient.getUserById(userId);
            if (user == null) {
                throw new NotFoundException("User", userId);
            }
            return user;
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new NotFoundException("User", userId);
        }
    }

    private Map<Long, UserDtoOut> loadUsersForEvents(Collection<Event> events) {
        if (events == null || events.isEmpty()) {
            return Map.of();
        }

        List<Long> ids = events.stream()
                .map(Event::getInitiatorId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (ids.isEmpty()) {
            return Map.of();
        }

        try {
            List<UserDtoOut> users = userClient.getUsers(ids);
            return users.stream().collect(Collectors.toMap(UserDtoOut::getId, u -> u));
        } catch (Exception e) {
            log.error("Error fetching users for events: {}", e.getMessage(), e);
            return Map.of();
        }
    }

    private Map<Long, CategoryDtoOut> loadCategoriesForEvents(Collection<Event> events) {
        if (events == null || events.isEmpty()) {
            return Map.of();
        }

        return events.stream()
                .map(Event::getCategoryId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toMap(id -> id, this::getCategoryOrThrow));
    }

    private LocationDto loadLocation(Long locationId) {
        if (locationId == null) {
            return null;
        }
        try {
            LocationFullDtoOut full = locationClient.getLocationById(locationId);
            return full == null ? null : eventMapper.toShortLocationDto(full);
        } catch (Exception e) {
            log.error("Error fetching location {}: {}", locationId, e.getMessage());
            return null;
        }
    }

    private Long resolveLocationId(LocationDto dto) {
        if (dto == null) {
            throw new ConditionNotMetException("Location is required");
        }

        if (dto.getId() != null) {
            LocationFullDtoOut full = locationClient.getLocationById(dto.getId());
            if (full == null) {
                throw new NotFoundException("Location", dto.getId());
            }
            if (!isLocationUsableForEvent(full)) {
                throw new ConditionNotMetException("Location cannot be used for events");
            }
            return full.getId();
        }

        LocationAutoRequest req = LocationAutoRequest.builder()
                .latitude(dto.getLatitude())
                .longitude(dto.getLongitude())
                .build();

        LocationFullDtoOut auto = locationClient.getOrCreateAutoGenerated(req);
        if (auto == null) {
            throw new ConditionNotMetException("Cannot resolve location for given coordinates");
        }
        return auto.getId();
    }

    private boolean isLocationUsableForEvent(LocationFullDtoOut full) {
        return full.getState() == LocationState.APPROVED || full.getState() == LocationState.AUTO_GENERATED;
    }
}