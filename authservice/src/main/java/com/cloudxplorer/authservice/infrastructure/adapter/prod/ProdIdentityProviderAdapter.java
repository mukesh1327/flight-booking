package com.cloudxplorer.authservice.infrastructure.adapter.prod;

import com.cloudxplorer.authservice.domain.model.IdentityAuthResult;
import com.cloudxplorer.authservice.domain.model.IdentityTokens;
import com.cloudxplorer.authservice.domain.model.IdentityUser;
import com.cloudxplorer.authservice.domain.port.IdentityProviderPort;
import com.cloudxplorer.authservice.infrastructure.config.AuthServiceProperties;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
@Profile("prod")
public class ProdIdentityProviderAdapter implements IdentityProviderPort {

    private final RestClient restClient;
    private final AuthServiceProperties properties;

    public ProdIdentityProviderAdapter(RestClient restClient, AuthServiceProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    @PostConstruct
    void validateConfig() {
        if (properties.prodStrictConfig()) {
            require(properties.keycloak().baseUrl(), "KEYCLOAK_BASE_URL");
            require(properties.publicClient().realm(), "KEYCLOAK_PUBLIC_REALM");
            require(properties.publicClient().clientId(), "KEYCLOAK_CLIENT_ID_PUBLIC");
            require(properties.publicClient().redirectUri(), "PUBLIC_REDIRECT_URI");
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public IdentityAuthResult exchangePublicGoogleCode(String code, String codeVerifier) {
        Map<String, Object> tokenBody = postToken(Map.of(
            "grant_type", "authorization_code",
            "client_id", properties.publicClient().clientId(),
            "client_secret", nullSafe(properties.publicClient().clientSecret()),
            "redirect_uri", properties.publicClient().redirectUri(),
            "code", code,
            "code_verifier", codeVerifier
        ));

        String accessToken = (String) tokenBody.getOrDefault("access_token", "");
        String refreshToken = (String) tokenBody.getOrDefault("refresh_token", "");
        long expiresIn = asLong(tokenBody.getOrDefault("expires_in", 900));

        Map<String, Object> userInfo = restClient.get()
            .uri(tokenBaseUrl().replace("/token", "/userinfo"))
            .headers(headers -> headers.setBearerAuth(accessToken))
            .retrieve()
            .body(Map.class);

        String email = userInfo == null ? null : Objects.toString(userInfo.get("email"), "");
        String firstName = userInfo == null ? "" : Objects.toString(userInfo.get("given_name"), "");
        String lastName = userInfo == null ? "" : Objects.toString(userInfo.get("family_name"), "");
        String sub = userInfo == null ? "" : Objects.toString(userInfo.get("sub"), "");

        IdentityUser user = new IdentityUser(sub, email, firstName, lastName, "PUBLIC", List.of("CUSTOMER"));
        return new IdentityAuthResult(user, new IdentityTokens(accessToken, refreshToken, expiresIn));
    }

    @Override
    public IdentityTokens refresh(String refreshToken) {
        Map<String, Object> body = postToken(Map.of(
            "grant_type", "refresh_token",
            "client_id", properties.publicClient().clientId(),
            "client_secret", nullSafe(properties.publicClient().clientSecret()),
            "refresh_token", refreshToken
        ));
        return new IdentityTokens(
            Objects.toString(body.get("access_token"), ""),
            Objects.toString(body.get("refresh_token"), ""),
            asLong(body.getOrDefault("expires_in", 900))
        );
    }

    @Override
    public void logout(String refreshToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", properties.publicClient().clientId());
        if (properties.publicClient().clientSecret() != null && !properties.publicClient().clientSecret().isBlank()) {
            form.add("client_secret", properties.publicClient().clientSecret());
        }
        form.add("refresh_token", refreshToken);

        restClient.post()
            .uri(tokenBaseUrl().replace("/token", "/logout"))
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(form)
            .retrieve()
            .toBodilessEntity();
    }

    private Map<String, Object> postToken(Map<String, String> params) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        params.forEach((key, value) -> {
            if (value != null && !value.isBlank()) {
                form.add(key, value);
            }
        });

        return restClient.post()
            .uri(tokenBaseUrl())
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(form)
            .retrieve()
            .body(Map.class);
    }

    private String tokenBaseUrl() {
        return properties.keycloak().baseUrl() + "/realms/" + properties.publicClient().realm() + "/protocol/openid-connect/token";
    }

    private static void require(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required config: " + name);
        }
    }

    private static long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(Objects.toString(value, "900"));
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
