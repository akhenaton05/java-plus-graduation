package ru.practicum.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import org.springframework.kafka.annotation.KafkaListener;
import ru.practicum.service.KafkaConsumerService;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaConsumerListener {

    private final KafkaConsumerService consumerService;

    @KafkaListener(topics = "${kafka.similarity-consumer.topic}", groupId = "${kafka.similarity-consumer.properties.group.id}",
            containerFactory = "similarityListenerContainerFactory")
    public void handleSimilarity(EventSimilarityAvro avro) {
        log.debug("Received similarity message: {}", avro);
        consumerService.handleSimilarity(avro);
    }

    @KafkaListener(topics = "${kafka.action-consumer.topic}", groupId = "${kafka.action-consumer.properties.group.id}",
            containerFactory = "actionListenerContainerFactory")
    public void handleUserAction(UserActionAvro avro) {
        log.debug("Received user action message: {}", avro);
        consumerService.handleUserAction(avro);
    }
}
