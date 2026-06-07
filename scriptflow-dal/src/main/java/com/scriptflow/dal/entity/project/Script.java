package com.scriptflow.dal.entity.project;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scriptflow.dal.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Generated script entity.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pro_script")
public class Script extends BaseEntity {

    private Long projectId;
    private Integer version;
    private String yamlContent;
    private Integer wordCount;
    private Integer status;
    private String errorMsg;
    private String minioKey;
}
