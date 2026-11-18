package com.example.fraudcopilot.model;

public record TravelCheck(
		 String fromCity,
	        String toCity,
	        double distanceKm,
	        double requiredSpeedKmh,
	        boolean physicallyPossible
		) {

}
