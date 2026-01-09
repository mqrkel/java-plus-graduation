package ru.practicum.ewm.location.model;

import lombok.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Getter
@Setter
@Builder
@ToString
@AllArgsConstructor
public class LocationAdminFilter {
    private String text;
    private Long creator;
    private LocationState state;
    private Zone zone;
    private Integer minEvents;
    private Integer maxEvents;

    private Integer offset;
    private Integer limit;

    private Pageable pageable;

    public Pageable getPageable() {
        if (pageable == null) {
            Sort sort = Sort.by(Sort.Direction.ASC, "id");
            this.pageable = PageRequest.of(offset / limit, limit, sort);
        }
        return pageable;
    }
}
