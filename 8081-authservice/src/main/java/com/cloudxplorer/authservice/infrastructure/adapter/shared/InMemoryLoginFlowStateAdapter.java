package com.cloudxplorer.authservice.infrastructure.adapter.shared;

import com.cloudxplorer.authservice.domain.model.LoginFlowState;
import com.cloudxplorer.authservice.domain.port.LoginFlowStatePort;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryLoginFlowStateAdapter implements LoginFlowStatePort {

    private final Map<String, LoginFlowState> store = new ConcurrentHashMap<>();

    @Override
    public void save(LoginFlowState state) {
        store.put(state.state(), state);
    }

    @Override
    public LoginFlowState consume(String state) {
        return store.remove(state);
    }
}
