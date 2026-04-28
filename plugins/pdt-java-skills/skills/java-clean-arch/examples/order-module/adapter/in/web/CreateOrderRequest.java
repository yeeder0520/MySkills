package com.company.order.adapter.in.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public record CreateOrderRequest(
    @NotBlank(message = "客戶 ID 為必填")
    String customerId,

    @NotEmpty(message = "訂單項目為必填")
    @Valid
    List<OrderItemRequest> items,

    @NotBlank(message = "配送地址為必填")
    String shippingAddress
) {}
