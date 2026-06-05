package com.scriptflow.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scriptflow.common.exception.BusinessException;
import com.scriptflow.common.result.ResultCode;
import com.scriptflow.common.util.BeanCopyUtils;
import com.scriptflow.common.util.PageUtils;
import com.scriptflow.dal.entity.system.SysUser;
import com.scriptflow.dal.mapper.system.SysUserMapper;
import com.scriptflow.framework.service.BaseService;
import com.scriptflow.framework.service.Converter;
import com.scriptflow.system.dto.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService extends BaseService<SysUser, UserVO> {

    private final SysUserMapper userMapper;

    @Override
    protected BaseMapper<SysUser> getMapper() {
        return userMapper;
    }

    @Override
    protected Converter<SysUser, UserVO> getConverter() {
        return entity -> BeanCopyUtils.copy(entity, UserVO.class);
    }

    @Override
    public UserVO getById(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null || user.getDeleted() == 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "User not found");
        }
        return getConverter().convert(user);
    }

    public PageUtils<UserVO> page(int page, int pageSize, String keyword) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .orderByDesc(SysUser::getCreateTime);
        if (keyword != null) {
            wrapper.like(SysUser::getUsername, keyword)
                    .or()
                    .like(SysUser::getNickname, keyword)
                    .or()
                    .like(SysUser::getEmail, keyword);
        }
        return super.page(page, pageSize, wrapper);
    }

    public void updateStatus(Long id, Integer status) {
        SysUser user = findByIdOrThrow(id);
        user.setStatus(status);
        userMapper.updateById(user);
    }
}
