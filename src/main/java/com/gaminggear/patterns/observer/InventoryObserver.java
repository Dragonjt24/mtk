package com.gaminggear.patterns.observer;

import com.gaminggear.service.InventoryService;

import java.util.HashSet;
import java.util.Set;

/**
 * Observer responsible for deducting product inventory once when payment succeeds.
 */
public class InventoryObserver implements PaymentObserver {

    private final InventoryService inventoryService;
    private final Set<String> processedOrderIds = new HashSet<>();

    public InventoryObserver(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @Override
    public synchronized void onPaymentSuccess(PaymentEvent event) {
        if (event == null || event.getOrder() == null) {
            return;
        }
        String orderId = event.getOrder().getId();
        if (processedOrderIds.contains(orderId)) {
            return;
        }
        inventoryService.deductStockForOrder(event.getOrder());
        processedOrderIds.add(orderId);
    }
}
