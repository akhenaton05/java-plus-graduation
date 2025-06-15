package ru.practicum.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class UserActionAggregator {

    private final Map<Long, Map<Long, Double>> eventUserWeights = new ConcurrentHashMap<>();
    private final Map<Long, Double> eventWeightSums = new ConcurrentHashMap<>();
    private final Map<Long, Map<Long, Double>> minWeightsSums = new ConcurrentHashMap<>();

    public List<EventSimilarityAvro> updateState(UserActionAvro action) {
        List<EventSimilarityAvro> similarities = new ArrayList<>();
        long eventId = action.getEventId();
        long userId = action.getUserId();
        double newWeight = getWeightFromActionType(action.getActionType());
        Instant timestamp = action.getTimestamp();

        // Получение текущего веса
        Map<Long, Double> userWeights = eventUserWeights.computeIfAbsent(eventId, k -> new ConcurrentHashMap<>());
        double currentWeight = userWeights.getOrDefault(userId, 0.0);

        // Если вес не изменился, ничего не пересчитываем
        if (currentWeight >= newWeight) {
            log.debug("Weight for user {} and event {} not changed: {}", userId, eventId, currentWeight);
            return similarities;
        }

        // Обновление веса пользователя
        userWeights.put(userId, newWeight);

        // Обновление суммы весов для мероприятия
        double oldEventSum = eventWeightSums.getOrDefault(eventId, 0.0);
        double userWeightDiff = newWeight - currentWeight;
        eventWeightSums.put(eventId, oldEventSum + userWeightDiff);

        // Пересчёт сходства для всех пар с eventId
        for (long eventB : eventUserWeights.keySet()) {
            if (eventB == eventId) continue;

            // Упорядочивание идентификаторов
            long first = Math.min(eventId, eventB);
            long second = Math.max(eventId, eventB);

            // Получение весов пользователя для обоих мероприятий
            Double weightA = eventUserWeights.get(eventId).get(userId);
            Double weightB = eventUserWeights.getOrDefault(eventB, new ConcurrentHashMap<>()).getOrDefault(userId, 0.0);

            // Если пользователь не взаимодействовал с eventB, пропускаем
            if (weightB == 0.0) continue;

            // Обновление суммы минимальных весов
            Map<Long, Double> minSums = minWeightsSums.computeIfAbsent(first, k -> new ConcurrentHashMap<>());
            double currentMinSum = minSums.getOrDefault(second, 0.0);
            double oldMin = Math.min(currentWeight, weightB);
            double newMin = Math.min(newWeight, weightB);
            double minSumDiff = newMin - oldMin;
            minSums.put(second, currentMinSum + minSumDiff);

            // Расчёт косинусного сходства
            double sMin = minSums.get(second);
            double sA = eventWeightSums.get(first);
            double sB = eventWeightSums.getOrDefault(second, 0.0);
            double similarity = (sA * sB > 0) ? sMin / Math.sqrt(sA * sB) : 0.0;

            // Создание сообщения
            EventSimilarityAvro similarityAvro = new EventSimilarityAvro();
            similarityAvro.setEventA(first);
            similarityAvro.setEventB(second);
            similarityAvro.setScore(similarity);
            similarityAvro.setTimestamp(timestamp);
            similarities.add(similarityAvro);
        }

        return similarities;
    }

    private double getWeightFromActionType(ActionTypeAvro actionType) {
        switch (actionType) {
            case VIEW:
                return 0.4;
            case REGISTER:
                return 0.8;
            case LIKE:
                return 1.0;
            default:
                throw new IllegalArgumentException("Unknown action type: " + actionType);
        }
    }
}
