package com.cloudxplorer.authservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final AuthProperties authProperties;

    public SecurityConfig(AuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/register", "/auth/login", "/auth/health", "/auth/health/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(this::extractAuthorities);
        return converter;
    }

    private Set<GrantedAuthority> extractAuthorities(Jwt jwt) {
        Set<GrantedAuthority> authorities = new HashSet<>();
        for (String role : extractRoles(jwt)) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase(Locale.ROOT)));
        }
        return authorities;
    }

    private Set<String> extractRoles(Jwt jwt) {
        Set<String> roles = new HashSet<>();
        if (jwt == null) {
            return roles;
        }
        Object realmAccessObj = jwt.getClaim("realm_access");
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

        Object resourceAccessObj = jwt.getClaim("resource_access");
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
