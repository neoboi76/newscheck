package com.newscheck.newsserver.config;

import com.newscheck.newsserver.dto.ArticleEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Configures the Kafka consumer factory with manual ACK mode and
 * JSON deserialization of ArticleEvent payloads.
 */
@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:news-server-group}")
    private String groupId;

    @Bean
    public ConsumerFactory<String, ArticleEvent> consumerFactory() {
        JsonDeserializer<ArticleEvent> deserializer =
                new JsonDeserializer<>(ArticleEvent.class,
                        new com.fasterxml.jackson.databind.ObjectMapper()
                                .findAndRegisterModules());
        deserializer.addTrustedPackages("*");
        deserializer.setRemoveTypeHeaders(true);
        deserializer.setUseTypeMapperForKey(false);

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        // Allow up to 3 simultaneous partitions per listener thread
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 50);

        return new DefaultKafkaConsumerFactory<>(props,
                new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ArticleEvent>
    kafkaListenerContainerFactory() {

        ConcurrentKafkaListenerContainerFactory<String, ArticleEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());

        // MANUAL_IMMEDIATE: we call ack.acknowledge() ourselves in the listener
        factory.getContainerProperties()
                .setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        // Run 3 listener threads (one per partition by default)
        factory.setConcurrency(3);

        return factory;
    }
}