package com.example.ordersystem.repository;

import com.example.ordersystem.domain.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByDishIdOrderByCreatedAtDesc(Long dishId);

    List<Review> findAllByOrderByCreatedAtDesc();
}
