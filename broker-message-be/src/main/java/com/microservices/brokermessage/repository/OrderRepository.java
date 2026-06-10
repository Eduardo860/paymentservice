package com.microservices.brokermessage.repository;

import com.microservices.brokermessage.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {
    List<Order> findByUserId(Long userId);
    List<Order> findByStatus(String status);
}
