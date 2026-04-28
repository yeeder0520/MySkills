package com.company.order.application.dto;

import com.company.order.domain.model.Order;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderResponse(
    Long id,
    String orderNumber,
    BigDecimal totalAmount,
    String status,
    LocalDateTime createdAt
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
            order.getId(),
            order.getOrderNumber(),
            order.getTotalAmount().amount(),
            order.getStatus().name(),
            order.getCreatedAt()
        );
    }
}
