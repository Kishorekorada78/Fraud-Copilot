package com.example.fraudcopilot.controller;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.fraudcopilot.AuditAdvisor;
import com.example.fraudcopilot.PiiMasker;
import com.example.fraudcopilot.Tools.FraudActionTools;
import com.example.fraudcopilot.Tools.FraudTools;
import com.example.fraudcopilot.model.PendingAction;

import org.springframework.http.MediaType;
import reactor.core.publisher.Flux;

@CrossOrigin(origins = {"http://127.0.0.1:5500", "http://localhost:5500"})
@RestController
public class AgentController {

    private final ChatClient chatClient;
    private final FraudActionTools actionTools;

    public AgentController(ChatClient.Builder builder,
                           ChatMemory chatMemory,
                           VectorStore vectorStore,
                           FraudTools fraudTools,
                           FraudActionTools actionTools,
                           AuditAdvisor auditAdvisor) {
        this.actionTools = actionTools;
        this.chatClient = builder
        		.defaultSystem("""
        		        You are Fraud Copilot, an assistant for credit card fraud analysts.

        		        Rules:
        		        - To investigate or assess a customer, call analyzeCustomerActivity FIRST.
        		          Base your conclusions only on its transactions, cityChanges and flags.
        		        - Never do your own time or distance arithmetic. Use the tool's numbers.
        		        - Use checkTravelFeasibility only for hypothetical questions about two cities.
        		        - If flags contain IMPOSSIBLE_TRAVEL or EXPOSURE_ABOVE_5000_ESCALATE,
        		          call proposeCardBlock with a short reason. This only creates a
        		          recommendation; a human approves it.
        		        - Use the playbook context for policy and recommended actions.
        		        - If a customer has NO_TRANSACTIONS_FOUND, say so. Never invent data.
        		        """)
                .defaultAdvisors(
                        auditAdvisor,   // auditAdvisor is our timing and token logger. Its order of 0 puts it outermost.
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        QuestionAnswerAdvisor.builder(vectorStore)
                                .searchRequest(SearchRequest.builder()
                                        .topK(3)
                                        .similarityThreshold(0.5)
                                        .build())
                                .build(),
                        new SimpleLoggerAdvisor())
                .defaultTools(fraudTools, actionTools)
                .build();
    }

    @GetMapping("/agent/{caseId}")
    public String agent(@PathVariable String caseId,
                        @RequestParam String message) {
        String safeMessage = PiiMasker.mask(message);
        return chatClient.prompt()
                .user(safeMessage)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, caseId))
                .call()
                .content();
    }

    @GetMapping("/actions")
    public List<PendingAction> actions() {
        return actionTools.all();
    }

    @PostMapping("/actions/{id}/approve")
    public PendingAction approve(@PathVariable String id) {
        return actionTools.approve(id);
    }
    
    public record Token(String t) { }

    @GetMapping(value = "/agent/{caseId}/stream",
                produces = MediaType.TEXT_EVENT_STREAM_VALUE)  // tells Spring to send each item as an SSE event, keeping the connection open. Without it, Spring MVC would collect the whole Flux into a list and send it at the end, so you’d get no streaming at all.
    public Flux<Token> agentStream(@PathVariable String caseId,
                                   @RequestParam String message) {
        String safeMessage = PiiMasker.mask(message);
        return chatClient.prompt()
                .user(safeMessage)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, caseId))
                .stream()		// it sends the request and returns immediately with a stream
                .content()
                .map(Token::new);		// wraps each text chunk in a small JSON object ({"t":" the"}) instead of sending raw text.
    }
}
