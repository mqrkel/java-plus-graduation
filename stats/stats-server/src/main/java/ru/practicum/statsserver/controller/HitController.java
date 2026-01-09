package ru.practicum.statsserver.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.statsdto.HitDto;
import ru.practicum.statsdto.StatsDtoOut;
import ru.practicum.statsserver.service.HitService;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
public class HitController {

    private final HitService hitService;
    private static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    @PostMapping("/hit")
    @ResponseStatus(HttpStatus.CREATED)
    public void add(@RequestBody @Valid HitDto hitDto) {
        log.debug("request for adding hit: {}", hitDto);
        hitService.add(hitDto);
    }

    @GetMapping("/stats")
    @ResponseStatus(HttpStatus.OK)
    public Collection<StatsDtoOut> select(
            @RequestParam @NotNull @DateTimeFormat(pattern = DATETIME_FORMAT) LocalDateTime start,
            @RequestParam @NotNull @DateTimeFormat(pattern = DATETIME_FORMAT) LocalDateTime end,
            @RequestParam(required = false) ArrayList<String> uris,
            @RequestParam(defaultValue = "false") Boolean unique) {

        log.debug("request for statistics:");
        log.debug(" start date: {}", start);
        log.debug(" end date: {}", end);
        log.debug(" uris: {}", uris);

        return hitService.getStatistics(start, end, uris, unique);
    }
}
