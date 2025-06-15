package ru.practicum.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {
    private final KafkaProperties kafkaProperties;

    public KafkaConfig(KafkaProperties kafkaProperties) {
        this.kafkaProperties = kafkaProperties;
    }

    @Bean
    public ConsumerFactory<String, EventSimilarityAvro> similarityConsumerFactory() {
        Map<String, Object> props = flattenMap(kafkaProperties.getSimilarityConsumer().getProperties());
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, EventSimilarityAvro> similarityListenerContainerFactory(
            ConsumerFactory<String, EventSimilarityAvro> similarityConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, EventSimilarityAvro> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(similarityConsumerFactory);
        return factory;
    }

    @Bean
    public ConsumerFactory<String, UserActionAvro> actionConsumerFactory() {
        Map<String, Object> props = flattenMap(kafkaProperties.getActionConsumer().getProperties());
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, UserActionAvro> actionListenerContainerFactory(
            ConsumerFactory<String, UserActionAvro> actionConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, UserActionAvro> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(actionConsumerFactory);
        return factory;
    }

    // Для маппинга из application.yaml
    private Map<String, Object> flattenMap(Map<String, Object> source) {
        Map<String, Object> result = new HashMap<>();
        source.forEach((key, value) -> {
            if (value instanceof Map) {
                ((Map<?, ?>) value).forEach((subKey, subValue) ->
                        result.put(key + "." + subKey, subValue));
            } else {
                result.put(key, value);
            }
        });
        return result;
    }
}
