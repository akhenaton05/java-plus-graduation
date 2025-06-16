package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.entity.EventSimilarity;
import ru.practicum.entity.UserAction;
import ru.practicum.ewm.stats.proto.RecommendedEventProto;
import ru.practicum.repository.EventSimilarityRepository;
import ru.practicum.repository.UserInteractionRepository;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final EventSimilarityRepository similarityRepository;
    private final UserInteractionRepository interactionRepository;

    public Stream<RecommendedEventProto> getRecommendationsForUser(long userId, long maxResults) {
        log.info("getRecommendationsForUser {}", userId);
        //Получение maxResults последних взаимодействий пользователя
        List<UserAction> userActions = interactionRepository.findByIdUserIdOrderByTimestampDesc(userId);
        if (userActions.isEmpty()) {
            return Stream.empty();
        }
        List<Long> interactedEventIds = userActions.stream()
                .map(action -> action.getId().getEventId())
                .limit(10) // Ограничим 10 последними
                .collect(Collectors.toList());

        //Поиск похожих мероприятий
        Set<Long> candidateEventIds = interactedEventIds.stream()
                .flatMap(eventId -> {
                    List<EventSimilarity> similarities = similarityRepository.findByIdEventAOrIdEventB(eventId, eventId);
                    return similarities.stream()
                            .flatMap(s -> {
                                Set<Long> events = new HashSet<>();
                                events.add(s.getId().getEventA());
                                events.add(s.getId().getEventB());
                                return events.stream();
                            });
                })
                .filter(eventId -> !interactedEventIds.contains(eventId))
                .collect(Collectors.toSet());

        //Рассчет оценки для кандидатов
        return candidateEventIds.stream()
                .map(eventId -> {
                    // Найти K ближайших соседей (K=5)
                    List<EventSimilarity> neighbors = similarityRepository.findByIdEventAOrIdEventB(eventId, eventId)
                            .stream()
                            .filter(s -> interactedEventIds.contains(s.getId().getEventA()) ||
                                    interactedEventIds.contains(s.getId().getEventB()))
                            .sorted(Comparator.comparing(EventSimilarity::getScore).reversed())
                            .limit(5)
                            .collect(Collectors.toList());

                    //Поиск взвешенной оценки
                    double weightedSum = 0.0;
                    double similaritySum = 0.0;
                    for (EventSimilarity neighbor : neighbors) {
                        long neighborEventId = neighbor.getId().getEventA().equals(eventId) ?
                                neighbor.getId().getEventB() : neighbor.getId().getEventA();
                        UserAction action = userActions.stream()
                                .filter(a -> a.getId().getEventId().equals(neighborEventId))
                                .findFirst()
                                .orElse(null);
                        if (Objects.nonNull(action)) {
                            weightedSum += neighbor.getScore() * action.getWeight();
                            similaritySum += neighbor.getScore();
                        }
                    }
                    double score = similaritySum > 0 ? weightedSum / similaritySum : 0.0;

                    return RecommendedEventProto.newBuilder()
                            .setEventId(eventId)
                            .setScore(score)
                            .build();
                })
                .sorted(Comparator.comparingDouble(RecommendedEventProto::getScore).reversed())
                .limit(maxResults);
    }

    public Stream<RecommendedEventProto> getSimilarEvents(long eventId, long userId, long maxResults) {
        log.info("getSimilarEvents to {}", userId);
        //Поиск похожих мероприятий
        List<EventSimilarity> similarities = similarityRepository.findByIdEventAOrIdEventB(eventId, eventId);

        //Исключить мероприятия, с которыми пользователь взаимодействовал
        List<Long> interactedEventIds = interactionRepository.findByIdUserIdOrderByTimestampDesc(userId)
                .stream()
                .map(action -> action.getId().getEventId())
                .collect(Collectors.toList());

        //Поиск maxResult самых похожих
        return similarities.stream()
                .flatMap(s -> {
                    long otherEventId = s.getId().getEventA().equals(eventId) ? s.getId().getEventB() : s.getId().getEventA();
                    if (!interactedEventIds.contains(otherEventId)) {
                        return Stream.of(RecommendedEventProto.newBuilder()
                                .setEventId(otherEventId)
                                .setScore(s.getScore())
                                .build());
                    }
                    return Stream.empty();
                })
                .sorted(Comparator.comparingDouble(RecommendedEventProto::getScore).reversed())
                .limit(maxResults);
    }

    public Stream<RecommendedEventProto> getInteractionsCount(List<Long> eventIds) {
        log.info("Calculating interactions weight for eventIds={}", eventIds);
        try {
            if (Objects.isNull(eventIds) || eventIds.isEmpty()) {
                log.warn("Empty eventIds list provided");
                return Stream.empty();
            }
            //Получить сумму максимальных весов для каждого eventId
            List<Object[]> weightSums = interactionRepository.sumMaxWeightsByEventIds(eventIds);
            Map<Long, Double> weights = weightSums.stream()
                    .collect(Collectors.toMap(
                            row -> (Long) row[0], // event_id
                            row -> (Double) row[1], // sum of weights
                            (a, b) -> a
                    ));
            List<RecommendedEventProto> results = eventIds.stream()
                    .map(eventId -> RecommendedEventProto.newBuilder()
                            .setEventId(eventId)
                            .setScore(weights.getOrDefault(eventId, 0.0))
                            .build())
                    .collect(Collectors.toList());
            log.info("Returning interactions weight: {}", results);
            return results.stream();
        } catch (Exception e) {
            log.error("Error calculating interactions weight for eventIds={}", eventIds, e);
            throw new RuntimeException("Failed to calculate interactions weight", e);
        }
    }
}