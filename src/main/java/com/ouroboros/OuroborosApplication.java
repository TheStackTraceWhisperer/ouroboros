package com.ouroboros;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OuroborosApplication {

    public static void main(String[] args) {
        SpringApplication.run(OuroborosApplication.class, args);
    }

}