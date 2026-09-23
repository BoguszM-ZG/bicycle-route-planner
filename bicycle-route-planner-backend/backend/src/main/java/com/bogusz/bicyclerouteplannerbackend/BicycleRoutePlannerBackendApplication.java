package com.bogusz.bicyclerouteplannerbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class BicycleRoutePlannerBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BicycleRoutePlannerBackendApplication.class, args);
    }

}
