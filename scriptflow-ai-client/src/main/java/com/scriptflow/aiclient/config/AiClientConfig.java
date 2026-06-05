package com.scriptflow.aiclient.config;

import com.scriptflow.framework.properties.AiServiceProperties;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * AI service HTTP client configuration.
 */
@Configuration
public class AiClientConfig {

    @Bean
    public OkHttpClient aiOkHttpClient(AiServiceProperties properties) {
        return new OkHttpClient.Builder()
                .connectTimeout(properties.getConnectTimeout(), TimeUnit.SECONDS)
                .readTimeout(properties.getReadTimeout(), TimeUnit.SECONDS)
                .writeTimeout(properties.getWriteTimeout(), TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
    }
}
