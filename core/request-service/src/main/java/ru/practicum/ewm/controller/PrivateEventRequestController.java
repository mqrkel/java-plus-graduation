package ru.practicum.ewm.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.ewm.dto.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.dto.EventRequestStatusUpdateResult;
import ru.practicum.ewm.dto.ParticipationRequestDto;
import ru.practicum.ewm.service.ParticipationRequestService;

/**
 * Контроллер для работы с заявками на участие в событиях от лица текущего пользователя.
 */
@Slf4j
@RestController
@RequestMapping("/users/{userId}/events/{eventId}/requests")
@RequiredArgsConstructor
@Validated
public class PrivateEventRequestController {
    private final ParticipationRequestService requestService;

    /**
     * Изменяет статус заявок на участие в событии (подтверждение или отклонение).
     *
     * @param userId  ID текущего пользователя
     * @param eventId ID события
     * @param request объект с новыми статусами и списком ID заявок
     * @return результат изменения статусов
     */
    @PatchMapping
    public EventRequestStatusUpdateResult updateRequestStatuses(
            @PathVariable @Min(1) Long userId,
            @PathVariable @Min(1) Long eventId,
            @RequestBody @Valid EventRequestStatusUpdateRequest request) {
        log.debug("PATCH /users/{}/events/{}/requests with body {}", userId, eventId, request);
        return requestService.updateRequestStatuses(userId, eventId, request);
    }

    /**
     * Получает список всех заявок на участие в событии, созданном текущим пользователем.
     *
     * @param userId  ID пользователя
     * @param eventId ID события
     * @return список DTO заявок
     */
    @GetMapping
    public List<ParticipationRequestDto> getRequests(@PathVariable @Min(1) Long userId,
                                                     @PathVariable @Min(1) Long eventId) {
        log.debug("GET /users/{}/events/{}/requests", userId, eventId);
        return requestService.getRequestsForEvent(eventId, userId);
    }
}
