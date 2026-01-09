package ru.practicum.ewm.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Getter
@Setter
@Builder
@ToString
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LocationPrivateFilter {
    String text;
    LocationState state;
    Zone zone;

    Integer offset;
    Integer limit;

    Pageable pageable;

    public Pageable getPageable() {
        if (pageable == null) {
            Sort sort = Sort.by(Sort.Direction.ASC, "id");
            this.pageable = PageRequest.of(offset / limit, limit, sort);
        }
        return pageable;
    }
}
