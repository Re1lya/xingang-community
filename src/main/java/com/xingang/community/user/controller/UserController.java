package com.xingang.community.user.controller;

import com.xingang.community.common.result.Result;
import com.xingang.community.dto.LoginFormDTO;
import com.xingang.community.user.service.UserService;
import com.xingang.community.vo.UserLoginVO;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户登录接口。
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    @PostMapping("/login")
    public Result<UserLoginVO> login(@Valid @RequestBody LoginFormDTO loginForm) {
        UserLoginVO vo = userService.login(loginForm);
        return Result.ok(vo);
    }
}
