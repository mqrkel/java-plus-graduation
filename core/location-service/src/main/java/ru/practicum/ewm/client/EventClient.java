package ru.practicum.ewm.client;

import java.util.List;
import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "event-service",
        path = "/admin/events"
)
public interface EventClient {

    @GetMapping
    List<Map<String, Object>> getEventsByLocation(
            @RequestParam("location") Long locationId,
            @RequestParam("offset") Integer offset,
            @RequestParam("limit") Integer limit
    );
}