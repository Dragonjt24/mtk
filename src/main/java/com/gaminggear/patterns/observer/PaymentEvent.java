package com.gaminggear.patterns.observer;

import com.gaminggear.model.Order;
import com.gaminggear.model.Payment;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Immutable event payload published when an order payment succeeds.
 */
public final class PaymentEvent {

    private final String eventId;
    private final Order order;
    private final Payment payment;
    private final LocalDateTime occurredAt;

    public PaymentEvent(String eventId, Order order, Payment payment, LocalDateTime occurredAt) {
        this.eventId = Objects.requireNonNull(eventId, "eventId must not be null");
        this.order = Objects.requireNonNull(order, "order must not be null");
        this.payment = Objects.requireNonNull(payment, "payment must not be null");
        this.occurredAt = occurredAt == null ? LocalDateTime.now() : occurredAt;
    }

    public String getEventId() {
        return eventId;
    }

    public Order getOrder() {
        return order;
    }

    public Payment getPayment() {
        return payment;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }
}
