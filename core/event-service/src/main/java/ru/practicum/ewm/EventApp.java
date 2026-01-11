package ru.practicum.ewm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
        "ru.practicum.ewm",
        "ru.practicum.statsclient"
})
@EnableFeignClients(basePackages = {
        "ru.practicum.ewm.client",
        "ru.practicum.statsclient"
})
public class EventApp {
    public static void main(String[] args) {
        SpringApplication.run(EventApp.class, args);
    }
}