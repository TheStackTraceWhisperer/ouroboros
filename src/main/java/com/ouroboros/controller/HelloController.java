package com.ouroboros.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping("/")
    public String hello() {
        return "Hello from Ouroboros Spring Boot Application!";
    }

    @GetMapping("/health")
    public String health() {
        return "OK";
    }

}