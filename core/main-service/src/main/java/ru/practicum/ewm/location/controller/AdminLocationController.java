package ru.practicum.ewm.location.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.location.dto.LocationCreateDto;
import ru.practicum.ewm.location.dto.LocationFullDtoOut;
import ru.practicum.ewm.location.dto.LocationUpdateAdminDto;
import ru.practicum.ewm.location.model.LocationAdminFilter;
import ru.practicum.ewm.location.model.LocationState;
import ru.practicum.ewm.location.model.Zone;
import ru.practicum.ewm.location.service.LocationService;

import java.util.Collection;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/locations")
public class AdminLocationController {

    private final LocationService locationService;

    /**
     * Создать локацию от имени администратора.
     * Локация автоматически переводится в статус APPROVED
     * @return DTO созданной локаций
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LocationFullDtoOut create(@RequestBody @Valid LocationCreateDto dto) {
        log.debug("request for adding location by admin: {}", dto);
        return locationService.addLocationByAdmin(dto);
    }

    /**
     * Редактировать существующую локацию от имени администратора.
     * @return DTO обновленной локаций
     */
    @PatchMapping("/{id}")
    public LocationFullDtoOut update(@PathVariable @Min(1) Long id,
                                     @RequestBody @Valid LocationUpdateAdminDto dto) {
        log.debug("request for update location id: {} by admin", id);
        return locationService.update(id, dto);
    }

    /**
     * Получить список локаций от имени администратора.
     * Фильтрация по статусу, создателю, количеству мероприятий, имени, координатам (с неким радиусом)
     * @return список DTO локаций
     */
    @GetMapping
    public Collection<LocationFullDtoOut> getAll(
            @RequestParam(required = false) String text,
            @RequestParam(required = false) Long user,
            @RequestParam(required = false) LocationState state,
            @RequestParam(required = false) @DecimalMin("-90.0")  @DecimalMax("90.0")  Double lat,
            @RequestParam(required = false) @DecimalMin("-180.0") @DecimalMax("180.0") Double lon,
            @RequestParam(defaultValue = "10.0") @DecimalMin("0.0") Double radius,
            @RequestParam(required = false) Integer minEvents,
            @RequestParam(required = false) Integer maxEvents,
            @RequestParam(defaultValue = "0") Integer offset,
            @RequestParam(defaultValue = "10") Integer limit) {
        log.debug("request for search locations by admin");
        LocationAdminFilter filter = LocationAdminFilter.builder()
                .text(text)
                .creator(user)
                .state(state)
                .minEvents(minEvents)
                .maxEvents(maxEvents)
                .offset(offset)
                .limit(limit)
                .build();
        if (lat != null && lon != null)
            filter.setZone(new Zone(lat, lon, radius));

        return locationService.findAllByFilter(filter);
    }

    /**
     * Получить локацию по id от имени администратора.
     * @return DTO локации
     */
    @GetMapping("/{id}")
    public LocationFullDtoOut get(@PathVariable @Min(1) Long id) {
        log.debug("request for get location id:{} by admin", id);
        return locationService.getByIdForAdmin(id);
    }

    /**
     * Удалить существующую локацию от имени администратора.
     * Удаляется только локация, не имеющая мероприятий
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable @Min(1) Long id) {
        log.debug("request for delete location id:{} by admin", id);
        locationService.delete(id);
    }
}
