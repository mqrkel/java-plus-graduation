package ru.practicum.ewm.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.ewm.service.ParticipationRequestService;


@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/requests")
public class InternalRequestController {

    private final ParticipationRequestService service;

    public record EventRequestsCountDto(Long eventId, Integer count) {
    }

    @GetMapping("/count-confirmed")
    public List<EventRequestsCountDto> countConfirmed(@RequestParam List<Long> eventIds) {
        return service.countConfirmedRequestsForEvents(eventIds).stream()
                .map(p -> new EventRequestsCountDto(p.getEventId(), p.getCount()))
                .toList();
    }
}