package ru.practicum.kafka;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Objects;
import java.util.Properties;

@Getter
@Setter
@Slf4j
@Configuration
@ConfigurationProperties(prefix = "kafka.producer.properties")
public class KafkaProducerProperties {
    private String bootstrapServer;
    private String keySerializeClass;
    private String valueSerializeClass;

    public Properties setProperties() {
        if (Objects.isNull(bootstrapServer)) {
            log.error("Kafka bootstrap-server is not configured");
            throw new IllegalStateException("Kafka bootstrap-server is not configured");
        }
        if (Objects.isNull(keySerializeClass)) {
            log.error("Kafka key-serializer-class is not configured");
            throw new IllegalStateException("Kafka key-serializer-class is not configured");
        }
        if (Objects.isNull(valueSerializeClass)) {
            log.error("Kafka value-serializer-class is not configured");
            throw new IllegalStateException("Kafka value-serializer-class is not configured");
        }
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, keySerializeClass);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, valueSerializeClass);
        return properties;
    }

    @PostConstruct
    public void init() {
        log.info("Loaded Kafka producer config: bootstrap={}, keySerializer={}, valueSerializer={}",
                bootstrapServer, keySerializeClass, valueSerializeClass);
    }
}