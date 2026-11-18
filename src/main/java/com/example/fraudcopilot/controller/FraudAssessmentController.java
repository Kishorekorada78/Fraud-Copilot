package com.example.fraudcopilot.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.fraudcopilot.model.RiskAssessment;
import com.example.fraudcopilot.model.TransactionRequest;

@RestController
public class FraudAssessmentController {
	
	private final ChatClient chatClient;
	
	public FraudAssessmentController(ChatClient.Builder builder)
	{
		this.chatClient=builder
				.defaultSystem("""
						 You are a credit card fraud analyst assistant.
                        Assess only the transaction data you are given.
                        Do not invent any additional data.
                        You only recommend; a human analyst decides.
						""")
				.build();
	}
	 @PostMapping("/assess")
	    public RiskAssessment assess(@RequestBody TransactionRequest tx) {
	        return chatClient.prompt()
	                .user(u -> u.text("""
	                        Assess the fraud risk of this transaction.
	                        Customer: {customerId}
	                        Amount: {amount}
	                        Merchant: {merchant}
	                        Current location: {currentLocation}
	                        Previous location: {previousLocation}
	                        Minutes since previous transaction: {minutes}

	                        riskScore is 0 to 100.
	                        riskLevel is one of LOW, MEDIUM, HIGH.
	                        """)
	                        .param("customerId", tx.customerId())
	                        .param("amount", tx.amount())
	                        .param("merchant", tx.merchant())
	                        .param("currentLocation", tx.currentLocation())
	                        .param("previousLocation", tx.previousLocation())
	                        .param("minutes", tx.minutesSinceLastTransaction()))
	                .call()
	                .entity(RiskAssessment.class);
	    }

}
