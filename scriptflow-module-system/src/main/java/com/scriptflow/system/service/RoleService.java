package com.scriptflow.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scriptflow.common.exception.BusinessException;
import com.scriptflow.common.result.ResultCode;
import com.scriptflow.common.util.PageUtils;
import com.scriptflow.dal.entity.system.SysRole;
import com.scriptflow.dal.entity.system.SysRolePermission;
import com.scriptflow.dal.mapper.system.SysRoleMapper;
import com.scriptflow.dal.mapper.system.SysRolePermissionMapper;
import com.scriptflow.system.dto.RoleDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Role management service.
 */
@Service
@RequiredArgsConstructor
public class RoleService {

    private final SysRoleMapper roleMapper;
    private final SysRolePermissionMapper rolePermissionMapper;

    public PageUtils<SysRole> page(int page, int pageSize) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<SysRole>()
                .orderByAsc(SysRole::getCreateTime);

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<SysRole> mpPage =
                roleMapper.selectPage(
                        new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, pageSize),
                        wrapper);

        return PageUtils.of((int) mpPage.getCurrent(), (int) mpPage.getSize(),
                (int) mpPage.getTotal(), mpPage.getRecords());
    }

    public List<SysRole> listAll() {
        return roleMapper.selectList(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getStatus, 1));
    }

    @Transactional(rollbackFor = Exception.class)
    public SysRole create(RoleDTO dto) {
        SysRole role = new SysRole();
        role.setName(dto.getName());
        role.setCode(dto.getCode());
        role.setDescription(dto.getDescription());
        role.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        roleMapper.insert(role);

        if (dto.getPermissionIds() != null && !dto.getPermissionIds().isEmpty()) {
            assignPermissions(role.getId(), dto.getPermissionIds());
        }
        return role;
    }

    @Transactional(rollbackFor = Exception.class)
    public SysRole update(RoleDTO dto) {
        SysRole role = roleMapper.selectById(dto.getId());
        if (role == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Role not found");
        }
        role.setName(dto.getName());
        role.setDescription(dto.getDescription());
        role.setStatus(dto.getStatus());
        roleMapper.updateById(role);

        // Reassign permissions
        rolePermissionMapper.delete(new LambdaQueryWrapper<SysRolePermission>()
                .eq(SysRolePermission::getRoleId, role.getId()));
        if (dto.getPermissionIds() != null && !dto.getPermissionIds().isEmpty()) {
            assignPermissions(role.getId(), dto.getPermissionIds());
        }
        return role;
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        roleMapper.deleteById(id);
        rolePermissionMapper.delete(new LambdaQueryWrapper<SysRolePermission>()
                .eq(SysRolePermission::getRoleId, id));
    }

    private void assignPermissions(Long roleId, List<Long> permissionIds) {
        for (Long permId : permissionIds) {
            SysRolePermission rp = new SysRolePermission();
            rp.setRoleId(roleId);
            rp.setPermissionId(permId);
            rolePermissionMapper.insert(rp);
        }
    }
}
