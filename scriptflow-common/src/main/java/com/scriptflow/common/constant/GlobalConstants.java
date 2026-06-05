package com.scriptflow.common.constant;

/**
 * System-wide constants.
 */
public interface GlobalConstants {

    String DATE_FORMAT = "yyyy-MM-dd";
    String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    String TIME_ZONE = "Asia/Shanghai";

    int PAGE_SIZE_DEFAULT = 20;
    int PAGE_SIZE_MAX = 200;

    String DEFAULT_PASSWORD = "123456";

    interface Status {
        int ENABLED = 1;
        int DISABLED = 0;
        int DELETED = -1;
    }

    interface TaskType {
        String NOVEL_ANALYSIS = "novel_analysis";
        String CHARACTER_EXTRACT = "character_extract";
        String SCRIPT_GENERATE = "script_generate";
        String SCRIPT_REVISE = "script_revise";
    }

    interface TaskStatus {
        int PENDING = 0;
        int PROCESSING = 1;
        int COMPLETED = 2;
        int FAILED = 3;
        int CANCELLED = 4;
    }

    interface ScriptStatus {
        int DRAFT = 0;
        int GENERATING = 1;
        int COMPLETED = 2;
        int FAILED = 3;
    }

    interface ProjectStatus {
        int ACTIVE = 1;
        int ARCHIVED = 0;
    }
}
