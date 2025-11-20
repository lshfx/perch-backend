package com.perch.controller;

import com.perch.entity.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 健康检查控制器
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    /**
     * Ping 接口
     * @return Pong 响应
     */
    @GetMapping("/ping")
    public Result<String> ping() {
        return Result.success("Pong! Backend is running...", "服务正常");
    }
}