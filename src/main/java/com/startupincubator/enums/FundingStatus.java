package com.startupincubator.enums;

public enum FundingStatus {
    PENDING("Pending"),
    APPROVED("Approved"),
    FUNDED("Funded"),
    REJECTED("Rejected");

    private final String displayName;

    FundingStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}