package com.scriptflow.boot;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;

/**
 * ScriptFlow - AI-assisted novel-to-script conversion platform.
 *
 * Spring Boot application entry point.
 */
@EnableRabbit
@SpringBootApplication
@ComponentScan(basePackages = "com.scriptflow")
@MapperScan("com.scriptflow.dal.mapper")
public class ScriptFlowApplication {
    public static void main(String[] args) {
        SpringApplication.run(ScriptFlowApplication.class, args);
        System.out.println("ScriptFlow application started successfully!");
    }
}
