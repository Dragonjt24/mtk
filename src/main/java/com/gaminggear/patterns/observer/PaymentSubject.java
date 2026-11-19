package com.gaminggear.patterns.observer;

/**
 * Subject interface for managing PaymentObserver subscriptions and event broadcasting.
 */
public interface PaymentSubject {

    void registerObserver(PaymentObserver observer);

    void unregisterObserver(PaymentObserver observer);

    void notifyObservers(PaymentEvent event);
}
