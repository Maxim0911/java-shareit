package ru.practicum.shareit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ShareItGateway {
    public static void main(String[] args) {
        SpringApplication.run(ShareItGateway.class, args);
        System.out.println("✅ ShareIt Gateway started on port 8080");
        System.out.println("🌐 http://localhost:8080");
    }
}