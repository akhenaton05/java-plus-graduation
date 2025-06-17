package ru.practicum.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.service.UserActionAggregator;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaTopicListener {

    private final UserActionAggregator userActionAggregator;
    private final KafkaSimilarityProducer kafkaSimilarityProducer;

    @KafkaListener(topics = "${kafka.consumer.topic}",
            groupId = "aggregator-group",
            containerFactory = "kafkaListenerContainerFactory")
    public void handleEvent(UserActionAvro userActionAvro) {
        log.info("Received event: {}", userActionAvro);
        List<EventSimilarityAvro> similarities = userActionAggregator.updateState(userActionAvro);
        similarities.forEach(similarity -> {
            log.info("Similarity calculated for events {} and {}: score={}",
                    similarity.getEventA(), similarity.getEventB(), similarity.getScore());
            kafkaSimilarityProducer.send(similarity);
        });
    }
}
