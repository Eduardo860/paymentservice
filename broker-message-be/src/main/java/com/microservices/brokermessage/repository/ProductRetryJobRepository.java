package com.microservices.brokermessage.repository;

import com.microservices.brokermessage.model.ProductRetryJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ProductRetryJobRepository extends JpaRepository<ProductRetryJob, UUID> {

    List<ProductRetryJob> findByStatusAndNextRunAtLessThanEqual(String status, OffsetDateTime dateTime);
    
    List<ProductRetryJob> findByProductIdAndActionAndStatusIn(String productId, String action, List<String> statuses);
}
