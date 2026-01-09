package ru.practicum.ewm.participation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.EventState;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.exception.ConditionNotMetException;
import ru.practicum.ewm.exception.ForbiddenException;
import ru.practicum.ewm.exception.NoAccessException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.participation.dto.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.participation.dto.EventRequestStatusUpdateResult;
import ru.practicum.ewm.participation.dto.ParticipationRequestDto;
import ru.practicum.ewm.participation.mapper.ParticipationRequestMapper;
import ru.practicum.ewm.participation.model.ParticipationRequest;
import ru.practicum.ewm.participation.model.RequestStatus;
import ru.practicum.ewm.participation.repository.ParticipationRequestRepository;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static ru.practicum.ewm.participation.model.RequestStatus.CANCELED;
import static ru.practicum.ewm.participation.model.RequestStatus.CONFIRMED;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ParticipationRequestServiceImpl implements ParticipationRequestService {

    private final UserRepository userRepo;
    private final EventRepository eventRepo;
    private final ParticipationRequestRepository requestRepo;

    /**
     * Создает запрос на участие пользователя в событии.
     *
     * @param userId  ID пользователя, который хочет подать заявку
     * @param eventId ID события, в котором хотят участвовать
     * @return DTO созданной заявки
     * @throws NotFoundException        если пользователь или событие не найдены
     * @throws ConditionNotMetException если заявка уже существует, или инициатор пытается участвовать в своём событии,
     *                                  или событие не опубликовано, или достигнут лимит участников
     */
    @Transactional
    public ParticipationRequestDto createRequest(Long userId, Long eventId) {
        log.debug("Пользователь {} пытается создать запрос участия для события {}", userId, eventId);

        User user = getUserById(userId);
        Event event = getEventById(eventId);

        checkRequestNotExists(userId, eventId);
        checkNotEventInitiator(userId, event);
        checkEventIsPublished(event);
        checkParticipantLimit(event, eventId);

        RequestStatus status = determineRequestStatus(event);

        ParticipationRequest request = new ParticipationRequest();
        request.setRequester(user);
        request.setEvent(event);
        request.setCreated(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS));
        request.setStatus(status);

        log.debug("Создана заявка от пользователя {} на событие {} со статусом {}", userId, eventId, status);
        return ParticipationRequestMapper.toDto(requestRepo.save(request));
    }

    /**
     * Получает список всех заявок текущего пользователя.
     *
     * @param userId ID пользователя
     * @return список DTO заявок
     * @throws NotFoundException если пользователь не найден
     */
    @Override
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        if (!userRepo.existsById(userId)) {
            throw new NotFoundException("User", userId);
        }
        return requestRepo.findAllByRequesterId(userId).stream()
                .map(ParticipationRequestMapper::toDto)
                .toList();
    }

    /**
     * Обновляет статус заявок на участие в событии (подтверждение или отклонение).
     * <p>
     * Проверяет, что:
     * - пользователь является инициатором события;
     * - событие опубликовано;
     * - все заявки находятся в статусе ожидания;
     * - не превышен лимит участников.
     * </p>
     *
     * @param userId  ID пользователя (инициатора события)
     * @param eventId ID события
     * @param request объект с новыми статусами и списком ID заявок
     * @return результат изменения статусов (подтверждённые и отклонённые заявки)
     * @throws NotFoundException        если событие не найдено
     * @throws ForbiddenException       если пользователь не является инициатором
     * @throws ConditionNotMetException если событие не опубликовано или заявки не в статусе ожидания
     * @throws IllegalArgumentException если передан неверный статус
     */
    @Transactional
    public EventRequestStatusUpdateResult updateRequestStatuses(Long userId,
                                                                Long eventId,
                                                                EventRequestStatusUpdateRequest request) {
        Event event = getEventWithCheck(userId, eventId);

        List<ParticipationRequest> requests = getPendingRequestsOrThrow(request.getRequestIds());

        return switch (request.getStatus()) {
            case "CONFIRMED" -> confirmRequests(event, requests);
            case "REJECTED" -> rejectRequests(requests);
            default -> throw new IllegalArgumentException("Incorrect status: " + request.getStatus());
        };
    }

    /**
     * Получает список заявок на участие в событии, созданном указанным пользователем.
     *
     * @param eventId     ID события
     * @param initiatorId ID пользователя (инициатора события)
     * @return список заявок на участие в событии
     * @throws NotFoundException если событие или пользователь не найдены
     * @throws NoAccessException если пользователь не является инициатором события
     */
    @Override
    public List<ParticipationRequestDto> getRequestsForEvent(Long eventId, Long initiatorId) {
        log.debug("getRequestsForEvent: {} of user: {}", eventId, initiatorId);

        getUserById(initiatorId);
        Event event = getEventById(eventId);

        if (!event.getInitiator().getId().equals(initiatorId)) {
            throw new NoAccessException("Only initiator can view requests of event");
        }

        List<ParticipationRequest> allByEventId = requestRepo.findAllByEventId(eventId);

        return allByEventId.stream()
                .map(ParticipationRequestMapper::toDto)
                .toList();
    }

    /**
     * Отменяет заявку пользователя на участие.
     *
     * @param userId    ID пользователя
     * @param requestId ID заявки на участие
     * @return DTO отменённой заявки
     * @throws NotFoundException  если заявка не найдена
     * @throws ForbiddenException если пользователь не является автором заявки
     */
    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        log.debug("Пользователь {} отменяет заявку с ID {}", userId, requestId);

        ParticipationRequest request = requestRepo.findById(requestId)
                .orElseThrow(() -> new NotFoundException("ParticipationRequest", requestId));

        if (!request.getRequester().getId().equals(userId)) {
            throw new ForbiddenException("Only the author of the application can cancel it.");
        }

        request.setStatus(CANCELED);
        return ParticipationRequestMapper.toDto(requestRepo.save(request));
    }

    // Вспомогательный метод — получаем пользователя из базы, иначе кидаем NotFoundException.
    private User getUserById(Long userId) {
        return userRepo.findById(userId)
                .orElseThrow(() -> new NotFoundException("User", userId));
    }

    // Аналогично, получаем событие из базы или кидаем исключение.
    private Event getEventById(Long eventId) {
        return eventRepo.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event", eventId));
    }

    // Проверка: заявка уже существует? Если да — кидаем ошибку (не надо дублировать).
    private void checkRequestNotExists(Long userId, Long eventId) {
        if (requestRepo.existsByRequesterIdAndEventId(userId, eventId)) {
            throw new ConditionNotMetException("Participation request has already been sent.");
        }
    }

    // Проверяем, что инициатор события не пытается подать заявку на своё событие (это нечестно).
    private void checkNotEventInitiator(Long userId, Event event) {
        if (event.getInitiator().getId().equals(userId)) {
            throw new ConditionNotMetException("Initiator cannot participate in their own event.");
        }
    }

    // Проверяем, что событие опубликовано (в смысле — не в черновике и не отменено).
    private void checkEventIsPublished(Event event) {
        if (!event.getState().equals(EventState.PUBLISHED)) {
            throw new ConditionNotMetException("Cannot participate in an unpublished event.");
        }
    }

    // Проверяем, не достигнут ли лимит участников события.
    private void checkParticipantLimit(Event event, Long eventId) {
        long confirmed = requestRepo.countByEventIdAndStatus(eventId, CONFIRMED);
        if (event.getParticipantLimit() > 0 && confirmed >= event.getParticipantLimit()) {
            throw new ConditionNotMetException("Event participant limit has been reached.");
        }
    }

    // Решаем, будет ли заявка сразу подтверждена или в статусе ожидания (зависит от настроек события).
    private RequestStatus determineRequestStatus(Event event) {
        return (!Boolean.TRUE.equals(event.getRequestModeration()) || event.getParticipantLimit() == 0)
                ? RequestStatus.CONFIRMED
                : RequestStatus.PENDING;
    }

    /**
     * Получает событие по ID и проверяет, что:
     * - пользователь является инициатором;
     * - событие опубликовано.
     *
     * @param userId  ID пользователя
     * @param eventId ID события
     * @return объект события
     * @throws NotFoundException        если событие не найдено
     * @throws ForbiddenException       если пользователь не инициатор
     * @throws ConditionNotMetException если событие не опубликовано
     */
    private Event getEventWithCheck(Long userId, Long eventId) {
        Event event = eventRepo.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event", eventId));

        if (!event.getInitiator().getId().equals(userId)) {
            throw new ForbiddenException("The user is not the initiator of the event");
        }

        if (!EventState.PUBLISHED.equals(event.getState())) {
            throw new ConditionNotMetException("The event must be published");
        }

        return event;
    }

    /**
     * Проверяет, что все заявки находятся в статусе ожидания (PENDING).
     *
     * @param requestIds список ID заявок
     * @return список найденных заявок
     * @throws ConditionNotMetException если хотя бы одна заявка не в статусе PENDING
     */
    private List<ParticipationRequest> getPendingRequestsOrThrow(List<Long> requestIds) {
        List<ParticipationRequest> requests = requestRepo.findAllById(requestIds);
        boolean hasNonPending = requests.stream()
                .anyMatch(r -> r.getStatus() != RequestStatus.PENDING);

        if (hasNonPending) {
            throw new ConditionNotMetException("Request must have status PENDING");
        }

        return requests;
    }

    /**
     * Подтверждает заявки на участие, если не превышен лимит участников.
     * Если лимит достигнут — остальные заявки отклоняются.
     *
     * @param event    событие, к которому относятся заявки
     * @param requests список заявок
     * @return результат обработки заявок
     */
    private EventRequestStatusUpdateResult confirmRequests(Event event, List<ParticipationRequest> requests) {
        checkIfLimitAvailableOrThrow(event);

        int limit = event.getParticipantLimit();
        long confirmedCount = requestRepo.countByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED);
        int available = limit - (int) confirmedCount;

        List<ParticipationRequest> confirmed = new ArrayList<>();
        List<ParticipationRequest> rejected = new ArrayList<>();

        for (ParticipationRequest request : requests) {
            if (shouldAutoConfirm(event)) {
                confirmRequest(request, confirmed);
            } else if (available > 0) {
                confirmRequest(request, confirmed);
                available--;
            } else {
                rejectRequest(request, rejected);
            }
        }

        requestRepo.saveAll(requests);

        return new EventRequestStatusUpdateResult(
                confirmed.stream().map(ParticipationRequestMapper::toDto).toList(),
                rejected.stream().map(ParticipationRequestMapper::toDto).toList()
        );
    }

    /**
     * Проверяет, достигнут ли лимит участников события, и выбрасывает исключение, если да.
     */
    private void checkIfLimitAvailableOrThrow(Event event) {
        int limit = event.getParticipantLimit();
        long confirmedCount = requestRepo.countByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED);
        if (limit != 0 && Boolean.TRUE.equals(event.getRequestModeration()) && confirmedCount >= limit) {
            throw new ConditionNotMetException("The limit of the participants of the event will reach");
        }
    }

    /**
     * Определяет, должна ли заявка подтверждаться автоматически.
     */
    private boolean shouldAutoConfirm(Event event) {
        return event.getParticipantLimit() == 0 || Boolean.FALSE.equals(event.getRequestModeration());
    }

    /**
     * Подтверждает заявку и добавляет её в список подтверждённых.
     */
    private void confirmRequest(ParticipationRequest request, List<ParticipationRequest> confirmed) {
        request.setStatus(RequestStatus.CONFIRMED);
        confirmed.add(request);
    }

    /**
     * Отклоняет заявку и добавляет её в список отклонённых.
     */
    private void rejectRequest(ParticipationRequest request, List<ParticipationRequest> rejected) {
        request.setStatus(RequestStatus.REJECTED);
        rejected.add(request);
    }


    /**
     * Массово отклоняет все переданные заявки.
     *
     * @param requests список заявок
     * @return результат с отклонёнными заявками
     */
    private EventRequestStatusUpdateResult rejectRequests(List<ParticipationRequest> requests) {
        for (ParticipationRequest r : requests) {
            r.setStatus(RequestStatus.REJECTED);
        }

        requestRepo.saveAll(requests);

        return new EventRequestStatusUpdateResult(
                List.of(),
                requests.stream().map(ParticipationRequestMapper::toDto).toList()
        );
    }

}