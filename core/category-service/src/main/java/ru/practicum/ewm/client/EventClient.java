package ru.practicum.ewm.client;


import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.ewm.dto.EventInternalDto;

@FeignClient(
        name = "event-service",
        path = "/internal/events"
)
public interface EventClient {

    @GetMapping("/{eventId}")
    EventInternalDto getEventById(@PathVariable("eventId") Long eventId);

    @GetMapping("/exists-by-category")
    Boolean existsByCategoryId(@RequestParam("categoryId") Long categoryId);

}