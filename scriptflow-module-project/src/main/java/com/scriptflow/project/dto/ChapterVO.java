package com.scriptflow.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Chapter view object.
 */
@Data
@Schema(description = "Chapter view object")
public class ChapterVO {

    @Schema(description = "Chapter ID")
    private Long id;

    @Schema(description = "Project ID")
    private Long projectId;

    @Schema(description = "Chapter number")
    private Integer chapterNo;

    @Schema(description = "Chapter title")
    private String title;

    @Schema(description = "Content (truncated for list view)")
    private String contentPreview;

    @Schema(description = "Full content")
    private String content;

    @Schema(description = "Word count")
    private Integer wordCount;

    @Schema(description = "Summary")
    private String summary;

    @Schema(description = "MD5 hash of content for change detection")
    private String contentHash;

    @Schema(description = "Creation time")
    private LocalDateTime createTime;
}
