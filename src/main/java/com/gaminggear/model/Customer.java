package com.gaminggear.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gaminggear.model.enums.MembershipTier;

import java.math.BigDecimal;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Customer {
    private String id;
    private String fullName;
    private String phone;
    private String email;
    private String address;
    private MembershipTier membershipTier;
    private BigDecimal totalSpent;

    public Customer() {
        this.membershipTier = MembershipTier.NORMAL;
        this.totalSpent = BigDecimal.ZERO;
    }

    public Customer(String id, String fullName, String phone, String email,
                    String address, MembershipTier membershipTier, BigDecimal totalSpent) {
        this.id = id;
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.membershipTier = membershipTier == null ? MembershipTier.NORMAL : membershipTier;
        this.totalSpent = totalSpent == null ? BigDecimal.ZERO : totalSpent;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public MembershipTier getMembershipTier() {
        return membershipTier;
    }

    public void setMembershipTier(MembershipTier membershipTier) {
        this.membershipTier = membershipTier;
    }

    public BigDecimal getTotalSpent() {
        return totalSpent;
    }

    public void setTotalSpent(BigDecimal totalSpent) {
        this.totalSpent = totalSpent;
    }

    @Override
    public String toString() {
        return fullName + " [" + membershipTier.name() + "] - " + phone;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Customer customer)) return false;
        return Objects.equals(id, customer.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
