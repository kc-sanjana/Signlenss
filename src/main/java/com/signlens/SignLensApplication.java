package com.signlens;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * SignLens - Luxury Street Sign Detection and Translation System
 * Built with Spring Boot (Java) + AI Vision API
 */
@SpringBootApplication
public class SignLensApplication {
    public static void main(String[] args) {
        SpringApplication.run(SignLensApplication.class, args);
        System.out.println("\n========================================");
        System.out.println("  SignLens is running!");
        System.out.println("  Open: http://localhost:8080");
        System.out.println("========================================\n");
    }
}
