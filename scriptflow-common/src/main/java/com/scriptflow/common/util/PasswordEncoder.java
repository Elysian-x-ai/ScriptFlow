package com.scriptflow.common.util;

import cn.hutool.crypto.digest.BCrypt;

/**
 * Password encoder using BCrypt via Hutool.
 * Replaces plaintext password storage in AuthService.
 */
public final class PasswordEncoder {

    private PasswordEncoder() {}

    /**
     * Encode raw password to BCrypt hash.
     */
    public static String encode(String rawPassword) {
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt());
    }

    /**
     * Verify raw password against BCrypt hash.
     */
    public static boolean matches(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) return false;
        return BCrypt.checkpw(rawPassword, encodedPassword);
    }

    /**
     * Check if a stored password is already BCrypt-encoded.
     * BCrypt hashes always start with "$2a$", "$2b$", or "$2y$".
     */
    public static boolean isEncoded(String storedPassword) {
        return storedPassword != null
                && (storedPassword.startsWith("$2a$")
                || storedPassword.startsWith("$2b$")
                || storedPassword.startsWith("$2y$"));
    }
}
