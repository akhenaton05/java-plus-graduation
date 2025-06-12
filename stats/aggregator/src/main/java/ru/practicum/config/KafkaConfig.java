package ru.practicum.config;

import org.apache.avro.specific.SpecificRecordBase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
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
    public ProducerFactory<String, SpecificRecordBase> producerFactory() {
        Map<String, Object> props = flattenMap(kafkaProperties.getProducer().getProperties());
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, SpecificRecordBase> kafkaTemplate(ProducerFactory<String, SpecificRecordBase> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    public ConsumerFactory<String, UserActionAvro> consumerFactory() {
        Map<String, Object> props = flattenMap(kafkaProperties.getConsumer().getProperties());
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, UserActionAvro> kafkaListenerContainerFactory(
            ConsumerFactory<String, UserActionAvro> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, UserActionAvro> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        return factory;
    }

    //Для правильного маппинга из application.yaml
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