package ru.practicum.ewm.service;

import java.util.Collection;
import ru.practicum.ewm.dto.EventCreateDto;
import ru.practicum.ewm.dto.EventDtoOut;
import ru.practicum.ewm.dto.EventShortDtoOut;
import ru.practicum.ewm.dto.EventUpdateAdminDto;
import ru.practicum.ewm.dto.EventUpdateDto;
import ru.practicum.ewm.model.EventAdminFilter;
import ru.practicum.ewm.model.EventFilter;

public interface EventService {

    EventDtoOut add(Long userId, EventCreateDto eventDto);

    EventDtoOut update(Long userId, Long eventId, EventUpdateDto updateRequest);

    EventDtoOut update(Long eventId, EventUpdateAdminDto eventDto);

    EventDtoOut findPublished(Long eventId);

    EventDtoOut find(Long userId, Long eventId);

    Collection<EventShortDtoOut> findShortEventsBy(EventFilter filter);

    Collection<EventDtoOut> findFullEventsBy(EventAdminFilter filter);

    Collection<EventShortDtoOut> findByInitiator(Long userId, Integer offset, Integer limit);
}
