package com.microservices.brokermessage.chain;

import com.microservices.brokermessage.dto.RetryContext;

/**
 * Chain of Responsibility contract.
 * Each handler processes its step and delegates to the next if no failure occurred.
 */
public interface RetryHandler {

    void handle(RetryContext context);

    void setNext(RetryHandler next);
}
