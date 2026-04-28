package com.company.order.adapter.out.persistence;

import com.company.order.domain.model.Order;
import com.company.order.domain.model.OrderStatus;
import com.company.order.domain.repository.OrderRepository;
import com.company.order.domain.valueobject.CustomerId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * OrderRepository 的 JPA 實作。
 *
 * 負責 Domain Order ↔ JPA OrderEntity 的轉換。
 * Domain 完全不知道 JPA 的存在。
 */
@Repository
public class JpaOrderRepository implements OrderRepository {

    private final SpringDataOrderRepository jpaRepo;

    public JpaOrderRepository(SpringDataOrderRepository jpaRepo) {
        this.jpaRepo = jpaRepo;
    }

    @Override
    public Order save(Order order) {
        OrderEntity entity = OrderEntity.from(order);
        return jpaRepo.save(entity).toDomain();
    }

    @Override
    public Optional<Order> findById(Long id) {
        return jpaRepo.findById(id).map(OrderEntity::toDomain);
    }

    @Override
    public Optional<Order> findByOrderNumber(String orderNumber) {
        return jpaRepo.findByOrderNumber(orderNumber).map(OrderEntity::toDomain);
    }

    @Override
    public List<Order> findByCustomerId(CustomerId customerId) {
        return jpaRepo.findByCustomerId(customerId.value()).stream()
            .map(OrderEntity::toDomain)
            .toList();
    }

    @Override
    public Page<Order> findByStatus(OrderStatus status, Pageable pageable) {
        return jpaRepo.findByStatus(status, pageable).map(OrderEntity::toDomain);
    }

    @Override
    public void delete(Order order) {
        jpaRepo.deleteById(order.getId());
    }
}
