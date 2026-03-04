package com.newscheck.aggregator.config;

import com.newscheck.aggregator.entity.NewsCategory;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Programmatically declares all Kafka topics.
 * Spring Boot's KafkaAdmin will create them on startup if they don't exist yet.
 *
 * Topic naming convention:  news.<category>
 * e.g.  news.technology,  news.sports,  news.breaking
 */
@Configuration
public class KafkaTopicConfig {

    private static final int PARTITIONS   = 3;   // Allows parallel consumption per topic
    private static final int REPLICAS     = 1;   // Use 3 in a multi-broker production cluster

    @Bean public NewTopic topicGeneral()       { return buildTopic(NewsCategory.GENERAL); }
    @Bean public NewTopic topicTechnology()    { return buildTopic(NewsCategory.TECHNOLOGY); }
    @Bean public NewTopic topicSports()        { return buildTopic(NewsCategory.SPORTS); }
    @Bean public NewTopic topicBusiness()      { return buildTopic(NewsCategory.BUSINESS); }
    @Bean public NewTopic topicEntertainment() { return buildTopic(NewsCategory.ENTERTAINMENT); }
    @Bean public NewTopic topicHealth()        { return buildTopic(NewsCategory.HEALTH); }
    @Bean public NewTopic topicScience()       { return buildTopic(NewsCategory.SCIENCE); }
    @Bean public NewTopic topicPolitics()      { return buildTopic(NewsCategory.POLITICS); }
    @Bean public NewTopic topicBreaking()      { return buildTopic(NewsCategory.BREAKING); }

    private NewTopic buildTopic(NewsCategory category) {
        return TopicBuilder.name(category.toKafkaTopic())
                           .partitions(PARTITIONS)
                           .replicas(REPLICAS)
                           .build();
    }
}
