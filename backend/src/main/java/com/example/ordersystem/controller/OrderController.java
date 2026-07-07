package com.example.ordersystem.controller;

import com.example.ordersystem.common.ApiResponse;
import com.example.ordersystem.dto.Dtos;
import com.example.ordersystem.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/orders")
    public ApiResponse<Dtos.OrderView> create(
            @RequestHeader(value = "X-Auth-Token", required = false) String token,
            @Valid @RequestBody Dtos.OrderCreateRequest request) {
        return ApiResponse.ok("订单已提交", orderService.create(token, request));
    }

    @GetMapping("/orders/my")
    public ApiResponse<List<Dtos.OrderView>> myOrders(
            @RequestHeader(value = "X-Auth-Token", required = false) String token) {
        return ApiResponse.ok(orderService.myOrders(token));
    }

    @GetMapping("/admin/orders")
    public ApiResponse<List<Dtos.OrderView>> adminOrders(
            @RequestHeader(value = "X-Auth-Token", required = false) String token) {
        return ApiResponse.ok(orderService.adminOrders(token));
    }

    @PutMapping("/admin/orders/{id}/status")
    public ApiResponse<Dtos.OrderView> updateStatus(
            @RequestHeader(value = "X-Auth-Token", required = false) String token,
            @PathVariable Long id,
            @Valid @RequestBody Dtos.OrderStatusRequest request) {
        return ApiResponse.ok("订单状态已更新", orderService.updateStatus(token, id, request));
    }
}
