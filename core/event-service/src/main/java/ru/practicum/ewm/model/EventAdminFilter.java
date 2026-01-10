package ru.practicum.ewm.model;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;

import static ru.practicum.ewm.constants.Constants.DATE_TIME_FORMAT;

@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventAdminFilter {

    List<Long> users;
    List<Long> categories;
    List<EventState> states;

    @DateTimeFormat(pattern = DATE_TIME_FORMAT)
    LocalDateTime rangeStart;

    @DateTimeFormat(pattern = DATE_TIME_FORMAT)
    LocalDateTime rangeEnd;

    Long locationId;
    Zone zone;

    @Builder.Default
    Integer from = 0;

    @Builder.Default
    Integer size = 10;

    Pageable pageable;

    public Pageable getPageable() {
        if (pageable == null) {
            Sort sort = Sort.by(Sort.Direction.DESC, "id");
            this.pageable = PageRequest.of(from / size, size, sort);
        }
        return pageable;
    }
}
