package com.example.fraudcopilot.controller;

import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PlaybookSearchController {
	
	private final VectorStore vectorStore;
	
	public PlaybookSearchController(VectorStore vectorStore) {
		this.vectorStore=vectorStore;
	}
	
	@GetMapping("/search")
	public List<Map<String,Object>> search(@RequestParam String q){
		// Retrieve
		List<Document> results=vectorStore.similaritySearch(
				SearchRequest.builder().query(q).topK(3).build()
				);
		return results.stream()
				.map( d-> Map.of(
						"section",d.getMetadata().get("section"),
						"text",d.getText()
						)
				)
				.toList();
	}

}
