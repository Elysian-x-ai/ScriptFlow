package com.scriptflow.dal.entity.project;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scriptflow.dal.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * ScriptFlow project entity.
 * Each project represents a novel-to-script conversion task group.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pro_project")
public class Project extends BaseEntity {

    private String name;
    private String description;
    private String cover;
    private String novelTitle;
    private String author;
    private String novelLanguage;
    private Integer chapterCount;
    private Integer status;
    private Long userId;
}
