package ru.practicum.ewm.repository;

import java.util.Collection;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.model.Event;

public interface EventRepository extends
        JpaRepository<Event, Long>,
        JpaSpecificationExecutor<Event> {

    @Query(value = """
            SELECT * FROM events
            WHERE initiator_id = :userId
            ORDER BY id
            LIMIT :limit
            OFFSET :offset
            """, nativeQuery = true)
    Collection<Event> findByInitiatorId(
            @Param("userId") Long userId,
            @Param("offset") int offset,
            @Param("limit") int limit);

    @Query(value = """
            SELECT e FROM Event e
            WHERE e.id = :id AND e.state = 'PUBLISHED'
            """)
    Optional<Event> findPublishedById(@Param("id") Long id);

    boolean existsByCategoryId(Long categoryId);

}
