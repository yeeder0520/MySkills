package com.company.order.domain.valueobject;

/**
 * Customer 識別碼。用 record 包裝原始型別避免 primitive obsession。
 */
public record CustomerId(String value) {
    public CustomerId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("CustomerId 不可為空");
        }
    }
}
