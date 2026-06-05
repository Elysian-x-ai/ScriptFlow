package com.scriptflow.boot;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * ScriptFlow - AI-assisted novel-to-script conversion platform.
 *
 * Spring Boot application entry point.
 */
@SpringBootApplication
@ComponentScan(basePackages = {
        "com.scriptflow.common",
        "com.scriptflow.framework",
        "com.scriptflow.dal",
        "com.scriptflow.system",
        "com.scriptflow.project",
        "com.scriptflow.task",
        "com.scriptflow.prompt",
        "com.scriptflow.export",
        "com.scriptflow.storage",
        "com.scriptflow.aiclient"
})
@MapperScan("com.scriptflow.dal.mapper")
public class ScriptFlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScriptFlowApplication.class, args);
    }
}
