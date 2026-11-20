package com.gaminggear.storage;

import com.gaminggear.model.Bill;
import com.gaminggear.model.Category;
import com.gaminggear.model.Customer;
import com.gaminggear.model.InventoryLog;
import com.gaminggear.model.Order;
import com.gaminggear.model.Payment;
import com.gaminggear.model.Product;
import com.gaminggear.model.PromotionConfig;
import com.gaminggear.model.User;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared in-memory data store backed solely by Java Collections.
 * No database or JDBC/ORM technology is used.
 */
public class InMemoryDataStore {

    private final Map<String, Category> categories = new LinkedHashMap<>();
    private final Map<String, Product> products = new LinkedHashMap<>();
    private final Map<String, Customer> customers = new LinkedHashMap<>();
    private final Map<String, Order> orders = new LinkedHashMap<>();
    private final Map<String, Payment> payments = new LinkedHashMap<>();
    private final Map<String, Bill> bills = new LinkedHashMap<>();
    private final Map<String, User> users = new LinkedHashMap<>();
    private final List<InventoryLog> inventoryLogs = new ArrayList<>();
    private PromotionConfig promotionConfig = new PromotionConfig();

    public Map<String, Category> getCategories() {
        return categories;
    }

    public Map<String, Product> getProducts() {
        return products;
    }

    public Map<String, Customer> getCustomers() {
        return customers;
    }

    public Map<String, Order> getOrders() {
        return orders;
    }

    public Map<String, Payment> getPayments() {
        return payments;
    }

    public Map<String, Bill> getBills() {
        return bills;
    }

    public Map<String, User> getUsers() {
        return users;
    }

    public List<InventoryLog> getInventoryLogs() {
        return inventoryLogs;
    }

    public PromotionConfig getPromotionConfig() {
        if (promotionConfig == null) {
            promotionConfig = new PromotionConfig();
        }
        return promotionConfig;
    }

    public void setPromotionConfig(PromotionConfig promotionConfig) {
        this.promotionConfig = promotionConfig == null ? new PromotionConfig() : promotionConfig;
    }

    public synchronized void clearAll() {
        categories.clear();
        products.clear();
        customers.clear();
        orders.clear();
        payments.clear();
        bills.clear();
        users.clear();
        inventoryLogs.clear();
        promotionConfig = new PromotionConfig();
    }

    public synchronized DataSnapshot toSnapshot() {
        DataSnapshot snapshot = new DataSnapshot();
        snapshot.setCategories(new ArrayList<>(categories.values()));
        snapshot.setProducts(new ArrayList<>(products.values()));
        snapshot.setCustomers(new ArrayList<>(customers.values()));
        snapshot.setOrders(new ArrayList<>(orders.values()));
        snapshot.setPayments(new ArrayList<>(payments.values()));
        snapshot.setBills(new ArrayList<>(bills.values()));
        snapshot.setUsers(new ArrayList<>(users.values()));
        snapshot.setInventoryLogs(new ArrayList<>(inventoryLogs));
        snapshot.setPromotionConfig(getPromotionConfig());
        return snapshot;
    }

    public synchronized void loadFromSnapshot(DataSnapshot snapshot) {
        clearAll();
        if (snapshot == null) {
            return;
        }
        if (snapshot.getCategories() != null) {
            for (Category c : snapshot.getCategories()) {
                if (c != null && c.getId() != null) {
                    categories.put(c.getId(), c);
                }
            }
        }
        if (snapshot.getProducts() != null) {
            for (Product p : snapshot.getProducts()) {
                if (p != null && p.getId() != null) {
                    products.put(p.getId(), p);
                }
            }
        }
        if (snapshot.getCustomers() != null) {
            for (Customer c : snapshot.getCustomers()) {
                if (c != null && c.getId() != null) {
                    customers.put(c.getId(), c);
                }
            }
        }
        if (snapshot.getOrders() != null) {
            for (Order o : snapshot.getOrders()) {
                if (o != null && o.getId() != null) {
                    orders.put(o.getId(), o);
                }
            }
        }
        if (snapshot.getPayments() != null) {
            for (Payment p : snapshot.getPayments()) {
                if (p != null && p.getId() != null) {
                    payments.put(p.getId(), p);
                }
            }
        }
        if (snapshot.getBills() != null) {
            for (Bill b : snapshot.getBills()) {
                if (b != null && b.getId() != null) {
                    bills.put(b.getId(), b);
                }
            }
        }
        if (snapshot.getUsers() != null) {
            for (User u : snapshot.getUsers()) {
                if (u != null && u.getId() != null) {
                    users.put(u.getId(), u);
                }
            }
        }
        if (snapshot.getInventoryLogs() != null) {
            inventoryLogs.addAll(snapshot.getInventoryLogs());
        }
        if (snapshot.getPromotionConfig() != null) {
            this.promotionConfig = snapshot.getPromotionConfig();
        }
    }
}
