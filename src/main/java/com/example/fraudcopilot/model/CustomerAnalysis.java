package com.example.fraudcopilot.model;

import java.util.List;

public record CustomerAnalysis(String customerId,
        int transactionCount,
        double totalAmount,
        List<Transaction> transactions,
        List<TransactionLeg> cityChanges,
        List<String> flags) {

}
