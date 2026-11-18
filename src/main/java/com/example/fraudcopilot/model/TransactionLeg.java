package com.example.fraudcopilot.model;

public record TransactionLeg(String fromTransactionId,
        String toTransactionId,
        long minutesBetween,
        TravelCheck travel) {

}
