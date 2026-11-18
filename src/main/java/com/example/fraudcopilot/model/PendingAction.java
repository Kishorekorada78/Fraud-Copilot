package com.example.fraudcopilot.model;

public record PendingAction(String id, String type, String customerId,
        String reason, String status) {

}
