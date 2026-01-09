package ru.practicum.ewm.event.model;

import lombok.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import ru.practicum.ewm.location.model.Zone;

import java.time.LocalDateTime;
import java.util.List;

import static ru.practicum.ewm.constants.Constants.DATE_TIME_FORMAT;

@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class EventAdminFilter {

    private List<Long> users;
    private List<Long> categories;
    private List<EventState> states;

    @DateTimeFormat(pattern = DATE_TIME_FORMAT)
    private LocalDateTime rangeStart;

    @DateTimeFormat(pattern = DATE_TIME_FORMAT)
    private LocalDateTime rangeEnd;

    private Long locationId;
    private Zone zone;

    @Builder.Default
    private Integer from = 0;

    @Builder.Default
    private Integer size = 10;

    private Pageable pageable;

    public Pageable getPageable() {
        if (pageable == null) {
            Sort sort = Sort.by(Sort.Direction.DESC, "id");
            this.pageable = PageRequest.of(from / size, size, sort);
        }
        return pageable;
    }
}
