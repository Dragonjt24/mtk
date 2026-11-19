package com.gaminggear.patterns.observer;

/**
 * Observer interface for components reacting to successful payment events.
 */
public interface PaymentObserver {

    void onPaymentSuccess(PaymentEvent event);
}
