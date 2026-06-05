package com.scriptflow.framework.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j / Swagger API documentation configuration.
 */
@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI scriptFlowOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ScriptFlow API")
                        .description("AI-assisted novel-to-script conversion platform API")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("ScriptFlow Team")
                                .email("dev@scriptflow.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}
