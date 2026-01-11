package analyzer.service.impl;

import analyzer.model.Recommendation;
import analyzer.repository.EventSimilarityRepository;
import analyzer.repository.UserActionRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.grpc.ewm.dashboard.message.InteractionsCountRequestProto;
import ru.practicum.grpc.ewm.dashboard.message.RecommendedEventProto;
import ru.practicum.grpc.ewm.dashboard.message.SimilarEventsRequestProto;
import ru.practicum.grpc.ewm.dashboard.message.UserPredictionsRequestProto;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class RecommendationsService implements analyzer.service.RecommendationsService {

    UserActionRepository userActionRepository;
    EventSimilarityRepository eventSimilarityRepository;

    @Override
    public List<RecommendedEventProto> getRecommendationsForUser(UserPredictionsRequestProto request) {
        long userId = request.getUserId();
        int limit = (int) request.getMaxResult();
        if (limit <= 0) {
            return List.of();
        }

        log.info("Запрос персонализированных рекомендаций для userId={}, limit={}", userId, limit);

        Pageable recentInteractionsPageable = PageRequest.of(0, limit);
        List<Long> recentEventIds = userActionRepository.findRecentEventIdsByUserId(userId, recentInteractionsPageable);

        if (recentEventIds.isEmpty()) {
            log.warn("Для userId={} не найдено недавних действий.", userId);
            return List.of();
        }

        // -- Найти похожие новые события, исключая все, что пользователь уже видел.
        Set<Long> allUserEvents = userActionRepository.findEventIdsByUserId(userId);

        Pageable candidatesPageable = PageRequest.of(0, limit);
        List<Recommendation> candidateRecs = eventSimilarityRepository.findTopSimilarToSetExcluding(
                recentEventIds,
                allUserEvents,
                candidatesPageable
        );
        Set<Long> candidateEventIds = candidateRecs.stream().map(Recommendation::getEventId).collect(Collectors.toSet());

        if (candidateEventIds.isEmpty()) {
            log.warn("Не найдено новых кандидатов для рекомендаций для userId={}", userId);
            return List.of();
        }

        // -- найти ближайших просмотренных соседей для всех кандидатов.
        Map<Long, List<Recommendation>> neighboursMap = eventSimilarityRepository.findNeighbourEventsFrom(
                candidateEventIds,
                allUserEvents,
                limit
        );

        // -- Получить оценки пользователя для всех найденных соседей
        Set<Long> allNeighbourIds = neighboursMap.values().stream()
                .flatMap(List::stream)
                .map(Recommendation::getEventId)
                .collect(Collectors.toSet());
        Map<Long, Double> userRatings = userActionRepository.findWeightsByUserIdAndEventIds(userId, allNeighbourIds);

        // -- Вычисляем финальный score
        List<RecommendedEventProto> finalRecommendations =
                buildFinalRecommendations(candidateEventIds, neighboursMap, userRatings, limit);

        log.info("Сформировано {} рекомендаций для userId={}", finalRecommendations.size(), userId);
        return finalRecommendations;
    }

    @Override
    public List<RecommendedEventProto> getSimilarEvents(SimilarEventsRequestProto request) {
        long eventId = request.getEventId();
        long userId = request.getUserId();
        int limit = (int) request.getMaxResult();
        if (limit <= 0) {
            return List.of();
        }

        log.info("Запрос похожих событий для eventId={}, исключая для userId={}, limit={}", eventId, userId, limit);

        Set<Long> seenEventIds = new HashSet<>(userActionRepository.findEventIdsByUserId(userId));
        seenEventIds.add(eventId);

        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "score"));
        List<Recommendation> similarEvents = eventSimilarityRepository.findTopSimilarExcluding(
                eventId,
                seenEventIds,
                pageable
        );

        log.info("Найдено {} похожих событий для eventId={}", similarEvents.size(), eventId);

        return similarEvents.stream()
                .map(rec -> RecommendedEventProto.newBuilder()
                        .setEventId(rec.getEventId())
                        .setScore(rec.getScore().floatValue())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<RecommendedEventProto> getInteractionsCount(InteractionsCountRequestProto request) {
        List<Long> eventIds = request.getEventIdList();
        if (eventIds.isEmpty()) {
            return List.of();
        }

        log.info("Запрос суммы весов взаимодействий для {} событий", eventIds.size());

        Map<Long, Double> eventWeights = userActionRepository.getAggregatedWeightsForEvents(eventIds);

        return eventIds.stream()
                .map(eventId -> RecommendedEventProto.newBuilder()
                        .setEventId(eventId)
                        .setScore(eventWeights.getOrDefault(eventId, 0.0).floatValue())
                        .build())
                .sorted(Comparator.comparing(RecommendedEventProto::getScore).reversed())
                .collect(Collectors.toList());
    }

    private List<RecommendedEventProto> buildFinalRecommendations(Set<Long> candidateEventIds,
                                                                  Map<Long, List<Recommendation>> neighboursMap,
                                                                  Map<Long, Double> userRatings,
                                                                  int limit) {
        List<RecommendedEventProto> result = new ArrayList<>();

        for (Long candidateId : candidateEventIds) {
            float score = calculateCandidateScore(candidateId, neighboursMap, userRatings);
            if (score <= 0.0f) {
                continue;
            }
            result.add(RecommendedEventProto.newBuilder()
                    .setEventId(candidateId)
                    .setScore(score)
                    .build());
        }

        return result.stream()
                .sorted(Comparator.comparing(RecommendedEventProto::getScore).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    private float calculateCandidateScore(Long candidateId,
                                          Map<Long, List<Recommendation>> neighboursMap,
                                          Map<Long, Double> userRatings) {
        List<Recommendation> neighbours = neighboursMap.get(candidateId);
        if (neighbours == null || neighbours.isEmpty()) {
            return 0.0f;
        }

        double weightedSum = 0.0;
        double similaritySum = 0.0;

        for (Recommendation neighbour : neighbours) {
            Double rating = userRatings.get(neighbour.getEventId());
            if (rating == null) {
                continue;
            }
            double similarity = neighbour.getScore();
            weightedSum += rating * similarity;
            similaritySum += similarity;
        }

        if (similaritySum == 0.0) {
            return 0.0f;
        }

        return (float) (weightedSum / similaritySum);
    }
}