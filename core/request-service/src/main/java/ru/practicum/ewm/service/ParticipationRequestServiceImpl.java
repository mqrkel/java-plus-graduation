package ru.practicum.ewm.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import ru.practicum.ewm.client.EventClient;
import ru.practicum.ewm.client.UserClient;
import ru.practicum.ewm.dto.EventInternalDto;
import ru.practicum.ewm.dto.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.dto.EventRequestStatusUpdateResult;
import ru.practicum.ewm.dto.EventState;
import ru.practicum.ewm.dto.ParticipationRequestDto;
import ru.practicum.ewm.dto.RequestsCountDto;
import ru.practicum.ewm.exception.ConditionNotMetException;
import ru.practicum.ewm.exception.ForbiddenException;
import ru.practicum.ewm.exception.NoAccessException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.ParticipationRequestMapper;
import ru.practicum.ewm.model.ParticipationRequest;
import ru.practicum.ewm.model.RequestStatus;
import ru.practicum.ewm.model.RequestsCount;
import ru.practicum.ewm.repository.ParticipationRequestRepository;

import static ru.practicum.ewm.model.RequestStatus.CANCELED;
import static ru.practicum.ewm.model.RequestStatus.CONFIRMED;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParticipationRequestServiceImpl implements ParticipationRequestService {

    private final UserClient userClient;
    private final EventClient eventClient;
    private final ParticipationRequestRepository requestRepo;
    private final TransactionTemplate transactionTemplate;
    private final ParticipationRequestMapper requestMapper;

    /**
     * Создает запрос на участие пользователя в событии.
     * <p>
     * Внешние HTTP-вызовы (user / event) выполняются вне транзакции,
     * вся работа с БД — внутри TransactionTemplate.
     */
    @Override
    public ParticipationRequestDto createRequest(Long userId, Long eventId) {
        log.debug("Пользователь {} пытается создать запрос участия для события {}", userId, eventId);

        // --- внешние вызовы, без транзакции ---
        validateUserExists(userId);
        EventInternalDto event = getEventById(eventId);

        // --- транзакционный блок только для работы с БД ---
        return transactionTemplate.execute(status -> {
            // проверки, использующие репозиторий
            checkRequestNotExists(userId, eventId);
            checkNotEventInitiator(userId, event);
            checkEventIsPublished(event);
            checkParticipantLimit(event, eventId);

            RequestStatus requestStatus = determineRequestStatus(event);

            ParticipationRequest request = new ParticipationRequest();
            request.setRequesterId(userId);
            request.setEventId(eventId);
            request.setCreated(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS));
            request.setStatus(requestStatus);

            log.debug(
                    "Создана заявка от пользователя {} на событие {} со статусом {}",
                    userId, eventId, requestStatus
            );

            return requestMapper.toDto(requestRepo.save(request));
        });
    }

    /**
     * Получает список всех заявок текущего пользователя.
     * Внешний вызов — вне транзакции, репозиторий сам откроет краткую транзакцию.
     */
    @Override
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        validateUserExists(userId);

        return requestRepo.findAllByRequesterId(userId).stream()
                .map(requestMapper::toDto)
                .toList();
    }

    /**
     * Обновляет статус заявок на участие в событии (подтверждение или отклонение).
     * <p>
     * Внешний вызов в EventService — вне транзакции.
     * Загрузка и изменение заявок + проверки лимитов — внутри TransactionTemplate.
     */
    @Override
    public EventRequestStatusUpdateResult updateRequestStatuses(Long userId,
                                                                Long eventId,
                                                                EventRequestStatusUpdateRequest request) {
        // внешний вызов, без транзакции
        EventInternalDto event = getEventWithCheck(userId, eventId);

        return transactionTemplate.execute(status -> {
            List<ParticipationRequest> requests = getPendingRequestsOrThrow(request.getRequestIds());

            return switch (request.getStatus()) {
                case "CONFIRMED" -> confirmRequests(event, requests);
                case "REJECTED" -> rejectRequests(requests);
                default -> throw new IllegalArgumentException("Incorrect status: " + request.getStatus());
            };
        });
    }

    /**
     * Получает список заявок на участие в событии, созданном указанным пользователем.
     */
    @Override
    public List<ParticipationRequestDto> getRequestsForEvent(Long eventId, Long initiatorId) {
        log.debug("getRequestsForEvent: {} of user: {}", eventId, initiatorId);

        validateUserExists(initiatorId);

        EventInternalDto event = getEventById(eventId);

        if (!event.getInitiatorId().equals(initiatorId)) {
            throw new NoAccessException("Only initiator can view requests of event");
        }

        return requestRepo.findAllByEventId(eventId).stream()
                .map(requestMapper::toDto)
                .toList();
    }

    /**
     * Отменяет заявку пользователя на участие.
     * Только работа с БД — можно оставить обычный @Transactional.
     */
    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        log.debug("Пользователь {} отменяет заявку с ID {}", userId, requestId);

        ParticipationRequest request = requestRepo.findById(requestId)
                .orElseThrow(() -> new NotFoundException("ParticipationRequest", requestId));

        if (!request.getRequesterId().equals(userId)) {
            throw new ForbiddenException("Only the author of the application can cancel it.");
        }

        request.setStatus(CANCELED);
        return requestMapper.toDto(requestRepo.save(request));
    }

    // === Внешние вызовы-helpers (без транзакций) ===

    private void validateUserExists(Long userId) {
        try {
            userClient.getUserById(userId);
        } catch (Exception e) {
            throw new NotFoundException("User", userId);
        }
    }

    private EventInternalDto getEventById(Long eventId) {
        try {
            return eventClient.getEventById(eventId);
        } catch (Exception e) {
            throw new NotFoundException("Event", eventId);
        }
    }

    // === Проверки и работа с БД (вызываются из транзакционных блоков) ===
    // Проверка: заявка уже существует? Если да — кидаем ошибку (не надо дублировать).
    private void checkRequestNotExists(Long userId, Long eventId) {
        if (requestRepo.existsByRequesterIdAndEventId(userId, eventId)) {
            throw new ConditionNotMetException("Participation request has already been sent.");
        }
    }

    // Проверяем, что инициатор события не пытается подать заявку на своё событие (это нечестно).
    private void checkNotEventInitiator(Long userId, EventInternalDto event) {
        if (event.getInitiatorId().equals(userId)) {
            throw new ConditionNotMetException("Initiator cannot participate in their own event.");
        }
    }

    // Проверяем, что событие опубликовано (в смысле — не в черновике и не отменено).
    private void checkEventIsPublished(EventInternalDto event) {
        if (!event.getState().equals(EventState.PUBLISHED)) {
            throw new ConditionNotMetException("Cannot participate in an unpublished event.");
        }
    }

    // Проверяем, не достигнут ли лимит участников события.
    private void checkParticipantLimit(EventInternalDto event, Long eventId) {
        long confirmed = requestRepo.countByEventIdAndStatus(eventId, CONFIRMED);
        if (event.getParticipantLimit() > 0 && confirmed >= event.getParticipantLimit()) {
            throw new ConditionNotMetException("Event participant limit has been reached.");
        }
    }

    // Решаем, будет ли заявка сразу подтверждена или в статусе ожидания (зависит от настроек события).
    private RequestStatus determineRequestStatus(EventInternalDto event) {
        return (!Boolean.TRUE.equals(event.getRequestModeration()) || event.getParticipantLimit() == 0)
                ? RequestStatus.CONFIRMED
                : RequestStatus.PENDING;
    }

    /**
     * Проверяет, что пользователь — инициатор события, и что событие опубликовано.
     * Тут только внешний сервис, без транзакций.
     */
    private EventInternalDto getEventWithCheck(Long userId, Long eventId) {
        EventInternalDto event = getEventById(eventId);

        if (!event.getInitiatorId().equals(userId)) {
            throw new ForbiddenException("The user is not the initiator of the event");
        }

        if (!EventState.PUBLISHED.equals(event.getState())) {
            throw new ConditionNotMetException("The event must be published");
        }

        return event;
    }

    /**
     * Проверяет, что все заявки в статусе PENDING.
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
     */
    private EventRequestStatusUpdateResult confirmRequests(EventInternalDto event, List<ParticipationRequest> requests) {
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
                confirmed.stream().map(requestMapper::toDto).toList(),
                rejected.stream().map(requestMapper::toDto).toList()
        );
    }

    /**
     * Проверяет, достигнут ли лимит участников события, и выбрасывает исключение, если да.
     */
    private void checkIfLimitAvailableOrThrow(EventInternalDto event) {
        int limit = event.getParticipantLimit();
        long confirmedCount = requestRepo.countByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED);
        if (limit != 0 && Boolean.TRUE.equals(event.getRequestModeration()) && confirmedCount >= limit) {
            throw new ConditionNotMetException("The limit of the participants of the event will reach");
        }
    }

    /**
     * Определяет, должна ли заявка подтверждаться автоматически.
     */
    private boolean shouldAutoConfirm(EventInternalDto event) {
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
                requests.stream().map(requestMapper::toDto).toList()
        );
    }

    @Override
    public List<RequestsCountDto> countConfirmedRequestsForEvents(List<Long> eventIds) {
        List<RequestsCount> projections = requestRepo.countConfirmedRequestsForEvents(eventIds);

        return projections.stream()
                .map(p -> {
                    RequestsCountDto dto = new RequestsCountDto();
                    dto.setEventId(p.getId());
                    dto.setCount(p.getCount());
                    return dto;
                })
                .toList();
    }
}