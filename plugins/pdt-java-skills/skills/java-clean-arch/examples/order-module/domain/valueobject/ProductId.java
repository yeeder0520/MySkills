package com.company.order.domain.valueobject;

public record ProductId(String value) {
    public ProductId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ProductId 不可為空");
        }
    }
}
