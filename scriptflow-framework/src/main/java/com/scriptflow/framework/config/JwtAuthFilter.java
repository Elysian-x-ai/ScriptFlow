package com.scriptflow.framework.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scriptflow.common.result.R;
import com.scriptflow.common.result.ResultCode;
import com.scriptflow.framework.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT authentication filter.
 * Validates JWT token from the Authorization header for every /api/** request,
 * except whitelisted paths (login, register, swagger).
 */
@Slf4j
@Component
@Order(-1)
@RequiredArgsConstructor
@EnableConfigurationProperties(JwtProperties.class)
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtProperties jwtProperties;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        JwtUtil.init(jwtProperties.getSecret(), jwtProperties.getExpirationMs());
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        String cp = request.getContextPath();
        if (cp != null && !cp.isEmpty()) {
            path = path.substring(cp.length());
        }
        return path.equals("/api/system/auth/login")
                || path.equals("/api/system/auth/register")
                || path.startsWith("/swagger-ui/")
                || path.startsWith("/v3/api-docs/")
                || path.equals("/doc.html")
                || path.startsWith("/webjars/")
                || path.equals("/favicon.ico");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // Allow OPTIONS preflight
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extract token: Authorization header first, then query param (for SSE)
        String token = request.getHeader("Authorization");
        if (token == null || token.isBlank()) {
            token = request.getParameter("token");
        }
        if (token == null || token.isBlank()) {
            writeUnauthorized(response, "未提供 token");
            return;
        }

        // Strip "Bearer " prefix if present
        token = token.trim();
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        if (!JwtUtil.validateToken(token)) {
            writeUnauthorized(response, "token 无效或已过期");
            return;
        }

        try {
            Long userId = JwtUtil.getUserIdFromToken(token);
            JwtUtil.setCurrentUserId(userId);
            filterChain.doFilter(request, response);
        } finally {
            JwtUtil.removeCurrentUserId();
        }
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), R.fail(ResultCode.UNAUTHORIZED, message));
    }
}
