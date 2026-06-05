package com.scriptflow.dal.entity.project;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scriptflow.dal.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Novel character entity.
 * Note: named CharacterEntity to avoid conflict with java.lang.Character.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pro_character")
public class CharacterEntity extends BaseEntity {

    private Long projectId;
    private String name;
    private String alias;
    private String gender;
    private String age;
    private String personality;
    private String appearance;
    private String background;
    private String description;
    private String roleType;
}
