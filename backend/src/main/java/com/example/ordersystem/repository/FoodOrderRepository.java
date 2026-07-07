package com.example.ordersystem.repository;

import com.example.ordersystem.domain.FoodOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FoodOrderRepository extends JpaRepository<FoodOrder, Long> {
    List<FoodOrder> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<FoodOrder> findAllByOrderByCreatedAtDesc();

    Optional<FoodOrder> findByIdAndUserId(Long id, Long userId);
}
