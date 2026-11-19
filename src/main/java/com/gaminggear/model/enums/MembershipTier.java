package com.gaminggear.model.enums;

public enum MembershipTier {
    NORMAL("Thường"),
    VIP("VIP");

    private final String displayName;

    MembershipTier(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
