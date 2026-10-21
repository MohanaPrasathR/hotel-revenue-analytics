package com.mohana.staypulse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class StayPulseApplication {

    public static void main(String[] args) {
        SpringApplication.run(StayPulseApplication.class, args);
    }
}