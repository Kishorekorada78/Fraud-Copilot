package com.example.fraudcopilot.model;

import java.time.LocalDateTime;

public record Transaction(
		String id,
        String customerId,
        double amount,
        String merchant,
        String city,
        LocalDateTime time
		) {

}
