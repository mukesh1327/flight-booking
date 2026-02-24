package com.cloudxplorer.authservice.infrastructure.adapter.shared;

import com.cloudxplorer.authservice.domain.model.IdentityTokens;
import com.cloudxplorer.authservice.domain.model.UserSession;
import com.cloudxplorer.authservice.domain.port.SessionPort;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemorySessionAdapter implements SessionPort {

    private final Map<String, Map<String, UserSession>> sessionsByUser = new ConcurrentHashMap<>();

    @Override
    public UserSession create(String userId, String device, String ip, String riskLevel, IdentityTokens tokens) {
        UserSession session = new UserSession(
            "sess_" + UUID.randomUUID(),
            userId,
            device == null ? "web" : device,
            ip == null ? "unknown" : ip,
            Instant.now(),
            Instant.now(),
            riskLevel
        );
        sessionsByUser.computeIfAbsent(userId, ignored -> new ConcurrentHashMap<>()).put(session.sessionId(), session);
        return session;
    }

    @Override
    public List<UserSession> getByUserId(String userId) {
        return sessionsByUser.getOrDefault(userId, Map.of()).values().stream().toList();
    }

    @Override
    public void revoke(String userId, String sessionId) {
        Map<String, UserSession> userSessions = sessionsByUser.get(userId);
        if (userSessions != null) {
            userSessions.remove(sessionId);
        }
    }

    @Override
    public void revokeAll(String userId) {
        sessionsByUser.remove(userId);
    }
}
