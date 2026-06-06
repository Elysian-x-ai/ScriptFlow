package com.scriptflow.framework.config;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.http.HttpMethod;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token interceptor registration.
 * Secures all /api/** endpoints except login/register and Swagger docs.
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> {
            if (HttpMethod.OPTIONS.matches(SaHolder.getRequest().getMethod())) return;
            StpUtil.checkLogin();
        }))
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/system/auth/login",
                        "/api/system/auth/register",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/doc.html",
                        "/webjars/**",
                        "/favicon.ico"
                );
    }
}
