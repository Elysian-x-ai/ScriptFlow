package com.scriptflow.dal.entity.system;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Role-Permission association entity.
 */
@Data
@TableName("sys_role_permission")
public class SysRolePermission implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long roleId;
    private Long permissionId;
    private LocalDateTime createTime;
}
