package com.example.fraudcopilot.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FraudChatController {
	
	private ChatClient chatClient;     // sends prompts to the LLM and give you back to the response
	
	public FraudChatController(ChatClient.Builder builder)
	{
		this.chatClient=builder
				.defaultSystem("""
						 You are Fraud Copilot, an assistant for credit card
                        fraud analysts. Help them reason about suspicious
                        transactions and fraud patterns. Be concise and
                        factual. You only recommend actions; a human analyst
                        makes the final decision. If you lack data, say so
                        instead of guessing.
						""")
				.build();
	}
	
	@GetMapping("/chat")
	public String chat(@RequestParam String message) {
		return chatClient.prompt()
				.user(message)
				.call()
				.content();
	}

}
