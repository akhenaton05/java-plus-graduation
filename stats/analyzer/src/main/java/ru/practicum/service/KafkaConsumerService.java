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
        log.info("Processing similarity event: ");
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
        log.info("Processing user action: userId={}, eventId={}, actionType={}, timestamp={}",
                avro.getUserId(), avro.getEventId(), avro.getActionType(), avro.getTimestamp());
        UserAction.UserInteractionId id = new UserAction.UserInteractionId();
        id.setUserId(avro.getUserId());
        id.setEventId(avro.getEventId());

        Optional<UserAction> existingAction = interactionRepository.findById(id);
        UserAction action = existingAction.orElse(new UserAction());
        action.setId(id);
        action.setActionType(avro.getActionType().toString());
        double newWeight = getWeightFromActionType(avro.getActionType());

        if (Objects.isNull(action.getWeight()) || newWeight > action.getWeight()) {
            action.setWeight(newWeight);
            action.setTimestamp(avro.getTimestamp());
            interactionRepository.save(action);
            log.info("Saved user action: userId={}, eventId={}, weight={}, actionType={}",
                    id.getUserId(), id.getEventId(), action.getWeight(), action.getActionType());
        } else {
            log.info("Skipped saving user action: userId={}, eventId={}, newWeight={} <= currentWeight={}",
                    id.getUserId(), id.getEventId(), newWeight, action.getWeight());
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
                throw new IllegalArgumentException("Неизвестный тип действия: " + actionType);
        }
    }
}
