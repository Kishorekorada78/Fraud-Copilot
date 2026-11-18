package com.example.fraudcopilot.model;

import java.util.List;

public record RiskAssessment(
		 int riskScore,
	        String riskLevel,
	        List<String> reasons,
	        String recommendedAction
		) {

}
