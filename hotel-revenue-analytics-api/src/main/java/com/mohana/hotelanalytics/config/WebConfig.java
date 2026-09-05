package com.mohana.hotelanalytics.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origin-patterns:*}")
    private String allowedOriginPatterns;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] patterns = allowedOriginPatterns.split(",");
        for (int i = 0; i < patterns.length; i++) {
            patterns[i] = patterns[i].trim();
        }

        registry.addMapping("/**")
                .allowedOriginPatterns(patterns)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH")
                .allowedHeaders("*")
                .exposedHeaders("Content-Disposition", "X-Trace-Id")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
