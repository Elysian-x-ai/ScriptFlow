package com.scriptflow.dal.entity.system;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scriptflow.dal.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Multi-tenant entity.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_tenant")
public class SysTenant extends BaseEntity {

    private String name;
    private String code;
    private String contactName;
    private String contactPhone;
    private LocalDateTime expireTime;
    private Integer status;
    private String remark;
}
