package com.perch.security;

import lombok.Getter;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

/**
 * Authentication details carrying userId plus standard web details.
 */
@Getter
public class UserIdAuthenticationDetails {
    private final Long userId;
    private final WebAuthenticationDetails webDetails;

    public UserIdAuthenticationDetails(Long userId, WebAuthenticationDetails webDetails) {
        this.userId = userId;
        this.webDetails = webDetails;
    }

}
