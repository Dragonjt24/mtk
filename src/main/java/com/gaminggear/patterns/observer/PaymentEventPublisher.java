package com.gaminggear.patterns.observer;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Concrete Subject responsible solely for managing registered observers and publishing PaymentEvents.
 */
public class PaymentEventPublisher implements PaymentSubject {

    private final List<PaymentObserver> observers = new CopyOnWriteArrayList<>();

    @Override
    public void registerObserver(PaymentObserver observer) {
        if (observer != null && !observers.contains(observer)) {
            observers.add(observer);
        }
    }

    @Override
    public void unregisterObserver(PaymentObserver observer) {
        if (observer != null) {
            observers.remove(observer);
        }
    }

    @Override
    public void notifyObservers(PaymentEvent event) {
        if (event == null) {
            return;
        }
        for (PaymentObserver observer : observers) {
            observer.onPaymentSuccess(event);
        }
    }

    public List<PaymentObserver> getRegisteredObservers() {
        return Collections.unmodifiableList(observers);
    }
}
