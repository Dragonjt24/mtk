package com.gaminggear.patterns.observer;

import com.gaminggear.service.BillService;

import java.util.HashSet;
import java.util.Set;

/**
 * Observer responsible for creating an official Bill when payment succeeds.
 * Guarantees idempotency so duplicate events never generate duplicate invoices.
 */
public class BillObserver implements PaymentObserver {

    private final BillService billService;
    private final Set<String> processedOrderIds = new HashSet<>();

    public BillObserver(BillService billService) {
        this.billService = billService;
    }

    @Override
    public synchronized void onPaymentSuccess(PaymentEvent event) {
        if (event == null || event.getOrder() == null || event.getPayment() == null) {
            return;
        }
        String orderId = event.getOrder().getId();
        if (processedOrderIds.contains(orderId) || billService.findByOrderId(orderId).isPresent()) {
            processedOrderIds.add(orderId);
            return;
        }
        billService.createBillFromOrder(event.getOrder(), event.getPayment());
        processedOrderIds.add(orderId);
    }
}
