package ru.practicum.ewm.participation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.participation.model.ParticipationRequest;
import ru.practicum.ewm.participation.model.RequestStatus;
import ru.practicum.ewm.participation.model.RequestsCount;

import java.util.List;

public interface ParticipationRequestRepository extends JpaRepository<ParticipationRequest, Long> {
    boolean existsByRequesterIdAndEventId(Long userId, Long eventId);

    List<ParticipationRequest> findAllByRequesterId(Long userId);

    List<ParticipationRequest> findAllByEventId(Long eventId);

    Integer countByEventIdAndStatus(Long eventId, RequestStatus status);

    @Query("""
            SELECT pr.event.id as id, COUNT(pr) as count
            FROM ParticipationRequest pr
            WHERE pr.event.id IN :ids AND pr.status = 'CONFIRMED'
            GROUP BY pr.event.id""")
    List<RequestsCount> countConfirmedRequestsForEvents(@Param("ids") List<Long> ids);
}