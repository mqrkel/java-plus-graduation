package ru.practicum.ewm.participation.service;

import ru.practicum.ewm.participation.dto.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.participation.dto.EventRequestStatusUpdateResult;
import ru.practicum.ewm.participation.dto.ParticipationRequestDto;

import java.util.List;

public interface ParticipationRequestService {

    /**
     * Создает запрос на участие пользователя в событии.
     *
     * @param userId  ID пользователя, который хочет подать заявку
     * @param eventId ID события, в котором хотят участвовать
     * @return DTO созданной заявки
     */
    ParticipationRequestDto createRequest(Long userId, Long eventId);

    /**
     * Получает список всех заявок текущего пользователя.
     *
     * @param userId ID пользователя
     * @return список DTO заявок
     */
    List<ParticipationRequestDto> getUserRequests(Long userId);

    List<ParticipationRequestDto> getRequestsForEvent(Long eventId, Long initiatorId);

    /**
     * Отменяет заявку пользователя на участие.
     *
     * @param userId    ID пользователя
     * @param requestId ID заявки на участие
     * @return DTO отменённой заявки
     */
    ParticipationRequestDto cancelRequest(Long userId, Long requestId);

    /**
     * Обновляет статусы заявок (подтверждение или отклонение).
     *
     * @param userId  ID инициатора события
     * @param eventId ID события
     * @param request объект с новыми статусами и списком ID заявок
     * @return результат обработки — список подтверждённых и отклонённых заявок
     */
    EventRequestStatusUpdateResult updateRequestStatuses(Long userId,
                                                         Long eventId,
                                                         EventRequestStatusUpdateRequest request);
}