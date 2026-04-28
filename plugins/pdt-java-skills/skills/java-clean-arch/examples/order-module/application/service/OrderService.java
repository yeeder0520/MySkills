package com.company.order.application.service;

import com.company.order.application.dto.CreateOrderCommand;
import com.company.order.application.dto.OrderResponse;
import com.company.order.application.exception.BusinessException;
import com.company.order.application.exception.ErrorCode;
import com.company.order.domain.model.Order;
import com.company.order.domain.model.OrderItem;
import com.company.order.domain.model.OrderStatus;
import com.company.order.domain.repository.OrderRepository;
import com.company.order.domain.valueobject.CustomerId;
import com.company.order.domain.valueobject.Money;
import com.company.order.domain.valueobject.ProductId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Currency;
import java.util.List;

/**
 * Order Use Cases。
 *
 * Application Service 只做「協調」：載入依賴 → 呼叫領域行為 → 持久化。
 * 業務規則（狀態轉移檢查、金額計算）封裝在 Domain Entity 內部。
 */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional(rollbackFor = Exception.class)
    public OrderResponse createOrder(CreateOrderCommand command) {
        log.info("建立訂單, customerId={}", command.customerId());

        Currency currency = Currency.getInstance("TWD");
        List<OrderItem> items = command.items().stream()
            .map(c -> OrderItem.create(
                new ProductId(c.productId()),
                c.quantity(),
                new Money(c.unitPrice(), currency)
            ))
            .toList();

        Order order = Order.create(new CustomerId(command.customerId()), items);
        Order saved = orderRepository.save(order);

        log.info("訂單已建立, orderNumber={}", saved.getOrderNumber());
        return OrderResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long orderId) {
        return orderRepository.findById(orderId)
            .map(OrderResponse::from)
            .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> listOrders(OrderStatus status, Pageable pageable) {
        return orderRepository.findByStatus(status, pageable).map(OrderResponse::from);
    }

    @Transactional(rollbackFor = Exception.class)
    public void confirmOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        order.confirm();  // 狀態轉移檢查在 Entity 內
        orderRepository.save(order);
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        order.cancel();
        orderRepository.save(order);
    }
}
