package com.perch.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Security helpers for current request context.
 */
public final class SecurityUtils {
    private SecurityUtils() {
    }

    public static Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return null;
        }
        Object details = auth.getDetails();
        if (details instanceof UserIdAuthenticationDetails) {
            return ((UserIdAuthenticationDetails) details).getUserId();
        }
        return null;
    }
}
