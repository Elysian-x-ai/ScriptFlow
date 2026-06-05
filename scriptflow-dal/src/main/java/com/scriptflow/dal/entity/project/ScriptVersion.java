package com.scriptflow.dal.entity.project;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scriptflow.dal.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Script version entity for git-like version tracking.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pro_script_version")
public class ScriptVersion extends BaseEntity {

    private Long scriptId;
    private Integer versionNo;
    private String yamlContent;
    private String diffContent;
    private String changeLog;
    private Long createBy;
}
