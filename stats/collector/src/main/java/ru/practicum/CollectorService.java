package ru.practicum;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;

@SpringBootApplication(
        scanBasePackages = "ru.practicum",
        exclude = {KafkaAutoConfiguration.class}
)
public class CollectorService {
    public static void main(String[] args) {
        SpringApplication.run(CollectorService.class, args);
    }
}