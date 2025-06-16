package ru.practicum.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaCollectorProducer {
    private final @Qualifier("customKafkaProducerFactory") KafkaProducer<String, SpecificRecordBase> kafkaProducer;

    public void send(String topic, SpecificRecordBase data) {
        try {
            kafkaProducer.send(new ProducerRecord<>(topic, data),
                    (metadata, e) -> {
                        if (Objects.nonNull(e)) {
                            log.error("[{}] Ошибка отправки: {}", topic, e.getMessage());
                        } else {
                            log.info("Отправлено в {} - {}", topic, metadata.partition());
                        }
                    });
        } catch (KafkaException ex) {
            log.error("Ошибка при отправлении сообщения:", ex);
            throw new KafkaException("Ошибка при отправлении сообщения", ex);
        }
    }
}
