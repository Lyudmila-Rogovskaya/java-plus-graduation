package ru.practicum.moderation_service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@Slf4j
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class ModerationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ModerationServiceApplication.class, args);
    }

    @PostConstruct
    public void logStart() {
        log.info("Moderation service started successfully");
    }

}
