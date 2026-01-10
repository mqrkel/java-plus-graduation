package ru.practicum.ewm.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.model.ParticipationRequest;
import ru.practicum.ewm.model.RequestStatus;
import ru.practicum.ewm.model.RequestsCount;

public interface ParticipationRequestRepository extends JpaRepository<ParticipationRequest, Long> {
    boolean existsByRequesterIdAndEventId(Long userId, Long eventId);

    List<ParticipationRequest> findAllByRequesterId(Long userId);

    List<ParticipationRequest> findAllByEventId(Long eventId);

    Integer countByEventIdAndStatus(Long eventId, RequestStatus status);

    @Query("""
            SELECT pr.eventId as id, COUNT(pr) as count
            FROM ParticipationRequest pr
            WHERE pr.eventId IN :ids AND pr.status = 'CONFIRMED'
            GROUP BY pr.eventId""")
    List<RequestsCount> countConfirmedRequestsForEvents(@Param("ids") List<Long> ids);
}