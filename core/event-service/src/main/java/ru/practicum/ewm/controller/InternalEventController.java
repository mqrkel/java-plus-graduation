package ru.practicum.ewm.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import ru.practicum.ewm.dto.EventInternalDto;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.repository.EventRepository;

@RestController
@RequestMapping("/internal/events")
@RequiredArgsConstructor
public class InternalEventController {

    private final EventRepository eventRepository;

    @GetMapping("/exists-by-category")
    public Boolean existsByCategory(@RequestParam Long categoryId) {
        return eventRepository.existsByCategoryId(categoryId);
    }

    @GetMapping("/{eventId}")
    public EventInternalDto getEventById(@PathVariable Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        EventInternalDto dto = new EventInternalDto();
        dto.setId(event.getId());
        dto.setInitiatorId(event.getInitiatorId());
        dto.setState(event.getState());
        dto.setParticipantLimit(event.getParticipantLimit());
        dto.setRequestModeration(event.getRequestModeration());
        return dto;
    }

}