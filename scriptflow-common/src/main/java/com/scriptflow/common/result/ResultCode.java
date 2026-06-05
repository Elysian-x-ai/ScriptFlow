package com.scriptflow.common.result;

/**
 * Unified response code enumeration.
 */
public enum ResultCode {

    SUCCESS(200, "success"),
    FAIL(500, "Internal server error"),

    // Auth
    UNAUTHORIZED(401, "Unauthorized"),
    FORBIDDEN(403, "Forbidden"),
    TOKEN_EXPIRED(4001, "Token expired"),
    TOKEN_INVALID(4002, "Token invalid"),

    // Validation
    BAD_REQUEST(400, "Bad request"),
    PARAM_ERROR(4004, "Parameter error"),
    PARAM_MISSING(4005, "Required parameter missing"),

    // Business
    NOT_FOUND(404, "Resource not found"),
    CONFLICT(409, "Resource conflict"),
    DUPLICATE(4006, "Duplicate entry"),

    // System
    SERVICE_UNAVAILABLE(503, "Service temporarily unavailable"),
    DB_ERROR(5001, "Database error"),
    RATE_LIMIT(429, "Too many requests"),

    // Task
    TASK_NOT_FOUND(7001, "Task not found"),
    TASK_PROCESSING(7002, "Task is processing"),
    TASK_FAILED(7003, "Task failed"),

    // External services
    AI_SERVICE_ERROR(8001, "AI service error"),
    STORAGE_ERROR(8002, "Storage service error"),
    EXPORT_ERROR(8003, "Export failed");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() { return code; }
    public String getMessage() { return message; }
}
