package com.company.order.application.dto;

import java.math.BigDecimal;

public record OrderItemCommand(
    String productId,
    int quantity,
    BigDecimal unitPrice
) {}
