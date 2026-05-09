package com.xingang.community.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xingang.community.common.constant.ErrorCode;
import com.xingang.community.common.constant.RedisConstants;
import com.xingang.community.common.exception.BusinessException;
import com.xingang.community.dto.LoginFormDTO;
import com.xingang.community.entity.User;
import com.xingang.community.user.mapper.UserMapper;
import com.xingang.community.user.service.UserService;
import com.xingang.community.vo.UserLoginVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 用户登录服务。当前为最小实现骨架：
 * - 根据手机号查找用户，不存在则自动注册
 * - 生成UUID token写入Redis
 *
 * <p>后续可由产品经理扩展为完整验证码/密码校验流程。</p>
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public UserLoginVO login(LoginFormDTO loginForm) {
        String phone = loginForm.getPhone();
        if (!StringUtils.hasText(phone)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "手机号不能为空");
        }

        // 按手机号查询用户
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getPhone, phone)
        );

        // 简化注册：不存在则自动创建
        if (user == null) {
            user = new User();
            user.setPhone(phone);
            user.setNickName("用户" + phone.substring(phone.length() - 4));
            userMapper.insert(user);
        }

        // 生成token
        String token = UUID.randomUUID().toString().replace("-", "");
        String tokenKey = RedisConstants.LOGIN_TOKEN_KEY + token;

        // 写入Redis（简化版：只存userId）
        stringRedisTemplate.opsForValue().set(
                tokenKey,
                String.valueOf(user.getId()),
                RedisConstants.LOGIN_TOKEN_TTL_MINUTES,
                TimeUnit.MINUTES
        );

        UserLoginVO vo = new UserLoginVO();
        vo.setToken(token);
        vo.setUserId(user.getId());
        vo.setNickName(user.getNickName());

        log.info("User login success: userId={}, phone={}", user.getId(), phone);
        return vo;
    }
}
