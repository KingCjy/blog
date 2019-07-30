package com.kingcjy.demo.controller;

import com.kingcjy.demo.annotation.CustomResolver;
import com.kingcjy.demo.person.Person;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/")
    public Person test(@CustomResolver Person person) {

        return person;
    }
}
