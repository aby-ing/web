package com.example.ordersystem.controller;

import com.example.ordersystem.common.ApiResponse;
import com.example.ordersystem.dto.Dtos;
import com.example.ordersystem.service.AiService;
import com.example.ordersystem.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AiController {
    private final AiService aiService;
    private final AuthService authService;

    public AiController(AiService aiService, AuthService authService) {
        this.aiService = aiService;
        this.authService = authService;
    }

    @PostMapping("/ai/recommend")
    public ApiResponse<Dtos.AiRecommendResult> recommend(
            @RequestHeader(value = "X-Auth-Token", required = false) String token,
            @Valid @RequestBody Dtos.AiRecommendRequest request) {
        authService.requireUser(token);
        return ApiResponse.ok(aiService.recommend(request));
    }

    @PostMapping("/ai/customer-service")
    public ApiResponse<Dtos.AiTextResult> customerService(
            @RequestHeader(value = "X-Auth-Token", required = false) String token,
            @Valid @RequestBody Dtos.CustomerServiceRequest request) {
        authService.requireUser(token);
        return ApiResponse.ok(aiService.customerService(request));
    }

    @PostMapping("/admin/ai/description")
    public ApiResponse<Dtos.AiTextResult> description(
            @RequestHeader(value = "X-Auth-Token", required = false) String token,
            @Valid @RequestBody Dtos.AiDescriptionRequest request) {
        authService.requireAdmin(token);
        return ApiResponse.ok(aiService.generateDescription(request));
    }

    @GetMapping("/admin/ai/reviews-analysis")
    public ApiResponse<Dtos.AiReviewAnalysis> reviewAnalysis(
            @RequestHeader(value = "X-Auth-Token", required = false) String token) {
        authService.requireAdmin(token);
        return ApiResponse.ok(aiService.analyzeReviews());
    }

    @GetMapping("/admin/ai/hot-dishes-analysis")
    public ApiResponse<Dtos.AiHotDishAnalysis> hotDishAnalysis(
            @RequestHeader(value = "X-Auth-Token", required = false) String token) {
        authService.requireAdmin(token);
        return ApiResponse.ok(aiService.analyzeHotDishes());
    }
    @PostMapping("/merchant/ai/description")
    public ApiResponse<Dtos.AiTextResult> merchantDescription(
            @RequestHeader(value = "X-Auth-Token", required = false) String token,
            @Valid @RequestBody Dtos.AiDescriptionRequest request) {
        authService.requireMerchantOrAdmin(token);
        return ApiResponse.ok(aiService.generateDescription(request));
    }

    @GetMapping("/merchant/ai/reviews-analysis")
    public ApiResponse<Dtos.AiReviewAnalysis> merchantReviewAnalysis(
            @RequestHeader(value = "X-Auth-Token", required = false) String token) {
        authService.requireMerchantOrAdmin(token);
        return ApiResponse.ok(aiService.analyzeReviews());
    }

    @GetMapping("/merchant/ai/hot-dishes-analysis")
    public ApiResponse<Dtos.AiHotDishAnalysis> merchantHotDishAnalysis(
            @RequestHeader(value = "X-Auth-Token", required = false) String token) {
        authService.requireMerchantOrAdmin(token);
        return ApiResponse.ok(aiService.analyzeHotDishes());
    }
}
