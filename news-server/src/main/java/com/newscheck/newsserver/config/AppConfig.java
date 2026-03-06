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

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    // Cache-Control headers for public article endpoints
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        WebContentInterceptor listingsCacheInterceptor = new WebContentInterceptor();
        listingsCacheInterceptor.setCacheControl(
                CacheControl.maxAge(60, TimeUnit.SECONDS).cachePublic());
        registry.addInterceptor(listingsCacheInterceptor)
                .addPathPatterns(
                        "/api/articles",
                        "/api/articles/category/**",
                        "/api/articles/search");

        WebContentInterceptor breakingCacheInterceptor = new WebContentInterceptor();
        breakingCacheInterceptor.setCacheControl(
                CacheControl.maxAge(30, TimeUnit.SECONDS).cachePublic());
        registry.addInterceptor(breakingCacheInterceptor)
                .addPathPatterns("/api/articles/breaking");

        WebContentInterceptor detailCacheInterceptor = new WebContentInterceptor();
        detailCacheInterceptor.setCacheControl(
                CacheControl.maxAge(5, TimeUnit.MINUTES).cachePublic());
        registry.addInterceptor(detailCacheInterceptor)
                .addPathPatterns("/api/articles/{id}");
    }
}
