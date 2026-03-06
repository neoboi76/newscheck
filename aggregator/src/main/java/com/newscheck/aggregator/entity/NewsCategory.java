package com.newscheck.aggregator.entity;

// Each category maps to a Kafka topic (news.<category>)
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

    /** Kafka topic name for this category. */
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
