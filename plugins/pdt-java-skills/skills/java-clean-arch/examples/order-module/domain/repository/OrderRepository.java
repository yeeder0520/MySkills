package com.company.order.domain.repository;

import com.company.order.domain.model.Order;
import com.company.order.domain.model.OrderStatus;
import com.company.order.domain.valueobject.CustomerId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * Order Repository 介面。
 *
 * 依賴反轉：介面定義在 Domain 層，實作放在 Adapter 層的 persistence 套件。
 *
 * 此處依賴 Spring Data 的 Page / Pageable 介面屬於可接受妥協（介面而非實作），
 * 嚴格純淨派可自定義 PageQuery / PageResult 包裝。
 */
public interface OrderRepository {

    Order save(Order order);

    Optional<Order> findById(Long id);

    Optional<Order> findByOrderNumber(String orderNumber);

    List<Order> findByCustomerId(CustomerId customerId);

    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    void delete(Order order);
}
