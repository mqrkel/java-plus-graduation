package ru.practicum.ewm.service;

import jakarta.persistence.criteria.Expression;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;
import ru.practicum.ewm.model.Location;
import ru.practicum.ewm.model.LocationState;
import ru.practicum.ewm.model.Zone;

@UtilityClass
public class LocationSpecifications {

    public static Specification<Location> withTextContains(String text) {
        if (text == null || text.isBlank())
            return null;

        return (root, query, cb) ->
                cb.or(
                        cb.like(cb.lower(root.get("name")), "%" + text.toLowerCase() + "%"),
                        cb.like(cb.lower(root.get("address")), "%" + text.toLowerCase() + "%")
                );
    }

    public static Specification<Location> withCreatorId(Long userId) {
        if (userId == null) {
            return null;
        }

        // Если ищем локации без создателя (null)
        if (userId == 0) {
            return (root, query, cb) ->
                    cb.isNull(root.get("creatorId"));
        }

        // Обычный поиск по creatorId
        return (root, query, cb) ->
                cb.equal(root.get("creatorId"), userId);
    }

    public static Specification<Location> withState(LocationState state) {
        if (state == null)
            return null;

        return (root, query, cb) ->
                cb.equal(root.get("state"), state);
    }

    public static Specification<Location> withCoordinates(Zone zone) {
        if (zone == null)
            return null;

        return (root, query, cb) -> {
            Expression<Double> distance = cb.function(
                    "calculate_distance_meters",
                    Double.class,
                    cb.literal(zone.getLatitude()),
                    cb.literal(zone.getLongitude()),
                    root.get("latitude"),
                    root.get("longitude")
            );
            return cb.lessThanOrEqualTo(distance, zone.getRadius());
        };
    }
}
