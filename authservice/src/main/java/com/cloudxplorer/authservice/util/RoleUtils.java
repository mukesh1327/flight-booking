package com.cloudxplorer.authservice.util;

import java.util.HashSet;
import java.util.Set;

public final class RoleUtils {
    public static final Set<String> SUPPORTED_USER_ROLES = Set.of("customer", "admin", "support_agent", "airline_ops");

    private RoleUtils() {
    }

    public static Set<String> supportedRoles(Set<String> roles) {
        Set<String> result = new HashSet<>();
        if (roles == null) {
            return result;
        }
        for (String role : roles) {
            if (SUPPORTED_USER_ROLES.contains(role)) {
                result.add(role);
            }
        }
        return result;
    }

    public static boolean hasExactlyOneSupportedRole(Set<String> roles) {
        return supportedRoles(roles).size() == 1;
    }

    public static boolean hasExactlyRole(Set<String> roles, String requiredRole) {
        Set<String> supported = supportedRoles(roles);
        return supported.size() == 1 && supported.contains(requiredRole);
    }

    public static boolean hasExactlyOneOfRoles(Set<String> roles, Set<String> allowedRoles) {
        Set<String> supported = supportedRoles(roles);
        if (supported.size() != 1) {
            return false;
        }
        return allowedRoles.contains(supported.iterator().next());
    }
}
