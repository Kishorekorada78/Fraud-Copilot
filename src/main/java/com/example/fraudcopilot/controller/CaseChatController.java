package com.example.fraudcopilot.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CaseChatController {

    private final ChatClient chatClient;

    public CaseChatController(ChatClient.Builder builder, ChatMemory chatMemory) {
        this.chatClient = builder
                .defaultSystem("""
                        You are Fraud Copilot helping an analyst investigate
                        one fraud case. Use only facts the analyst gives you.
                        If data is missing, say so. You only recommend;
                        a human decides.
                        """)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    @GetMapping("/case/{caseId}/chat")
    public String chat(@PathVariable String caseId,
                       @RequestParam String message) {
        return chatClient.prompt()
                .user(message)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, caseId))
                .call()
                .content();
    }
}
