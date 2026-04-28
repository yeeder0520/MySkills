package com.company.order.application.dto;

import java.util.List;

public record CreateOrderCommand(
    String customerId,
    List<OrderItemCommand> items,
    String shippingAddress
) {}
