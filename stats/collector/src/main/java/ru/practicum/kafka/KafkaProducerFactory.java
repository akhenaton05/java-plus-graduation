package ru.practicum.kafka;

import jakarta.annotation.PreDestroy;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Objects;
import java.util.Properties;

@Data
@Slf4j
@Configuration
@RequiredArgsConstructor
public class KafkaProducerFactory {
    private KafkaProducer<String, SpecificRecordBase> pr;
    private final KafkaProducerProperties config;

    @Bean(name = "customKafkaProducerFactory")
    public KafkaProducer<String, SpecificRecordBase> producer() {
        Properties properties = config.setProperties();
        if (Objects.isNull(properties)) {
            log.info("Ошибка загрузки свойств кафка");
        }
        log.info("Загруженная конфигурация {}: ", properties);
        pr = new KafkaProducer<>(properties);
        log.info("Создан kafka-producer {}", pr);
        return pr;
    }

    @PreDestroy
    public void closeProducer() {
        if (Objects.nonNull(pr)) {
            pr.close();
            log.info("kafka-producer закрыт");
        }
    }
}