package com.scriptflow.dal.entity.prompt;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scriptflow.dal.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Prompt template entity for AI prompt management.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pro_prompt_template")
public class PromptTemplate extends BaseEntity {

    private String name;
    private String code;
    private String type;
    private String category;
    private String content;
    private String variables;
    private String description;
    private Integer version;
    private Integer status;
}
