package com.diariofinanciero;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DiarioFinancieroApplication {

    public static void main(String[] args) {
        SpringApplication.run(DiarioFinancieroApplication.class, args);
    }
}
