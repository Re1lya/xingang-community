package com.xingang.community.vo;

import lombok.Data;

@Data
public class UserLoginVO {

    private String token;
    private Long userId;
    private String nickName;
}
