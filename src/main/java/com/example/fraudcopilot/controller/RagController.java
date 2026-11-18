package com.example.fraudcopilot.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RagController {

	private final ChatClient chatClient;
	private final VectorStore vectorStore;
	
	public RagController(ChatClient.Builder builder, VectorStore vectorStore) {
		this.vectorStore=vectorStore;
		this.chatClient=builder
				.defaultSystem("""
						 You are Fraud Copilot, an assistant for fraud analysts.
                        Answer ONLY using the playbook context provided.
                        If the context does not contain the answer, reply:
                        "The playbook does not cover this."
                        Mention the section name you used.
                        You only recommend; a human analyst decides.
						""")
				.build();
	}
	
	@GetMapping("/ask")
	public String ask(@RequestParam String question) {
		
		// 1. Retrieve
		List<Document> docs=vectorStore.similaritySearch(
				SearchRequest.builder()
                .query(question)
                .topK(3)
                .similarityThreshold(0.5)      // it avoids unrelevant chunks
                .build());
		
		// 2. Augment : join the chunks into one context String
		String context=docs.stream()  	// Turns the list into a stream,
				.map(Document::getText)	// Here each Document becomes just its text, and the metadata is dropped.Document::getText is a method reference
				.collect(Collectors.joining("\n\n---\n\n"));   //glues all Strings into one String, putting the separator between items.
		
		/*The separator "\n\n---\n\n" means:
			\n\n: a blank line
			---: a visible divider
			\n\n: another blank line   */
		
		// Generation 
		return chatClient.prompt()
				.user(u -> u.text("""
						Playbook context:
                        {context}

                        Question: {question}
						""")
						.param("context", context)
						.param("question", question))
				.call()
				.content();
				
	}
}
