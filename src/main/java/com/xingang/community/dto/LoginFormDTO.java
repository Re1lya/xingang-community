package com.xingang.community.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginFormDTO {

    @NotBlank(message = "手机号不能为空")
    private String phone;

    private String code;

    private String password;
}
