package com.xingang.community;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.xingang.community.**.mapper")
public class XingangCommunityApplication {

    public static void main(String[] args) {
        SpringApplication.run(XingangCommunityApplication.class, args);
    }
}

