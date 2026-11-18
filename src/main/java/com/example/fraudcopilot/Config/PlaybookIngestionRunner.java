package com.example.fraudcopilot.Config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

@Component
public class PlaybookIngestionRunner implements ApplicationRunner {

    private final VectorStore vectorStore;

    @Value("classpath:playbook/fraud-playbook.txt")
    private Resource playbook;

    public PlaybookIngestionRunner(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        // 1. Read the whole file into one String
        String text = StreamUtils.copyToString(
                playbook.getInputStream(), StandardCharsets.UTF_8);

        // 2. Split into sections at lines that start with "## "
        List<Document> chunks = new ArrayList<>();
        for (String section : text.split("(?m)^##\\s+")) {
            String trimmed = section.trim();
            if (trimmed.isEmpty()) continue;

            String title = trimmed.lines().findFirst().orElse("Untitled").trim();

            // 3. Same title always gives the same ID, so re-running replaces, not duplicates
            String id = UUID.nameUUIDFromBytes(
                    ("fraud-playbook:" + title).getBytes(StandardCharsets.UTF_8)).toString();

            chunks.add(new Document(id, trimmed,
                    Map.of("source", "fraud-playbook", "section", title)));

            System.out.println("CHUNK | " + title + " | " + trimmed.length() + " chars");
        }

        // 4. Embed and store
        vectorStore.add(chunks);
        System.out.println("Ingested " + chunks.size() + " playbook chunks");
    }
}