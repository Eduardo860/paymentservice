package com.microservices.brokermessage.repository;

import com.microservices.brokermessage.model.PaymentRetryJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentRetryJobRepository extends JpaRepository<PaymentRetryJob, UUID> {

    List<PaymentRetryJob> findByStatusAndNextRunAtLessThanEqual(String status, OffsetDateTime dateTime);
}
