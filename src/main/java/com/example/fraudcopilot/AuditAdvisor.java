package com.example.fraudcopilot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.stereotype.Component;

@Component
public class AuditAdvisor implements CallAdvisor {

    private static final Logger log = LoggerFactory.getLogger(AuditAdvisor.class);

    @Override
    public String getName() {
        return "AuditAdvisor";
    }

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request,
                                         CallAdvisorChain chain) {

        long start = System.currentTimeMillis();

        ChatClientResponse response = chain.nextCall(request);

        long ms = System.currentTimeMillis() - start;
        Object tokens = response.chatResponse().getMetadata().getUsage() != null
                ? response.chatResponse().getMetadata().getUsage().getTotalTokens()
                : "n/a";
        log.info("AUDIT | durationMs={} | totalTokens={}", ms, tokens);

        return response;
    }
}