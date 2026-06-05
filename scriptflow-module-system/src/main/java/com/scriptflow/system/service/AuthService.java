package com.scriptflow.system.service;

import com.scriptflow.common.exception.BusinessException;
import com.scriptflow.common.result.ResultCode;
import com.scriptflow.common.util.StringUtils;
import com.scriptflow.dal.entity.system.SysUser;
import com.scriptflow.dal.mapper.system.SysUserMapper;
import com.scriptflow.system.dto.LoginDTO;
import com.scriptflow.system.dto.LoginResultVO;
import com.scriptflow.system.dto.RegisterDTO;
import com.scriptflow.system.dto.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authentication service: login, register, token management.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper userMapper;

    /**
     * User login.
     */
    public LoginResultVO login(LoginDTO dto) {
        SysUser user = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, dto.getUsername())
                        .eq(SysUser::getDeleted, 0));

        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "Invalid username or password");
        }
        if (user.getStatus() == 0) {
            throw new BusinessException(ResultCode.FORBIDDEN, "Account has been disabled");
        }

        // Verify password (simplified; use BCrypt in production)
        if (!user.getPassword().equals(dto.getPassword())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "Invalid username or password");
        }

        // Simulate token generation (Sa-Token would be used in production)
        String token = "sf_" + user.getId() + "_" + System.currentTimeMillis();

        UserVO userVO = toUserVO(user);
        return new LoginResultVO(token, userVO);
    }

    /**
     * User registration.
     */
    @Transactional(rollbackFor = Exception.class)
    public UserVO register(RegisterDTO dto) {
        // Check username uniqueness
        Long count = userMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, dto.getUsername()));
        if (count > 0) {
            throw new BusinessException(ResultCode.DUPLICATE, "Username already exists");
        }

        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(dto.getPassword()); // Should be BCrypt-encoded
        user.setNickname(StringUtils.isBlank(dto.getNickname()) ? dto.getUsername() : dto.getNickname());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setStatus(1);
        userMapper.insert(user);

        return toUserVO(user);
    }

    /**
     * Get current user info.
     */
    public UserVO getUserInfo(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null || user.getDeleted() == 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "User not found");
        }
        return toUserVO(user);
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
