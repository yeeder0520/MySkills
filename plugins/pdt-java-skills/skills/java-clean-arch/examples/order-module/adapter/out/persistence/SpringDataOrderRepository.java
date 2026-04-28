package com.company.order.adapter.out.persistence;

import com.company.order.domain.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA 介面。
 *
 * 不對外暴露，只給 JpaOrderRepository 用。
 * 用 @EntityGraph 避免 N+1。
 */
interface SpringDataOrderRepository extends JpaRepository<OrderEntity, Long> {

    Optional<OrderEntity> findByOrderNumber(String orderNumber);

    List<OrderEntity> findByCustomerId(String customerId);

    @EntityGraph(attributePaths = {"items"})
    Page<OrderEntity> findByStatus(OrderStatus status, Pageable pageable);
}
