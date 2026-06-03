package com.sportreserve;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SportReserveApplication {

    public static void main(String[] args) {
        SpringApplication.run(SportReserveApplication.class, args);
    }
}
