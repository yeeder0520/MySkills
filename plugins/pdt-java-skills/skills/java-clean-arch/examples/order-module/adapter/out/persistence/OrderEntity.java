package com.company.order.adapter.out.persistence;

import com.company.order.domain.model.Order;
import com.company.order.domain.model.OrderItem;
import com.company.order.domain.model.OrderStatus;
import com.company.order.domain.valueobject.CustomerId;
import com.company.order.domain.valueobject.Money;
import com.company.order.domain.valueobject.ProductId;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

/**
 * JPA Entity。
 *
 * 與 Domain Order 完全分離：
 * - JPA 註解只在這裡，不污染 Domain
 * - 透過 from() / toDomain() 雙向轉換
 * - 即使資料庫 schema 變動，Domain 也不需要改
 */
@Entity
@Table(name = "orders")
class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", unique = true, nullable = false)
    private String orderNumber;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItemEntity> items = new ArrayList<>();

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected OrderEntity() {}

    public static OrderEntity from(Order order) {
        OrderEntity e = new OrderEntity();
        e.id = order.getId();
        e.orderNumber = order.getOrderNumber();
        e.customerId = order.getCustomerId().value();
        e.totalAmount = order.getTotalAmount().amount();
        e.currency = order.getTotalAmount().currency().getCurrencyCode();
        e.status = order.getStatus();
        e.createdAt = order.getCreatedAt();
        e.items = order.getItems().stream()
            .map(item -> OrderItemEntity.from(item, e))
            .toList();
        return e;
    }

    public Order toDomain() {
        Currency curr = Currency.getInstance(currency);
        Money total = new Money(totalAmount, curr);
        List<OrderItem> domainItems = items.stream()
            .map(OrderItemEntity::toDomain)
            .toList();

        return Order.reconstitute(
            id,
            orderNumber,
            new CustomerId(customerId),
            domainItems,
            total,
            status,
            createdAt
        );
    }
}
