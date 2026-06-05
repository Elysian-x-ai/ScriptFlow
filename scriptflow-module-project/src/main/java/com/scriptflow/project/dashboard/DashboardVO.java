package com.scriptflow.project.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DashboardVO {
    private long projectCount;
    private long projectMonthCount;
    private long taskCount;
    private long taskCompletedCount;
    private long taskProcessingCount;
    private long taskPendingCount;
    private long characterCount;
    private long chapterCount;
}
