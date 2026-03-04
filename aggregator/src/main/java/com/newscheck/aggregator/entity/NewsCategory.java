package com.newscheck.aggregator.entity;

/**
 * Canonical set of news categories.
 * Each category maps directly to a Kafka topic name.
 */
public enum NewsCategory {
    GENERAL("general"),
    TECHNOLOGY("technology"),
    SPORTS("sports"),
    BUSINESS("business"),
    ENTERTAINMENT("entertainment"),
    HEALTH("health"),
    SCIENCE("science"),
    POLITICS("politics"),
    BREAKING("breaking");

    private final String value;

    NewsCategory(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /** Kafka topic name for this category. Format: news.<category> */
    public String toKafkaTopic() {
        return "news." + value;
    }

    public static NewsCategory fromString(String s) {
        for (NewsCategory c : values()) {
            if (c.value.equalsIgnoreCase(s)) return c;
        }
        return GENERAL;
    }
}
