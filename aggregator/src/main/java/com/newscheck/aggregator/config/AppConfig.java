package com.newscheck.aggregator.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class AppConfig {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10));
        return new RestTemplate(factory);
    }

    /**
     * Bounded thread pool for parallel news API fetches.
     * 4 threads is enough to saturate network I/O without overwhelming
     * external API rate limits.
     */
    @Bean(name = "fetchExecutor")
    public ExecutorService fetchExecutor() {
        return Executors.newFixedThreadPool(4);
    }
}
