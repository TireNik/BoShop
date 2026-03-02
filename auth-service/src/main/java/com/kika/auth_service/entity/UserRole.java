package com.kika.auth_service.entity;

public enum UserRole {
    ROLE_USER,
    ROLE_MANAGER,
    ROLE_ADMIN;

    public static boolean isValidRole(String role) {
        for (UserRole userRole : UserRole.values()) {
            if (userRole.name().equals(role)) {
                return true;
            }
        }
        return false;
    }
}
