package com.microservices.brokermessage.chain;

import com.microservices.brokermessage.dto.RetryContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for all Chain of Responsibility handlers.
 * Stores the next handler reference and provides a helper to cascade to it.
 */
public abstract class AbstractRetryHandler implements RetryHandler {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private RetryHandler next;

    @Override
    public void setNext(RetryHandler next) {
        this.next = next;
    }

    /**
     * Delegates to the next handler only if the context has no failure.
     * Call this at the end of each handle() implementation.
     */
    protected void handleNext(RetryContext context) {
        if (next != null && !context.isFailed()) {
            next.handle(context);
        }
    }
}
