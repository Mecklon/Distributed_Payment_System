package com.mecklon.auth;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class controllers {


    @GetMapping("/")
    String fun(){
        return "hello world";
    }
}
