package com.example.fraudcopilot.model;

public record TransactionRequest(
		 String customerId,
	        double amount,
	        String merchant,
	        String currentLocation,
	        String previousLocation,
	        int minutesSinceLastTransaction
		) {

}
