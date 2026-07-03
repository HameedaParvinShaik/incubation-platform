package com.startupincubator.enums;

public enum RoleType {
    ROLE_ADMIN,
    ROLE_MANAGER,
    ROLE_MENTOR,
    ROLE_FOUNDER,
    ROLE_EVALUATOR,
    ROLE_INVESTOR,
    ROLE_TEAM_MEMBER;

    public String getDisplayName() {
        return this.name().replace("ROLE_", "");
    }
}