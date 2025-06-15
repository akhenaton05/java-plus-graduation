package ru.practicum.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "kafka")
@Getter
@Setter
@ToString
public class KafkaProperties {
    private ConsumerConfig similarityConsumer;
    private ConsumerConfig actionConsumer;

    @Getter
    @Setter
    @ToString
    public static class ConsumerConfig {
        private Map<String, Object> properties;
        private String topic;
    }
}