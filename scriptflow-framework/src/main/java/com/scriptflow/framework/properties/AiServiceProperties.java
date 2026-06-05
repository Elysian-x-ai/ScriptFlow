package com.scriptflow.framework.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Python AI service connection properties.
 */
@Data
@Component
@ConfigurationProperties(prefix = "scriptflow.ai")
public class AiServiceProperties {

    private String baseUrl = "http://localhost:8000";
    private int connectTimeout = 10;
    private int readTimeout = 120;
    private int writeTimeout = 60;
    private int maxRetries = 3;
    private String apiKey = "";
}
