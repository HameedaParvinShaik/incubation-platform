package com.startupincubator.enums;

public enum StartupStatus {
    PENDING,
    APPROVED,
    ACTIVE,
    COMPLETED,
    REJECTED,
    IN_PROGRESS,
    INACTIVE;

    public String getDisplayName() {
        return this.name();
    }

    public String getColor() {
        return switch (this) {
            case PENDING -> "warning";
            case APPROVED -> "info";
            case ACTIVE -> "success";
            case COMPLETED -> "primary";
            case REJECTED -> "danger";
            case IN_PROGRESS -> "warning";
            case INACTIVE -> "secondary";
        };
    }

    public static StartupStatus fromString(String status) {
        for (StartupStatus s : StartupStatus.values()) {
            if (s.name().equalsIgnoreCase(status)) {
                return s;
            }
        }
        return PENDING;
    }
}