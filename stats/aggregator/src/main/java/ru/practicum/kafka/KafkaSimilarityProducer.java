package ru.practicum.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.practicum.config.KafkaProperties;

import java.util.Objects;


@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaSimilarityProducer {

    private final KafkaTemplate<String, SpecificRecordBase> kafkaTemplate;
    private final KafkaProperties kafkaProperties;

    public void send(SpecificRecordBase snapshot) {
        String topic = kafkaProperties.getProducer().getTopic();
        kafkaTemplate.send(topic, snapshot).whenComplete((result, ex) -> {
            if (Objects.nonNull(ex)) {
                log.error("Failed to send snapshot to Kafka: {}", snapshot, ex);
            } else if (result != null) {
                log.debug("Snapshot sent to Kafka: partition={}, offset={}",
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}
