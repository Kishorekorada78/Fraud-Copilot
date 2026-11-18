package com.example.fraudcopilot.controller;

import java.util.Arrays;
import java.util.Map;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EmbeddingController {

	private final EmbeddingModel embeddingModel;
	
	public EmbeddingController(EmbeddingModel embeddingModel) {
		this.embeddingModel=embeddingModel;
	}
	
	// See What an embeddding looks like
	@GetMapping("/embed")
	public Map<String, Object> embed(@RequestParam String text){
		float[] vector=embeddingModel.embed(text);
		return Map.of(
				"Dimensions",vector.length,
				"first10Numbers", Arrays.copyOf(vector, 10)
				);
	}
	
	// Compare two texts by meaning
	@GetMapping("/similarity")
	 public Map<String, Object> similarity(@RequestParam String a,
             @RequestParam String b) {
		float[] va = embeddingModel.embed(a);
		float[] vb = embeddingModel.embed(b);
		return Map.of("cosineSimilarity", cosine(va, vb));
}
	private double cosine(float[] x, float[] y) {
		double dot=0,normX=0, normY=0;
		for(int i=0;i<x.length;i++) {
			dot+=x[i]*y[i];
			normX+=x[i]*x[i];
			normY+=y[i]*y[i];
		}
		return dot/ (Math.sqrt(normX)*Math.sqrt(normY));
	}
	
}
