package com.example.fraudcopilot.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.fraudcopilot.Tools.FraudTools;

@RestController
public class InvestigationController {
	
	private final ChatClient chatClient;
	private final FraudTools fraudTools;
	
	public InvestigationController(ChatClient.Builder builder, FraudTools fraudTools) {
		this.fraudTools=fraudTools;
		this.chatClient=builder
				.defaultSystem("""
						You are Fraud Copilot, an assistant for fraud analysts.
                        NEVER state transaction facts from memory.
                        Always use the provided tools to fetch transactions
                        and to check travel feasibility.
                        Base your assessment only on tool results.
                        You only recommend; a human analyst decides.
						""")
				.build();
	}
	
	@GetMapping("/investigate")
	public String investigate(@RequestParam String query) {
		return chatClient.prompt()
				.user(query)
				.tools(fraudTools)
				.call()
				.content();
	}

}
