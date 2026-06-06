package com.scriptflow.framework.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT configuration properties.
 */
@Data
@ConfigurationProperties(prefix = "scriptflow.jwt")
public class JwtProperties {

    /** Secret key for HMAC-SHA256 signing (must be at least 256 bits / 32 chars). */
    private String secret = "ScriptFlowDefaultSecretKey2024ForJWT!!";

    /** Token expiration in milliseconds (default: 30 days). */
    private long expirationMs = 2592000000L;
}
