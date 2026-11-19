package com.gaminggear.service;

import com.gaminggear.exception.BusinessException;
import com.gaminggear.exception.ResourceNotFoundException;
import com.gaminggear.model.Customer;
import com.gaminggear.model.Order;
import com.gaminggear.model.enums.MembershipTier;
import com.gaminggear.repository.CustomerRepository;
import com.gaminggear.repository.OrderRepository;
import com.gaminggear.storage.StorageSync;
import com.gaminggear.util.CurrencyUtil;
import com.gaminggear.util.IdGenerator;
import com.gaminggear.util.ValidationUtil;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class CustomerService {

    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final StorageSync storageSync;

    public CustomerService(CustomerRepository customerRepository,
                           OrderRepository orderRepository,
                           StorageSync storageSync) {
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
        this.storageSync = storageSync;
    }

    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    public Customer getCustomerById(String id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng: " + id));
    }

    public Customer createCustomer(String fullName, String phone, String email,
                                   String address, MembershipTier tier) {
        String cleanName = ValidationUtil.requireNonBlank(fullName, "Họ tên khách hàng");
        String cleanPhone = ValidationUtil.validatePhone(phone);
        String cleanEmail = ValidationUtil.validateEmail(email);
        String cleanAddress = ValidationUtil.requireNonBlank(address, "Địa chỉ");

        if (customerRepository.findByPhone(cleanPhone).isPresent()) {
            throw new BusinessException("Số điện thoại đã được đăng ký cho khách hàng khác.");
        }

        Customer customer = new Customer(
                IdGenerator.nextId("CUS"),
                cleanName,
                cleanPhone,
                cleanEmail,
                cleanAddress,
                tier == null ? MembershipTier.NORMAL : tier,
                BigDecimal.ZERO
        );
        customerRepository.save(customer);
        persist();
        return customer;
    }

    public Customer updateCustomer(String id, String fullName, String phone, String email,
                                   String address, MembershipTier tier) {
        Customer existing = getCustomerById(id);
        String cleanName = ValidationUtil.requireNonBlank(fullName, "Họ tên khách hàng");
        String cleanPhone = ValidationUtil.validatePhone(phone);
        String cleanEmail = ValidationUtil.validateEmail(email);
        String cleanAddress = ValidationUtil.requireNonBlank(address, "Địa chỉ");

        customerRepository.findByPhone(cleanPhone)
                .filter(c -> !c.getId().equals(id))
                .ifPresent(c -> {
                    throw new BusinessException("Số điện thoại đã thuộc về khách hàng khác.");
                });

        existing.setFullName(cleanName);
        existing.setPhone(cleanPhone);
        existing.setEmail(cleanEmail);
        existing.setAddress(cleanAddress);
        existing.setMembershipTier(tier == null ? existing.getMembershipTier() : tier);
        customerRepository.save(existing);
        persist();
        return existing;
    }

    public void deleteCustomer(String id) {
        getCustomerById(id);
        if (!orderRepository.findByCustomerId(id).isEmpty()) {
            throw new BusinessException("Không thể xóa khách hàng đã có lịch sử đơn hàng.");
        }
        customerRepository.deleteById(id);
        persist();
    }

    public List<Customer> searchCustomers(String keyword) {
        String q = keyword == null ? "" : keyword.trim().toLowerCase();
        if (q.isEmpty()) {
            return getAllCustomers();
        }
        return customerRepository.findAll().stream()
                .filter(c -> (c.getFullName() != null && c.getFullName().toLowerCase().contains(q))
                        || (c.getPhone() != null && c.getPhone().contains(q))
                        || (c.getEmail() != null && c.getEmail().toLowerCase().contains(q))
                        || (c.getId() != null && c.getId().toLowerCase().contains(q)))
                .collect(Collectors.toList());
    }

    public List<Order> getPurchaseHistory(String customerId) {
        getCustomerById(customerId);
        return orderRepository.findByCustomerId(customerId).stream()
                .sorted(Comparator.comparing(Order::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    public void addTotalSpent(String customerId, BigDecimal amount) {
        Customer customer = getCustomerById(customerId);
        BigDecimal safeAmount = CurrencyUtil.normalize(ValidationUtil.requireNonNegative(amount, "Số tiền chi tiêu"));
        BigDecimal current = customer.getTotalSpent() == null ? BigDecimal.ZERO : customer.getTotalSpent();
        customer.setTotalSpent(current.add(safeAmount));
        customerRepository.save(customer);
        persist();
    }

    private void persist() {
        if (storageSync != null) {
            storageSync.persistIfEnabled();
        }
    }
}
