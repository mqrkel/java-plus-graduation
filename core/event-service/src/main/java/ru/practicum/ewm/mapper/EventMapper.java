package ru.practicum.ewm.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.ewm.dto.CategoryDtoOut;
import ru.practicum.ewm.dto.EventCreateDto;
import ru.practicum.ewm.dto.EventDtoOut;
import ru.practicum.ewm.dto.EventShortDtoOut;
import ru.practicum.ewm.dto.LocationDto;
import ru.practicum.ewm.dto.LocationFullDtoOut;
import ru.practicum.ewm.dto.UserDtoOut;
import ru.practicum.ewm.model.Event;

@Mapper(componentModel = "spring")
public interface EventMapper {

    // === EventCreateDto -> Event ===
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "initiatorId", ignore = true)
    @Mapping(target = "locationId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "publishedOn", ignore = true)
    @Mapping(target = "state", ignore = true)
    @Mapping(target = "confirmedRequests", ignore = true)
    @Mapping(target = "views", ignore = true)
    Event fromCreateDto(EventCreateDto eventDto);

    // === Event -> EventDtoOut (full) ===
    @Mapping(source = "event.id",          target = "id")
    @Mapping(source = "event.annotation",  target = "annotation")
    @Mapping(source = "event.title",       target = "title")
    @Mapping(source = "event.paid",        target = "paid")
    @Mapping(source = "event.eventDate",   target = "eventDate")
    @Mapping(source = "event.description", target = "description")
    @Mapping(source = "event.createdAt",   target = "createdOn")
    @Mapping(source = "event.publishedOn", target = "publishedOn")
    @Mapping(source = "event.state",       target = "state")
    @Mapping(source = "event.confirmedRequests", target = "confirmedRequests")
    @Mapping(source = "event.views",       target = "views")
    @Mapping(source = "event.participantLimit",   target = "participantLimit")
    @Mapping(source = "event.requestModeration",  target = "requestModeration")
    @Mapping(source = "category",          target = "category")
    @Mapping(source = "initiator",         target = "initiator")
    @Mapping(source = "location",          target = "location")
    EventDtoOut toDto(Event event,
                      CategoryDtoOut category,
                      UserDtoOut initiator,
                      LocationDto location);

    // удобный оверлоад без категории
    default EventDtoOut toDto(Event event, UserDtoOut initiator, LocationDto location) {
        return toDto(event, null, initiator, location);
    }

    // === Event -> EventShortDtoOut ===
    @Mapping(source = "event.id",          target = "id")
    @Mapping(source = "event.annotation",  target = "annotation")
    @Mapping(source = "event.title",       target = "title")
    @Mapping(source = "event.paid",        target = "paid")
    @Mapping(source = "event.eventDate",   target = "eventDate")
    @Mapping(source = "event.confirmedRequests", target = "confirmedRequests")
    @Mapping(source = "event.views",       target = "views")
    @Mapping(source = "category",          target = "category")
    @Mapping(source = "initiator",         target = "initiator")
    EventShortDtoOut toShortDto(Event event,
                                CategoryDtoOut category,
                                UserDtoOut initiator);

    // оверлоад без категории
    default EventShortDtoOut toShortDto(Event event, UserDtoOut initiator) {
        return toShortDto(event, null, initiator);
    }

    // === LocationFullDtoOut -> LocationDto ===
    @Mapping(source = "id",        target = "id")
    @Mapping(source = "latitude",  target = "latitude")
    @Mapping(source = "longitude", target = "longitude")
    LocationDto toShortLocationDto(LocationFullDtoOut src);
}