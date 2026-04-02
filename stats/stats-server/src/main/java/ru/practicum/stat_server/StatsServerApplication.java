package ru.practicum.stat_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class StatsServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(StatsServerApplication.class, args);
    }

}
