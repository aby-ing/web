package com.example.ordersystem.repository;

import com.example.ordersystem.domain.Dish;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DishRepository extends JpaRepository<Dish, Long> {
    @Query("""
            select d from Dish d
            where (:onlyAvailable = false or (d.available = true and d.category.enabled = true))
              and (:categoryId is null or d.category.id = :categoryId)
              and (:keyword is null or lower(d.name) like lower(concat('%', :keyword, '%'))
                   or lower(d.ingredients) like lower(concat('%', :keyword, '%'))
                   or lower(d.tasteTags) like lower(concat('%', :keyword, '%')))
            order by d.available desc, d.sales desc, d.id asc
            """)
    List<Dish> search(@Param("keyword") String keyword,
                      @Param("categoryId") Long categoryId,
                      @Param("onlyAvailable") boolean onlyAvailable);

    List<Dish> findTop6ByAvailableTrueOrderBySalesDescIdAsc();

    boolean existsByCategoryIdAndAvailableTrue(Long categoryId);
}
