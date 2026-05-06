package com.microservices.brokermessage.repository;

import com.microservices.brokermessage.model.OrderRetryJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface OrderRetryJobRepository extends JpaRepository<OrderRetryJob, UUID> {

    List<OrderRetryJob> findByStatusAndNextRunAtLessThanEqual(String status, OffsetDateTime dateTime);
    
    List<OrderRetryJob> findByOrderIdAndActionAndStatusIn(String orderId, String action, List<String> statuses);
}
