package com.scriptflow.dal.entity.system;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scriptflow.dal.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * System user entity.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {

    private String username;
    private String password;
    private String nickname;
    private String email;
    private String phone;
    private String avatar;
    private Integer status;
    private Long tenantId;
    private String remark;
}
