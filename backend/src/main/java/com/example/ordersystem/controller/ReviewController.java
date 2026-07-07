package com.example.ordersystem.controller;

import com.example.ordersystem.common.ApiResponse;
import com.example.ordersystem.dto.Dtos;
import com.example.ordersystem.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {
    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    public ApiResponse<Dtos.ReviewView> create(
            @RequestHeader(value = "X-Auth-Token", required = false) String token,
            @Valid @RequestBody Dtos.ReviewRequest request) {
        return ApiResponse.ok("评价已提交", reviewService.create(token, request));
    }

    @GetMapping
    public ApiResponse<List<Dtos.ReviewView>> list(@RequestParam(required = false) Long dishId) {
        return ApiResponse.ok(reviewService.list(dishId));
    }
}
