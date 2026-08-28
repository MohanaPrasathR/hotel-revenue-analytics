package com.mohana.hotelanalytics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class HotelRevenueAnalyticsApplication {

    public static void main(String[] args) {
        SpringApplication.run(HotelRevenueAnalyticsApplication.class, args);
    }
}