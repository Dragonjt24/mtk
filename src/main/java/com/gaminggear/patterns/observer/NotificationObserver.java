package com.gaminggear.patterns.observer;

import com.gaminggear.util.CurrencyUtil;
import com.gaminggear.util.DateTimeUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Observer responsible for recording and forwarding payment success notifications.
 */
public class NotificationObserver implements PaymentObserver {

    private final List<String> notifications = new ArrayList<>();
    private Consumer<String> uiListener;

    public void setUiListener(Consumer<String> uiListener) {
        this.uiListener = uiListener;
    }

    @Override
    public synchronized void onPaymentSuccess(PaymentEvent event) {
        if (event == null || event.getOrder() == null || event.getPayment() == null) {
            return;
        }
        String message = String.format(
                "[%s] Thanh toán thành công đơn hàng %s (%s) - Khách hàng: %s - Số tiền: %s",
                DateTimeUtil.format(event.getOccurredAt()),
                event.getOrder().getId(),
                event.getPayment().getMethod().name(),
                event.getOrder().getCustomerName(),
                CurrencyUtil.formatVnd(event.getOrder().getFinalTotal())
        );
        notifications.add(0, message);
        if (uiListener != null) {
            uiListener.accept(message);
        }
    }

    public synchronized List<String> getNotifications() {
        return Collections.unmodifiableList(new ArrayList<>(notifications));
    }
}
