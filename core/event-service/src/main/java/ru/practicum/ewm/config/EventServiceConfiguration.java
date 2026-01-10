package ru.practicum.ewm.config;

import feign.Retryer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EventServiceConfiguration {

    @Bean
    public Retryer feignRetryer() {
        // firstBackoff=100ms, maxBackoff=1s, attempts=5
        return new Retryer.Default(100, 1000, 5);
    }
}