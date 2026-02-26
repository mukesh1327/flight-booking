package com.cloudxplorer.authservice.infrastructure.adapter.shared;

import com.cloudxplorer.authservice.domain.model.IdentityUser;
import com.cloudxplorer.authservice.domain.model.UserProfile;
import com.cloudxplorer.authservice.domain.port.UserProfilePort;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryUserProfileAdapter implements UserProfilePort {

    private final Map<String, UserProfile> byUserId = new ConcurrentHashMap<>();
    private final Map<String, String> byProviderKey = new ConcurrentHashMap<>();

    @Override
    public UserProfile createOrGetFromIdentity(IdentityUser user) {
        String providerKey = user.realm() + ":" + user.providerUserId();
        String existingId = byProviderKey.get(providerKey);
        if (existingId != null && byUserId.containsKey(existingId)) {
            return byUserId.get(existingId);
        }

        String userId = "usr_" + user.providerUserId().replace("-", "");
        UserProfile profile = new UserProfile(
            userId,
            user.email(),
            safe(user.firstName()),
            safe(user.lastName()),
            null,
            false,
            user.realm(),
            user.roles(),
            "INCOMPLETE",
            Instant.now()
        );
        byProviderKey.put(providerKey, userId);
        byUserId.put(userId, profile);
        return profile;
    }

    @Override
    public UserProfile getByUserId(String userId) {
        return byUserId.get(userId);
    }

    @Override
    public UserProfile update(String userId, String firstName, String lastName, String mobile) {
        UserProfile current = byUserId.get(userId);
        if (current == null) {
            throw new IllegalArgumentException("User not found");
        }
        String nextFirstName = firstName == null ? current.firstName() : firstName;
        String nextLastName = lastName == null ? current.lastName() : lastName;
        String nextMobile = mobile == null ? current.mobile() : mobile;
        String status = (nextFirstName == null || nextFirstName.isBlank() || nextLastName == null || nextLastName.isBlank())
            ? "INCOMPLETE"
            : "COMPLETE";
        UserProfile updated = new UserProfile(
            current.userId(),
            current.email(),
            nextFirstName,
            nextLastName,
            nextMobile,
            current.mobileVerified(),
            current.realm(),
            current.roles(),
            status,
            Instant.now()
        );
        byUserId.put(userId, updated);
        return updated;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
