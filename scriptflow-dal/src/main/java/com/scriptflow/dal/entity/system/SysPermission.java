package com.scriptflow.dal.entity.system;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scriptflow.dal.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Permission entity.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_permission")
public class SysPermission extends BaseEntity {

    private String name;
    private String code;
    private String type;
    private Long parentId;
    private String path;
    private String icon;
    private Integer sort;
    private Integer status;
}
