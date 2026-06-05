package com.scriptflow.system.service;

import cn.dev33.satoken.stp.StpUtil;
import com.scriptflow.common.constant.GlobalConstants;
import com.scriptflow.common.exception.BusinessException;
import com.scriptflow.common.result.ResultCode;
import com.scriptflow.common.util.BeanCopyUtils;
import com.scriptflow.common.util.PasswordEncoder;
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
     * User login with BCrypt password verification and Sa-Token session.
     */
    public LoginResultVO login(LoginDTO dto) {
        SysUser user = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, dto.getUsername())
                        .eq(SysUser::getDeleted, 0));

        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "Invalid username or password");
        }
        if (user.getStatus() == GlobalConstants.Status.DISABLED) {
            throw new BusinessException(ResultCode.FORBIDDEN, "Account has been disabled");
        }

        // Verify password using BCrypt
        if (!PasswordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "Invalid username or password");
        }

        // Create Sa-Token session
        StpUtil.login(user.getId());
        String token = StpUtil.getTokenValue();

        UserVO userVO = BeanCopyUtils.copy(user, UserVO.class);
        return new LoginResultVO(token, userVO);
    }

    /**
     * User registration with BCrypt-encoded password.
     */
    @Transactional(rollbackFor = Exception.class)
    public UserVO register(RegisterDTO dto) {
        Long count = userMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, dto.getUsername()));
        if (count > 0) {
            throw new BusinessException(ResultCode.DUPLICATE, "Username already exists");
        }

        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(PasswordEncoder.encode(dto.getPassword()));
        user.setNickname(StringUtils.isBlank(dto.getNickname()) ? dto.getUsername() : dto.getNickname());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setStatus(GlobalConstants.Status.ENABLED);
        userMapper.insert(user);

        return BeanCopyUtils.copy(user, UserVO.class);
    }

    /**
     * Get current user info from Sa-Token session.
     */
    public UserVO getCurrentUserInfo() {
        long userId = StpUtil.getLoginIdAsLong();
        SysUser user = userMapper.selectById(userId);
        if (user == null || user.getDeleted() == 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "User not found");
        }
        return BeanCopyUtils.copy(user, UserVO.class);
    }

    /**
     * Get user info by ID.
     */
    public UserVO getUserInfo(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null || user.getDeleted() == 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "User not found");
        }
        return BeanCopyUtils.copy(user, UserVO.class);
    }

    /**
     * Logout: clear Sa-Token session.
     */
    public void logout() {
        StpUtil.logout();
    }
}
