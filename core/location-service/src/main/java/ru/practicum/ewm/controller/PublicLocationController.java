package ru.practicum.ewm.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import java.util.Collection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.ewm.dto.LocationAutoRequest;
import ru.practicum.ewm.dto.LocationDtoOut;
import ru.practicum.ewm.dto.LocationFullDtoOut;
import ru.practicum.ewm.model.LocationPublicFilter;
import ru.practicum.ewm.model.Zone;
import ru.practicum.ewm.service.LocationService;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/locations")
public class PublicLocationController {

    private final LocationService locationService;

    /**
     * Получить список APPROVED локаций
     * Фильтрация по имени, координатам (с неким радиусом)
     *
     * @return список DTO локаций
     */
    @GetMapping
    public Collection<LocationDtoOut> getAll(
            @RequestParam(required = false) String text,
            @RequestParam(required = false) @DecimalMin("-90.0") @DecimalMax("90.0") Double lat,
            @RequestParam(required = false) @DecimalMin("-180.0") @DecimalMax("180.0") Double lon,
            @RequestParam(defaultValue = "10.0") @DecimalMin("0.0") Double radius,
            @RequestParam(defaultValue = "0") Integer offset,
            @RequestParam(defaultValue = "10") Integer limit) {
        log.debug("request for getting approved locations");
        LocationPublicFilter filter = LocationPublicFilter.builder()
                .text(text)
                .offset(offset)
                .limit(limit)
                .build();

        if (lat != null && lon != null)
            filter.setZone(new Zone(lat, lon, radius));

        return locationService.findAllByFilter(filter);
    }

    @GetMapping("/{id}")
    public LocationFullDtoOut get(@PathVariable @Min(1) Long id) {

        log.debug("request for get location id:{}", id);
        return locationService.getByIdForAdmin(id);
    }

    @PostMapping("/auto")
    public LocationDtoOut getOrCreateAuto(@RequestBody @Valid LocationAutoRequest request) {
        return locationService.getOrCreateAuto(request);
    }
}
