package com.gaminggear.service;

import com.gaminggear.exception.ResourceNotFoundException;
import com.gaminggear.model.Bill;
import com.gaminggear.model.BillItem;
import com.gaminggear.model.Customer;
import com.gaminggear.model.Order;
import com.gaminggear.model.OrderItem;
import com.gaminggear.model.Payment;
import com.gaminggear.model.enums.PaymentStatus;
import com.gaminggear.repository.BillRepository;
import com.gaminggear.repository.CustomerRepository;
import com.gaminggear.storage.StorageSync;
import com.gaminggear.util.IdGenerator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class BillService {

    private final BillRepository billRepository;
    private final CustomerRepository customerRepository;
    private final StorageSync storageSync;

    public BillService(BillRepository billRepository,
                       CustomerRepository customerRepository,
                       StorageSync storageSync) {
        this.billRepository = billRepository;
        this.customerRepository = customerRepository;
        this.storageSync = storageSync;
    }

    public List<Bill> getAllBills() {
        return billRepository.findAll().stream()
                .sorted(Comparator.comparing(Bill::getIssuedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    public Bill getBillById(String id) {
        return billRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hóa đơn: " + id));
    }

    public Optional<Bill> findByOrderId(String orderId) {
        return billRepository.findByOrderId(orderId);
    }

    public synchronized Bill createBillFromOrder(Order order, Payment payment) {
        Optional<Bill> existing = billRepository.findByOrderId(order.getId());
        if (existing.isPresent()) {
            return existing.get();
        }

        Optional<Customer> customerOpt = customerRepository.findById(order.getCustomerId());
        String phone = customerOpt.map(Customer::getPhone).orElse("N/A");
        String address = customerOpt.map(Customer::getAddress).orElse("N/A");

        List<BillItem> billItems = new ArrayList<>();
        for (OrderItem item : order.getItems()) {
            billItems.add(new BillItem(
                    item.getProductId(),
                    item.getProductName(),
                    item.getQuantity(),
                    item.getUnitPrice(),
                    item.getLineTotal()
            ));
        }

        Bill bill = new Bill(
                IdGenerator.nextId("BILL"),
                order.getId(),
                order.getCustomerId(),
                order.getCustomerName(),
                phone,
                address,
                billItems,
                order.getOriginalTotal(),
                order.getDiscountType(),
                order.getDiscountRate(),
                order.getDiscountAmount(),
                order.getFinalTotal(),
                LocalDateTime.now(),
                payment.getMethod(),
                PaymentStatus.SUCCESS
        );
        billRepository.save(bill);
        persist();
        return bill;
    }

    private void persist() {
        if (storageSync != null) {
            storageSync.persistIfEnabled();
        }
    }
}
