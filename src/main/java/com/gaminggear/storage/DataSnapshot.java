package com.gaminggear.storage;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gaminggear.model.Bill;
import com.gaminggear.model.Category;
import com.gaminggear.model.Customer;
import com.gaminggear.model.InventoryLog;
import com.gaminggear.model.Order;
import com.gaminggear.model.Payment;
import com.gaminggear.model.Product;
import com.gaminggear.model.User;

import java.util.ArrayList;
import java.util.List;

/**
 * Serializable container representing the full in-memory store state for JSON persistence.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DataSnapshot {
    private List<Category> categories = new ArrayList<>();
    private List<Product> products = new ArrayList<>();
    private List<Customer> customers = new ArrayList<>();
    private List<Order> orders = new ArrayList<>();
    private List<Payment> payments = new ArrayList<>();
    private List<Bill> bills = new ArrayList<>();
    private List<User> users = new ArrayList<>();
    private List<InventoryLog> inventoryLogs = new ArrayList<>();

    public List<Category> getCategories() {
        return categories;
    }

    public void setCategories(List<Category> categories) {
        this.categories = categories == null ? new ArrayList<>() : categories;
    }

    public List<Product> getProducts() {
        return products;
    }

    public void setProducts(List<Product> products) {
        this.products = products == null ? new ArrayList<>() : products;
    }

    public List<Customer> getCustomers() {
        return customers;
    }

    public void setCustomers(List<Customer> customers) {
        this.customers = customers == null ? new ArrayList<>() : customers;
    }

    public List<Order> getOrders() {
        return orders;
    }

    public void setOrders(List<Order> orders) {
        this.orders = orders == null ? new ArrayList<>() : orders;
    }

    public List<Payment> getPayments() {
        return payments;
    }

    public void setPayments(List<Payment> payments) {
        this.payments = payments == null ? new ArrayList<>() : payments;
    }

    public List<Bill> getBills() {
        return bills;
    }

    public void setBills(List<Bill> bills) {
        this.bills = bills == null ? new ArrayList<>() : bills;
    }

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users == null ? new ArrayList<>() : users;
    }

    public List<InventoryLog> getInventoryLogs() {
        return inventoryLogs;
    }

    public void setInventoryLogs(List<InventoryLog> inventoryLogs) {
        this.inventoryLogs = inventoryLogs == null ? new ArrayList<>() : inventoryLogs;
    }
}
