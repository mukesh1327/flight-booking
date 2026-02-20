package com.cloudxplorer.authservice.util;

import com.cloudxplorer.authservice.config.AuthProperties;
import com.cloudxplorer.authservice.model.PrincipalInfo;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class TokenUtils {
    private static final JsonParser JSON_PARSER = JsonParserFactory.getJsonParser();
    private final AuthProperties authProperties;

    public TokenUtils(AuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    public PrincipalInfo resolvePrincipal(Jwt jwt) {
        if (jwt == null) {
            return null;
        }
        return new PrincipalInfo(jwt.getSubject(), extractEmailFromJwt(jwt), extractRolesFromJwt(jwt));
    }

    public Set<String> extractRolesFromAccessToken(String token) {
        Set<String> roles = new HashSet<>();
        if (ValidationUtils.isBlank(token)) {
            return roles;
        }
        String[] tokenParts = token.split("\\.");
        if (tokenParts.length < 2) {
            return roles;
        }
        try {
            String payload = new String(Base64.getUrlDecoder().decode(tokenParts[1]), StandardCharsets.UTF_8);
            Map<String, Object> claims = JSON_PARSER.parseMap(payload);
            return extractRolesFromClaims(claims);
        } catch (Exception ex) {
            return roles;
        }
    }

    public String extractClaimFromAccessToken(String token, String field) {
        if (ValidationUtils.isBlank(token) || ValidationUtils.isBlank(field)) {
            return null;
        }
        String[] tokenParts = token.split("\\.");
        if (tokenParts.length < 2) {
            return null;
        }
        try {
            String payload = new String(Base64.getUrlDecoder().decode(tokenParts[1]), StandardCharsets.UTF_8);
            Map<String, Object> claims = JSON_PARSER.parseMap(payload);
            Object value = claims.get(field);
            return value == null ? null : value.toString();
        } catch (Exception ex) {
            return null;
        }
    }

    public Set<String> extractRolesFromJwt(Jwt jwt) {
        if (jwt == null) {
            return Set.of();
        }
        return extractRolesFromClaims(jwt.getClaims());
    }

    public String extractEmailFromJwt(Jwt jwt) {
        if (jwt == null) {
            return null;
        }
        String email = jwt.getClaimAsString("email");
        if (!ValidationUtils.isBlank(email)) {
            return email;
        }
        String preferredUsername = jwt.getClaimAsString("preferred_username");
        if (!ValidationUtils.isBlank(preferredUsername) && preferredUsername.contains("@")) {
            return preferredUsername;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Set<String> extractRolesFromClaims(Map<String, Object> claims) {
        Set<String> roles = new HashSet<>();
        if (claims == null) {
            return roles;
        }
        Object realmAccessObj = claims.get("realm_access");
        if (realmAccessObj instanceof Map<?, ?> realmAccess) {
            Object rolesObj = realmAccess.get("roles");
            if (rolesObj instanceof List<?> roleList) {
                for (Object role : roleList) {
                    if (role != null) {
                        roles.add(role.toString());
                    }
                }
            }
        }

        Object resourceAccessObj = claims.get("resource_access");
        if (resourceAccessObj instanceof Map<?, ?> resourceAccess) {
            Object clientAccessObj = resourceAccess.get(authProperties.getRhbk().getClientId());
            if (clientAccessObj instanceof Map<?, ?> clientAccess) {
                Object rolesObj = clientAccess.get("roles");
                if (rolesObj instanceof List<?> roleList) {
                    for (Object role : roleList) {
                        if (role != null) {
                            roles.add(role.toString());
                        }
                    }
                }
            }
        }
        return roles;
    }
}
