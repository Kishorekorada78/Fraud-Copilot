# Fraud Copilot

A learning project built with **Spring AI**: an assistant that helps a (fictional) fraud analyst investigate suspicious credit card activity.

The analyst asks about a customer in a chat UI. The agent fetches transactions through Java tools, checks for impossible travel, looks up the fraud playbook with RAG, remembers the conversation per case, and can **propose** a card block. A human must approve it.

> **This is a learning project.** All customers, transactions and playbook text are synthetic. It is not connected to any real bank system and is not production-ready (see [Limitations](#limitations)).

---

## What it does

- **Chat with an LLM** running locally through Ollama (`llama3.1`).
- **Answers policy questions from a fraud playbook** using RAG (embeddings, a vector store and retrieval).
- **Calls Java tools** to fetch transactions and compute time gaps, distances and fraud flags, so facts come from code and not from the model.
- **Remembers each case** separately (conversation memory stored in PostgreSQL).
- **Streams replies** word by word to a web UI.
- **Proposes, never executes.** The model can only create a pending "block card" recommendation, and a human approves it in the UI.
- **Masks card numbers** in user input before they reach the model.
- **Runs with Docker Compose** (the app and a PostgreSQL + pgvector database).

---

## Architecture

```
                  Browser (Fraud-ui/index.html)
                           │  GET /agent/{caseId}/stream
                           ▼
                ┌─────────────────────────┐
                │   Spring Boot app       │
                │                         │
                │  PiiMasker              │  masks card numbers
                │  ChatClient             │
                │   ├─ AuditAdvisor       │  timing + token logging
                │   ├─ Memory advisor     │  past messages per case
                │   ├─ RAG advisor        │  playbook chunks
                │   └─ Tools              │  analyze, travel check, propose block
                └───────┬─────────┬───────┘
                        │         │
              Ollama (host)     PostgreSQL + pgvector
              llama3.1          ├─ vector_store (playbook embeddings)
              nomic-embed-text  └─ spring_ai_chat_memory (chat history)
```

**Request flow for "Investigate customer C1001":**

1. The UI sends the message and a case ID.
2. The card-number masker cleans the text.
3. Advisors add the case's chat history and the most relevant playbook sections to the prompt.
4. The model decides to call `analyzeCustomerActivity`. Spring AI runs that Java method and returns the result.
5. The model explains the findings. If flags such as impossible travel are present, it calls `proposeCardBlock`.
6. The reply streams back to the UI. A pending action appears in the side panel for a human to approve.

---

## Tech stack

| Area | Technology |
|---|---|
| Language / framework | Java 21, Spring Boot 4.1, Spring AI 2.0 |
| LLM | Ollama with `llama3.1` |
| Embeddings | Ollama with `nomic-embed-text` (768 dimensions) |
| Vector store | PostgreSQL with pgvector (HNSW index, cosine distance) |
| Chat memory | Spring AI JDBC chat memory on PostgreSQL |
| Frontend | Plain HTML, CSS and JavaScript (`Fraud-ui/index.html`) |
| Packaging | Docker (multi-stage build) and Docker Compose |

---

## Spring AI concepts covered

| # | Concept | Where to look |
|---|---|---|
| 1 | `ChatClient`, system prompts | The chat controllers |
| 2 | Prompt templates and structured output (`.entity()`) | `/assess` returns a `RiskAssessment` record |
| 3 | Chat memory and conversation IDs | `MessageChatMemoryAdvisor`, now stored in Postgres |
| 4 | Embeddings and similarity | `/embed` and `/similarity` |
| 5 | Vector store and document ingestion | `PlaybookIngestionRunner`, pgvector |
| 6 | RAG (retrieval, augmentation, generation) | `/ask` (manual) and `QuestionAnswerAdvisor` |
| 7 | Tool calling (`@Tool`, `@ToolParam`) | `FraudTools`, `FraudActionTools` |
| 8 | Advisors, audit logging, guardrails, human approval | `AuditAdvisor`, `PiiMasker`, `/actions` |
| 9 | Frontend and deployment | `Fraud-ui`, `Dockerfile` |
| 10 | Streaming with SSE | `/agent/{caseId}/stream` |
| 11 | Persistence with pgvector and Docker Compose | `docker-compose.yml` |

---

## Project structure

```
Fraud Copilot/
├── src/main/java/com/example/fraudcopilot/
│   ├── AgentController.java          # main agent: memory + RAG + tools + streaming + approvals
│   ├── FraudTools.java               # read-only tools (transactions, travel, analysis)
│   ├── FraudActionTools.java         # propose-block tool + human approval logic
│   ├── AuditAdvisor.java             # timing and token logging
│   ├── PiiMasker.java                # card number masking
│   ├── WebConfig.java                # CORS for the frontend
│   ├── Config/PlaybookIngestionRunner.java   # loads the playbook into the vector store
│   └── model/                        # records: Transaction, TravelCheck, CustomerAnalysis, ...
├── src/main/resources/
│   ├── application.properties
│   └── playbook/fraud-playbook.txt   # fictional fraud playbook (sections start with "## ")
├── Fraud-ui/index.html               # chat UI
├── Dockerfile
├── docker-compose.yml
└── pom.xml
```

Earlier learning endpoints (`/chat`, `/assess`, `/embed`, `/similarity`, `/search`, `/ask`, `/investigate`) are kept so each concept can be tried on its own.

---

## Run it

### Prerequisites

- JDK 21 and Maven (or STS / IntelliJ), or just Docker
- Docker Desktop
- [Ollama](https://ollama.com) running locally

### 1. Pull the models

```
ollama pull llama3.1
ollama pull nomic-embed-text
```

### 2. Start the database

```
docker compose up -d db
```

The database is published on host port **5433**, to avoid clashing with a local PostgreSQL on 5432.

### 3. Start the app

**Option A: from your IDE.** Run the main class. It connects to `localhost:5433` by default.

**Option B: everything in containers.** Stop the IDE run first so port 8080 is free, then:

```
docker compose up --build
```

On startup the log should show the playbook chunks and `Ingested 6 playbook chunks`.

### 4. Open the UI

Open `Fraud-ui/index.html` with the VS Code **Live Server** extension. It runs on `http://127.0.0.1:5500` and calls the backend at `http://localhost:8080`.

Try:

- `Investigate customer C1001`
- `What does the playbook say about card testing?`
- `Is travel from Hyderabad to Dubai in 120 minutes possible?`

### Configuration

| Variable | Default | Purpose |
|---|---|---|
| `OLLAMA_BASE_URL` | `http://localhost:11434` | Where Ollama is running |
| `DB_URL` | `jdbc:postgresql://localhost:5433/fraud` | Database connection |
| `DB_USER` / `DB_PASSWORD` | `fraud` / `fraud` | Local demo credentials only |

---

## Key design decisions

1. **Facts come from code, explanations come from the LLM.** Time gaps, distances and flags are computed in Java (`analyzeCustomerActivity`). Small local models are unreliable at arithmetic and at chaining several tool calls, so one larger tool replaces a chain of small ones.
2. **The model proposes, a human executes.** `proposeCardBlock` only creates a pending record. The approval method is not annotated as a tool, so the model cannot see or call it.
3. **Sections are chunks.** The playbook is split on `## ` headings so each chunk is one complete idea, and chunk IDs are deterministic so restarting never creates duplicates.
4. **Interfaces make storage swappable.** Moving from in-memory storage to PostgreSQL changed dependencies and configuration, not the controller code.
5. **Configuration lives outside the code.** The same Docker image runs anywhere by changing environment variables.

---

## Things that went wrong (and what they taught me)

| Problem | Lesson |
|---|---|
| The model invented a customer transaction when asked without data | LLMs predict plausible text. Real data must be supplied by tools or retrieval. |
| The model misread "120 minutes" as 2 | Structured output guarantees shape, not truth. Compute facts in code. |
| Playbook produced 18 chunks instead of 6 | Splitting on every blank line cut sections into fragments. Chunk quality decides RAG quality. |
| Agent skipped the travel check tool | Small models struggle with multi-step tool chains. Fewer, bigger tools are more reliable. |
| `Failed to fetch` in the browser | CORS. The backend answered with 200, but the browser blocked the response without an allow-origin header. |
| Custom advisor not accepted by `defaultAdvisors(...)` | The class had the right methods but did not declare `implements CallAdvisor`. |
| `QuestionAnswerAdvisor` could not be resolved | The module was renamed in Spring AI 2.0. Check release notes, not old tutorials. |
| App could not connect to the database | Two programs were listening on port 5432. Port mappings have a host side and a container side. |

---

## Limitations

This is a learning project, not a production system.

- **Pending actions are stored in memory** and are lost on restart.
- **No authentication.** Anyone who can reach the API can call the approval endpoint.
- **Transaction data is fake** and held in a Java list.
- **PII masking covers user input only**, not tool output or logs.
- **`AuditAdvisor` does not run on streamed requests** (it only implements the blocking advisor interface).
- **The playbook is re-embedded on every startup.**
- **Chat history has no retention policy** and grows indefinitely.
- **The local 8B model can skip tools or paraphrase flags loosely.** A larger model would be more reliable, and the agent has no automated evaluation yet.
- **Credentials in `docker-compose.yml` are demo values.** Real deployments need a secrets manager.

---

## Ideas for next steps

- Persist pending actions in a database table.
- Add Spring Security and record who approved each action.
- Add a streaming audit advisor.
- Write an evaluation set to measure tool-use accuracy and groundedness.
- Expose the tools over MCP.
- Replace the fake data with a real transaction service.

---

## Disclaimer

All people, accounts, merchants, transactions and fraud rules in this repository are fictional and for educational purposes. The agent only makes recommendations; a human analyst makes every decision.
