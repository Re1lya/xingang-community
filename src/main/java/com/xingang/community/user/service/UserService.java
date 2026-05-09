package com.xingang.community.user.service;

import com.xingang.community.dto.LoginFormDTO;
import com.xingang.community.vo.UserLoginVO;

public interface UserService {

    /**
     * 用户登录，成功返回token和用户信息。
     */
    UserLoginVO login(LoginFormDTO loginForm);
}
