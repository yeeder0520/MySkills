package com.company.order.domain.model;

import com.company.order.domain.valueobject.CustomerId;
import com.company.order.domain.valueobject.Money;
import com.company.order.application.exception.BusinessException;
import com.company.order.application.exception.ErrorCode;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Order Aggregate Root.
 *
 * 純 Java 實作，不依賴任何框架。
 * 業務行為（confirm、cancel）封裝於 Entity 內部，狀態轉移檢查也在這裡。
 */
public class Order {
    private Long id;
    private String orderNumber;
    private CustomerId customerId;
    private List<OrderItem> items;
    private Money totalAmount;
    private OrderStatus status;
    private LocalDateTime createdAt;

    // JPA / 反序列化用
    protected Order() {}

    private Order(Long id, String orderNumber, CustomerId customerId,
                  List<OrderItem> items, Money totalAmount,
                  OrderStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.orderNumber = orderNumber;
        this.customerId = customerId;
        this.items = items;
        this.totalAmount = totalAmount;
        this.status = status;
        this.createdAt = createdAt;
    }

    /** 工廠方法：建立新訂單 */
    public static Order create(CustomerId customerId, List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            throw new BusinessException(ErrorCode.ORDER_ITEMS_REQUIRED);
        }
        return new Order(
            null,
            generateOrderNumber(),
            customerId,
            new ArrayList<>(items),
            calculateTotal(items),
            OrderStatus.PENDING,
            LocalDateTime.now()
        );
    }

    /** 從持久化還原（Repository 層使用） */
    public static Order reconstitute(Long id, String orderNumber, CustomerId customerId,
                                     List<OrderItem> items, Money totalAmount,
                                     OrderStatus status, LocalDateTime createdAt) {
        return new Order(id, orderNumber, customerId, items, totalAmount, status, createdAt);
    }

    public void confirm() {
        if (this.status != OrderStatus.PENDING) {
            throw new BusinessException(ErrorCode.ORDER_CANNOT_BE_CONFIRMED);
        }
        this.status = OrderStatus.CONFIRMED;
    }

    public void cancel() {
        if (this.status == OrderStatus.SHIPPED) {
            throw new BusinessException(ErrorCode.ORDER_CANNOT_BE_CANCELLED);
        }
        this.status = OrderStatus.CANCELLED;
    }

    private static Money calculateTotal(List<OrderItem> items) {
        return items.stream()
            .map(OrderItem::getSubtotal)
            .reduce(Money.ZERO, Money::add);
    }

    private static String generateOrderNumber() {
        return "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    // Getters
    public Long getId() { return id; }
    public String getOrderNumber() { return orderNumber; }
    public CustomerId getCustomerId() { return customerId; }
    public List<OrderItem> getItems() { return Collections.unmodifiableList(items); }
    public Money getTotalAmount() { return totalAmount; }
    public OrderStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
