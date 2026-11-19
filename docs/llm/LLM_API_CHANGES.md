# Changes needed in the LLM project (llm-exam-project)

Smart City sends `requestType: "HOUSING_CAPACITY"` to `POST /choose-your-fate/ai/ask`.
The LLM project does not know that type yet, so this needs a small change on its side.
The data description the model should see is `HOUSING_DATA_CONTEXT.md` in this folder.

## 1. New request type

`enums/AiRequestType.java`:

```java
@Schema(description = "Estimates the maximum number of residents of one housing record from its OSM data")
HOUSING_CAPACITY
```

Also add it to `allowableValues` in `AiRequestDTO`.

## 2. System prompt with the 4T's (`AiService.resolveSystemPrompt`)

```java
case HOUSING_CAPACITY -> """
    Traits: You are a careful urban data analyst. You only use the data given in the context and the
    background material, and you say so when data is missing.
    Task: Estimate the maximum number of residents (capacity, a whole number of at least 1) of the single
    housing record in the context. Missing values are written as "unknown"; never treat them as zero.
    Tone: Neutral, precise and brief.
    Target: A backend service that parses your answer as JSON. Answer with one JSON object and nothing else:
    {"capacity": <integer>, "confidence": "low|medium|high", "reasoning": "<one or two sentences>"}
    """;
```

`buildUserMessage`:

```java
case HOUSING_CAPACITY -> "Estimate the housing capacity for this record:\n" + request.getContext();
```

## 3. RAG

The current API has no retrieval: it only sends `context` to the model. The exam requires RAG,
so retrieval has to be added in the LLM project (or the API has to call a RAG service). Suggested minimum:

- Chunk and embed `HOUSING_DATA_CONTEXT.md` (the Python starter in the exam repo uses `nomic-embed-text`).
- Retrieve the top chunks for the incoming `context` and add them to the prompt before calling the model.
- Keep a way to turn retrieval off, so the exam can show how retrieval changes the answer.

## 4. Configuration to check

- The default model in `application.properties` and `.env` is `glm-4.6:cloud`, which is a cloud model.
  The exam requires a local LLM, so set `AI_OLLAMA_CHAT_MODEL` to a local model (the exam repo uses `qwen3:4b`).
- Port: the README and docker-compose say 8090, but `application.properties` sets `server.port=8080`.
  `HttpHousingCapacityClient.DEFAULT_ENDPOINT_URL` assumes 8090. Change one of them so they match.
- `app.ai.strip-markdown=true` only removes `**`, `*` and `#`. It does not affect the JSON answer.
