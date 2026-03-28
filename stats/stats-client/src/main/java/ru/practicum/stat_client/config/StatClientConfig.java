package ru.practicum.stat_client.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class StatClientConfig {

    @Value("${stat-server.url:http://localhost:9090}")
    private String statServerUrl;

    @Bean
    public RestClient restClient() {
        return RestClient.builder().baseUrl(statServerUrl).defaultHeader("Content-Type", "application/json").build();
    }
}