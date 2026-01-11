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

    // ========= CREATE =========

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "initiatorId", ignore = true)
    @Mapping(target = "locationId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "publishedOn", ignore = true)
    @Mapping(target = "state", ignore = true)
    @Mapping(target = "confirmedRequests", ignore = true)
    @Mapping(target = "rating", ignore = true)
    Event fromCreateDto(EventCreateDto eventDto);

    // ========= FULL DTO =========

    @Mapping(source = "event.id", target = "id")
    @Mapping(source = "event.annotation", target = "annotation")
    @Mapping(source = "event.title", target = "title")
    @Mapping(source = "event.paid", target = "paid")
    @Mapping(source = "event.eventDate", target = "eventDate")
    @Mapping(source = "event.description", target = "description")
    @Mapping(source = "event.createdAt", target = "createdOn")
    @Mapping(source = "event.publishedOn", target = "publishedOn")
    @Mapping(source = "event.state", target = "state")
    @Mapping(source = "event.confirmedRequests", target = "confirmedRequests")
    @Mapping(source = "event.rating", target = "rating")
    @Mapping(source = "event.participantLimit", target = "participantLimit")
    @Mapping(source = "event.requestModeration", target = "requestModeration")

    @Mapping(source = "category", target = "categoryDto")
    @Mapping(source = "location", target = "locationDto")
    @Mapping(source = "initiator.id", target = "initiator")
    EventDtoOut toDto(Event event,
                      CategoryDtoOut category,
                      UserDtoOut initiator,
                      LocationDto location);

    // удобный оверлоад
    default EventDtoOut toDto(Event event, UserDtoOut initiator, LocationDto location) {
        return toDto(event, null, initiator, location);
    }

    // ========= SHORT DTO =========

    @Mapping(source = "event.id", target = "id")
    @Mapping(source = "event.annotation", target = "annotation")
    @Mapping(source = "event.title", target = "title")
    @Mapping(source = "event.paid", target = "paid")
    @Mapping(source = "event.eventDate", target = "eventDate")
    @Mapping(source = "event.confirmedRequests", target = "confirmedRequests")
    @Mapping(source = "event.rating", target = "rating")

    @Mapping(source = "category", target = "category")
    @Mapping(source = "initiator", target = "initiator")
    EventShortDtoOut toShortDto(Event event,
                                CategoryDtoOut category,
                                UserDtoOut initiator);

    default EventShortDtoOut toShortDto(Event event, UserDtoOut initiator) {
        return toShortDto(event, null, initiator);
    }

    // ========= LOCATION =========

    @Mapping(source = "id", target = "id")
    @Mapping(source = "latitude", target = "latitude")
    @Mapping(source = "longitude", target = "longitude")
    LocationDto toShortLocationDto(LocationFullDtoOut src);
}