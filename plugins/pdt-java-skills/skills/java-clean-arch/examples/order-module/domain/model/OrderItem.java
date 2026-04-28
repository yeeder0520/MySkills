package com.company.order.domain.model;

import com.company.order.domain.valueobject.Money;
import com.company.order.domain.valueobject.ProductId;

/**
 * Order 內的項目。屬於 Order Aggregate 的一部分，不可單獨存在。
 */
public class OrderItem {
    private Long id;
    private ProductId productId;
    private int quantity;
    private Money unitPrice;

    protected OrderItem() {}

    private OrderItem(ProductId productId, int quantity, Money unitPrice) {
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public static OrderItem create(ProductId productId, int quantity, Money unitPrice) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("數量必須大於 0");
        }
        return new OrderItem(productId, quantity, unitPrice);
    }

    public Money getSubtotal() {
        return unitPrice.multiply(quantity);
    }

    public Long getId() { return id; }
    public ProductId getProductId() { return productId; }
    public int getQuantity() { return quantity; }
    public Money getUnitPrice() { return unitPrice; }
}
