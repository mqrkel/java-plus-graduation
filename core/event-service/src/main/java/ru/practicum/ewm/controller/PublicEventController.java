package ru.practicum.ewm.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.ewm.dto.EventDtoOut;
import ru.practicum.ewm.dto.EventShortDtoOut;
import ru.practicum.ewm.exception.InvalidRequestException;
import ru.practicum.ewm.model.EventFilter;
import ru.practicum.ewm.model.EventState;
import ru.practicum.ewm.model.Zone;
import ru.practicum.ewm.service.EventService;

import static ru.practicum.ewm.constants.Constants.DATE_TIME_FORMAT;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/events")
public class PublicEventController {

    private final EventService eventService;

    @GetMapping
    public Collection<EventShortDtoOut> getEvents(
            @Size(min = 3, max = 1000, message = "Текст должен быть длиной от 3 до 1000 символов")
            @RequestParam(required = false) String text,
            @RequestParam(required = false) List<Long> categories,
            @RequestParam(required = false) Boolean paid,
            @RequestParam(required = false) @DateTimeFormat(pattern = DATE_TIME_FORMAT) LocalDateTime rangeStart,
            @RequestParam(required = false) @DateTimeFormat(pattern = DATE_TIME_FORMAT) LocalDateTime rangeEnd,
            @RequestParam(defaultValue = "false") Boolean onlyAvailable,
            @RequestParam(required = false) Long location,
            @RequestParam(required = false) @DecimalMin("-90.0") @DecimalMax("90.0") Double lat,
            @RequestParam(required = false) @DecimalMin("-180.0") @DecimalMax("180.0") Double lon,
            @RequestParam(defaultValue = "10.0") @DecimalMin("0.0") Double radius,
            @RequestParam(defaultValue = "EVENT_DATE") String sort,
            @RequestParam(defaultValue = "0") Integer from,
            @RequestParam(defaultValue = "10") Integer size,
            HttpServletRequest request) {

        EventFilter filter = EventFilter.builder()
                .text(text)
                .categories(categories)
                .paid(paid)
                .locationId(location)
                .rangeStart(rangeStart)
                .rangeEnd(rangeEnd)
                .onlyAvailable(onlyAvailable)
                .sort(sort)
                .from(from)
                .size(size)
                .state(EventState.PUBLISHED)
                .build();

        if (lat != null && lon != null) {
            filter.setZone(new Zone(lat, lon, radius));
        }

        if (filter.getRangeStart() != null && filter.getRangeEnd() != null
                && filter.getRangeStart().isAfter(filter.getRangeEnd())) {
            throw new InvalidRequestException("The start date of the range must be earlier than the end date.");
        }

        log.debug("request for getting events (public)");
        return eventService.findShortEventsBy(filter);
    }

    @GetMapping("/{eventId}")
    public EventDtoOut get(@PathVariable @Min(1) Long eventId) {
        log.debug("request for published event id:{}", eventId);
        return eventService.findPublished(eventId);
    }

    @GetMapping("/recommendations")
    public List<EventDtoOut> getRecommendations(@RequestParam Long max,
                                                @RequestHeader("X-EWM-USER-ID") Long userId) {
        log.info("Поступил запрос на получение рекомендаций");
        return eventService.getRecommendation(userId, max);
    }
}