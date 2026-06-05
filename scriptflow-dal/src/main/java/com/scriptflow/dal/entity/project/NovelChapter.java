package com.scriptflow.dal.entity.project;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scriptflow.dal.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Novel chapter entity.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pro_novel_chapter")
public class NovelChapter extends BaseEntity {

    private Long projectId;
    private Integer chapterNo;
    private String title;
    private String content;
    private Integer wordCount;
    private String summary;
}
