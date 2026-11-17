package com.aistudybuddy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AiStudyBuddyApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiStudyBuddyApplication.class, args);
        System.out.println("=================================================");
        System.out.println(" AI Study Buddy Spring Boot Backend is Running! ");
        System.out.println(" http://localhost:8080                           ");
        System.out.println("=================================================");
    }
}
