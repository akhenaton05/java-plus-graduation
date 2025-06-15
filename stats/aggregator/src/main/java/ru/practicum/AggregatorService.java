package ru.practicum;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import ru.practicum.config.KafkaProperties;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableConfigurationProperties(KafkaProperties.class)
public class AggregatorService {
    public static void main(String[] args) {
        SpringApplication.run(AggregatorService.class, args);
    }
}