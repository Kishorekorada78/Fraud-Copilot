package com.example.fraudcopilot.Tools;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.example.fraudcopilot.model.PendingAction;

@Component
public class FraudActionTools {

    private final Map<String, PendingAction> actions = new ConcurrentHashMap<>();

    @Tool(description = "Propose blocking a customer's card. This does NOT block "
            + "the card. It only creates a pending recommendation that a human "
            + "analyst must approve. Use when fraud risk is high.")
    public PendingAction proposeCardBlock(
            @ToolParam(description = "Customer id, for example C1001") String customerId,
            @ToolParam(description = "Short reason for the recommendation") String reason) {
        PendingAction action = new PendingAction(
                UUID.randomUUID().toString().substring(0, 8),
                "BLOCK_CARD", customerId, reason, "PENDING_APPROVAL");
        actions.put(action.id(), action);
        return action;
    }

    // NOT a @Tool: only humans (through the REST API) can call this
    public PendingAction approve(String id) {
        PendingAction current = actions.get(id);
        if (current == null) {
            throw new IllegalArgumentException("Unknown action " + id);
        }
        PendingAction approved = new PendingAction(current.id(), current.type(),
                current.customerId(), current.reason(), "APPROVED_AND_EXECUTED");
        actions.put(id, approved);
        // A real system would call the card-management API here
        return approved;
    }

    public List<PendingAction> all() {
        return List.copyOf(actions.values());
    }
}
