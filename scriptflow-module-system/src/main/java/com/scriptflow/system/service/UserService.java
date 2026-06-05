package com.scriptflow.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scriptflow.common.exception.BusinessException;
import com.scriptflow.common.result.ResultCode;
import com.scriptflow.common.util.PageUtils;
import com.scriptflow.dal.entity.system.SysUser;
import com.scriptflow.dal.mapper.system.SysUserMapper;
import com.scriptflow.system.dto.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * User management service.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final SysUserMapper userMapper;

    public PageUtils<UserVO> page(int page, int pageSize, String keyword) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .like(SysUser::getUsername, keyword)
                .or()
                .like(SysUser::getNickname, keyword)
                .or()
                .like(SysUser::getEmail, keyword)
                .orderByDesc(SysUser::getCreateTime);

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<SysUser> mpPage =
                userMapper.selectPage(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, pageSize), wrapper);

        List<UserVO> records = mpPage.getRecords().stream()
                .map(this::toUserVO)
                .toList();
        return PageUtils.of((int) mpPage.getCurrent(), (int) mpPage.getSize(),
                (int) mpPage.getTotal(), records);
    }

    public UserVO getById(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null || user.getDeleted() == 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "User not found");
        }
        return toUserVO(user);
    }

    public void updateStatus(Long id, Integer status) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "User not found");
        }
        user.setStatus(status);
        userMapper.updateById(user);
    }

    private UserVO toUserVO(SysUser user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setAvatar(user.getAvatar());
        vo.setStatus(user.getStatus());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }
}
