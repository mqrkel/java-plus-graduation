package ru.practicum.ewm.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.Collection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.ewm.dto.EventCreateDto;
import ru.practicum.ewm.dto.EventDtoOut;
import ru.practicum.ewm.dto.EventShortDtoOut;
import ru.practicum.ewm.dto.EventUpdateDto;
import ru.practicum.ewm.service.EventService;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class PrivateEventController {

    private final EventService eventService;

    // Получение событий, добавленных текущим пользователем
    @GetMapping("/{userId}/events")
    public Collection<EventShortDtoOut> getEventsCreatedByUser(
            @PathVariable @Min(1) Long userId,
            @RequestParam(name = "from", defaultValue = "0") @Min(0) Integer offset,
            @RequestParam(name = "size", defaultValue = "10") @Min(1) Integer limit) {

        log.debug("request from user: get all events created by user id:{}", userId);

        return eventService.findByInitiator(userId, offset, limit);
    }

    // Добавление нового события
    @PostMapping("/{userId}/events")
    @ResponseStatus(HttpStatus.CREATED)
    public EventDtoOut createEvent(@PathVariable @Min(1) Long userId,
                                   @RequestBody @Valid EventCreateDto eventDto) {
        log.debug("request from user: create new event: {}", eventDto);
        return eventService.add(userId, eventDto);
    }

    // Обновление события
    @PatchMapping("/{userId}/events/{eventId}")
    public EventDtoOut updateEvent(
            @PathVariable @Min(1) Long userId,
            @PathVariable @Min(1) Long eventId,
            @RequestBody @Valid EventUpdateDto eventDto) {
        log.debug("request from user: update event: {}", eventDto);
        return eventService.update(userId, eventId, eventDto);
    }

    // Получение события по ID
    @GetMapping("/{userId}/events/{eventId}")
    public EventDtoOut getEventById(@PathVariable @Min(1) Long userId,
                                    @PathVariable @Min(1) Long eventId) {
        log.debug("request from user: get event: {}", eventId);
        return eventService.find(userId, eventId);
    }
}
