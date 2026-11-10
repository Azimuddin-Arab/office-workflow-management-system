package com.example.demo.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestProtectedController {

    @GetMapping("/api/protected")
    public String protectedEndpoint() {
        return "Authentication successful!";
    }
}