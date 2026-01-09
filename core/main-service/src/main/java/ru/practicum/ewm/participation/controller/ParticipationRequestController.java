package ru.practicum.ewm.participation.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.participation.dto.ParticipationRequestDto;
import ru.practicum.ewm.participation.service.ParticipationRequestService;

import java.util.List;

/**
 * REST-контроллер для работы с запросами на участие в событиях пользователя.
 * Все методы работают с URL в формате /users/{userId}/requests
 */
@RestController
@RequiredArgsConstructor
@Validated
public class ParticipationRequestController {

    private final ParticipationRequestService requestService;

    /**
     * Создает новый запрос на участие пользователя в событии.
     *
     * @param userId  ID пользователя, который хочет участвовать
     * @param eventId ID события, на которое подается заявка
     * @return ResponseEntity с DTO созданного запроса и статусом CREATED (201)
     */
    @PostMapping("/users/{userId}/requests")
    public ResponseEntity<ParticipationRequestDto> createRequest(
            @PathVariable Long userId,
            @RequestParam Long eventId) {

        ParticipationRequestDto createdRequest = requestService.createRequest(userId, eventId);
        return new ResponseEntity<>(createdRequest, HttpStatus.CREATED);
    }

    /**
     * Получает список всех запросов на участие пользователя.
     *
     * @param userId ID пользователя
     * @return ResponseEntity со списком DTO заявок и статусом OK (200)
     */
    @GetMapping("/users/{userId}/requests")
    public ResponseEntity<List<ParticipationRequestDto>> getUserRequests(@PathVariable Long userId) {
        List<ParticipationRequestDto> requests = requestService.getUserRequests(userId);
        return ResponseEntity.ok(requests);
    }

    /**
     * Отменяет заявку пользователя на участие в событии.
     *
     * @param userId    ID пользователя
     * @param requestId ID заявки, которую нужно отменить
     * @return ResponseEntity с DTO отмененной заявки и статусом OK (200)
     */
    @PatchMapping("/users/{userId}/requests/{requestId}/cancel")
    public ResponseEntity<ParticipationRequestDto> cancelRequest(
            @PathVariable Long userId,
            @PathVariable Long requestId) {
        ParticipationRequestDto canceledRequest = requestService.cancelRequest(userId, requestId);
        return ResponseEntity.ok(canceledRequest);
    }

}