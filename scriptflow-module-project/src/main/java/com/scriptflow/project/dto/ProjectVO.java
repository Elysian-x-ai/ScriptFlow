package com.scriptflow.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Project view object.
 */
@Data
@Schema(description = "Project view object")
public class ProjectVO {

    @Schema(description = "Project ID")
    private Long id;

    @Schema(description = "Project name")
    private String name;

    @Schema(description = "Description")
    private String description;

    @Schema(description = "Cover image URL")
    private String cover;

    @Schema(description = "Novel title")
    private String novelTitle;

    @Schema(description = "Novel author")
    private String author;

    @Schema(description = "Novel language")
    private String novelLanguage;

    @Schema(description = "Chapter count")
    private Integer chapterCount;

    @Schema(description = "Status: 1=active, 0=archived")
    private Integer status;

    @Schema(description = "Owner user ID")
    private Long userId;

    @Schema(description = "Creation time")
    private LocalDateTime createTime;

    @Schema(description = "Update time")
    private LocalDateTime updateTime;
}
