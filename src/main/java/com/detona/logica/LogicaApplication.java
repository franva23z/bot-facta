package com.detona.logica;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "com.detona") 
public class LogicaApplication {

    public static void main(String[] args) {
        SpringApplication.run(LogicaApplication.class, args);
        System.out.println("BOT RODANDO");
    }
}