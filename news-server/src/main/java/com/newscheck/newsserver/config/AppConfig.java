package com.newscheck.newsserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.servlet.mvc.WebContentInterceptor;

import java.util.concurrent.TimeUnit;

@Configuration
public class AppConfig implements WebMvcConfigurer {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /** Allow the Android emulator (10.0.2.2) and local dev to hit the API. */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /**
     * Add Cache-Control headers to public article GET endpoints.
     * - Article listings/search: 60 seconds (news updates frequently)
     * - Breaking news: 30 seconds (time-sensitive)
     * Clients and CDNs can cache responses, reducing server load.
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Article listings, category, search — cache 60s
        WebContentInterceptor listingsCacheInterceptor = new WebContentInterceptor();
        listingsCacheInterceptor.setCacheControl(
                CacheControl.maxAge(60, TimeUnit.SECONDS).cachePublic());
        registry.addInterceptor(listingsCacheInterceptor)
                .addPathPatterns(
                        "/api/articles",
                        "/api/articles/category/**",
                        "/api/articles/search");

        // Breaking news — cache 30s (more time-sensitive)
        WebContentInterceptor breakingCacheInterceptor = new WebContentInterceptor();
        breakingCacheInterceptor.setCacheControl(
                CacheControl.maxAge(30, TimeUnit.SECONDS).cachePublic());
        registry.addInterceptor(breakingCacheInterceptor)
                .addPathPatterns("/api/articles/breaking");

        // Single article detail — cache 5 min (article content doesn't change)
        WebContentInterceptor detailCacheInterceptor = new WebContentInterceptor();
        detailCacheInterceptor.setCacheControl(
                CacheControl.maxAge(5, TimeUnit.MINUTES).cachePublic());
        registry.addInterceptor(detailCacheInterceptor)
                .addPathPatterns("/api/articles/{id}");
    }
}
