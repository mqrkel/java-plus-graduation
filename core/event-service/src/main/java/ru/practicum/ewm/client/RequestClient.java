package ru.practicum.ewm.client;

import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.ewm.dto.RequestsCountDto;

@FeignClient(
        name = "request-service",
        contextId = "requestsClient",
        path = "/internal/requests"
)
public interface RequestClient {

    @GetMapping("/count-confirmed")
    List<RequestsCountDto> countConfirmedRequestsForEvents(
            @RequestParam("eventIds") List<Long> eventIds
    );
}