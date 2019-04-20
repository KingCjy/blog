package com.kingcjy.main.contoller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MyController {
    @Value("${spring.profile.value}")
    private String profile;

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return new ResponseEntity<>(profile, HttpStatus.OK);
    }
}
