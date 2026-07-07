package com.example.ordersystem.controller;

import com.example.ordersystem.common.ApiResponse;
import com.example.ordersystem.service.AuthService;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
public class SystemController {
    private final AuthService authService;
    private final ConfigurableApplicationContext applicationContext;

    public SystemController(AuthService authService, ConfigurableApplicationContext applicationContext) {
        this.authService = authService;
        this.applicationContext = applicationContext;
    }

    @PostMapping("/shutdown")
    public ApiResponse<Void> shutdown(@RequestHeader(value = "X-Auth-Token", required = false) String token) {
        authService.requireAdmin(token);
        Thread shutdownThread = new Thread(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            int exitCode = SpringApplication.exit(applicationContext, () -> 0);
            System.exit(exitCode);
        }, "app-shutdown");
        shutdownThread.setDaemon(false);
        shutdownThread.start();
        return ApiResponse.ok("系统正在停止", null);
    }
}
