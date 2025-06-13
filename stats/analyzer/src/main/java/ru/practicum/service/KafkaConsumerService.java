package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.entity.EventSimilarity;
import ru.practicum.entity.UserAction;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.repository.EventSimilarityRepository;
import ru.practicum.repository.UserInteractionRepository;

import java.util.Objects;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final EventSimilarityRepository similarityRepository;
    private final UserInteractionRepository interactionRepository;

    @Transactional
    public void handleSimilarity(EventSimilarityAvro avro) {
        log.debug("Processing similarity event: {}", avro);
        EventSimilarity similarity = new EventSimilarity();
        EventSimilarity.EventSimilarityId id = new EventSimilarity.EventSimilarityId();
        id.setEventA(avro.getEventA());
        id.setEventB(avro.getEventB());
        similarity.setId(id);
        similarity.setScore(avro.getScore());
        similarity.setUpdatedAt(avro.getTimestamp());

        similarityRepository.save(similarity);
    }

    @Transactional
    public void handleUserAction(UserActionAvro avro) {
        log.debug("Processing user action: {}", avro);
        UserAction.UserInteractionId id = new UserAction.UserInteractionId();
        id.setUserId(avro.getUserId());
        id.setEventId(avro.getEventId());

        Optional<UserAction> existingAction = interactionRepository.findById(id);
        UserAction action = existingAction.orElse(new UserAction());
        action.setId(id);
        action.setActionType(avro.getActionType().toString());
        double newWeight = getWeightFromActionType(avro.getActionType());

        // Обновляем только если новый вес больше текущего
        if (Objects.isNull(action.getWeight()) || newWeight > action.getWeight()) {
            action.setWeight(newWeight);
            action.setTimestamp(avro.getTimestamp());
            interactionRepository.save(action);
        }
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
