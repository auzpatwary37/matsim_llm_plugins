# MATSim LLM Plugin

This plugin integrates **MATSim (Multi-Agent Transport Simulation)** with **Large Language Models (LLMs)** using an OpenAI-compatible tool-calling interface. It enables agents to perform structured, context-aware decision-making during simulation.

---

## Overview

### 🧠 Purpose

This framework connects MATSim agents to LLMs for **stateful, tool-driven reasoning** within simulations. It supports:

* **Agent-specific conversational threads**
  Each MATSim agent maintains its own `ChatManager`, enabling persistent memory and multi-turn reasoning.

* **Multi-step and parallel tool execution**
  The LLM can invoke multiple tools in a single response. The system executes them iteratively until resolution.

* **Retrieval-Augmented Generation (RAG)**
  Context is dynamically injected using vector search:

  * **Static context**: network data, pricing, infrastructure
  * **Dynamic context**: agent experiences, runtime state

* **Typed tool calling with DTOs**

  * Java DTOs define schema visible to the LLM
  * Automatic parsing → validation → conversion into MATSim objects
  * Supports both structured outputs and internal reconstruction

* **Separation of tool types**

  * **LLM tools** → return outputs to LLM
  * **Dummy tools** → consumed directly by MATSim

* **Backend-agnostic architecture**

  * OpenAI
  * LM Studio
  * Ollama (OpenAI-compatible mode)

* **Dataset generation support (planned)**

  * JSONL logging for fine-tuning / distillation workflows

---

## Key Features

* OpenAI-style **multi-step tool calling in Java**
* Centralized **ToolManager** with automatic execution loop
* Per-agent **chat lifecycle management**
* Modular backend support (**OpenAI / LM Studio / Ollama**)
* DTO-based **automatic schema generation**
* Integrated **RAG with vector DB**
* Full **MATSim integration via Guice injection**

---

## ⚙️ Architecture

<img width="872" alt="architecture" src="https://github.com/user-attachments/assets/c751970a-6970-4a9b-8fc9-08447dee2952" />

---

## 🧠 ChatManager

The `ChatManager` is the core coordinator for each agent:

* Maintains full conversation history (`List<IChatMessage>`)
* Sends requests via `IChatCompletionClient`
* Handles multi-round tool execution
* Injects tool responses into conversation
* Produces outputs consumable by MATSim

---

## 🛠 Tool Framework

### Design

* Tools implement `ITool<T>`
* Arguments defined via:

  * `ToolArgument<T>`
  * `ToolArgumentDTO<T>` (GSON-serializable)
* DTOs:

  * Define schema for LLM
  * Parse responses into domain objects
  * Validate inputs

### Tool Types

| Type       | Behavior                  |
| ---------- | ------------------------- |
| Real Tool  | Output returned to LLM    |
| Dummy Tool | Output consumed by MATSim |

---

## 💬 Chat Clients

Implements `IChatCompletionClient`.

### Supported Backends

| Backend   | Endpoint                |
| --------- | ----------------------- |
| OpenAI    | HTTPS API               |
| LM Studio | OpenAI-compatible local |
| Ollama    | OpenAI-compatible local |

⚠️ **Important**
Ollama is supported via:

```
http://localhost:11434/v1/chat/completions
```

No separate native parser is required — it uses OpenAI-compatible request/response.

---

## 📚 Retrieval (RAG)

### Implementation

* Vector DB: **Qdrant (via LangChain4j)**
* Embeddings: OpenAI / HuggingFace (configurable)

### Features

* Static + dynamic document insertion
* Metadata filtering
* Configurable cleanup:

  * none
  * dynamic only
  * full reset

---

## 🧩 MATSim Integration

Integrated via:

```
LLMIntegrationModule
```

Provides global access to:

* `ChatManagerContainer`
* `IToolManager`
* `IVectorDB`
* `IChatCompletionClient`

Supports three modes:

| Mode                | Description                  |
| ------------------- | ---------------------------- |
| Replanning          | Strategy-based LLM planning  |
| Within-day          | Real-time decision updates   |
| Controller listener | Global lifecycle integration |

---

## 🚀 Setup Guide

### 1. LLM Server

#### OpenAI

* Use standard API
* Requires API key

#### LM Studio

1. Start server
2. Enable OpenAI-compatible API
3. Default:

```
http://localhost:1234/v1/chat/completions
```

#### Ollama

```bash
ollama run llama3
```

Endpoint:

```
http://localhost:11434/v1/chat/completions
```

---

### 2. Vector DB (Qdrant)

```bash
docker run -p 6333:6333 -p 6334:6334 qdrant/qdrant
```

* REST: `6333`
* gRPC: `6334` (used internally)

---

## 🛠 Tool Example

```java
class EchoTool implements ITool<String> {

    public EchoTool() {
        registerArgument(SimpleStringDTO.forArgument("message"));
        registerArgument(SimpleBooleanDTO.forArgument("shout"));
    }

    @Override
    public IToolResponse<String> callTool(String id, Map<String, Object> arguments, IVectorDB vectorDB) {

        String msg = (String) arguments.getOrDefault("message", "");
        boolean shout = Boolean.TRUE.equals(arguments.getOrDefault("shout", false));

        String result = shout ? msg.toUpperCase() : msg;

        return new IToolResponse<>() {
            @Override
            public String getToolCallOutputContainer() {
                return result;
            }

            @Override
            public boolean isForLLM() {
                return false; // dummy tool
            }
        };
    }
}
```

---

## 🧠 Agent Usage

```java
ChatManager manager = chatManagerContainer.getChatManagers().get(agentId);

IRequestMessage req =
    new SimpleRequestMessage(Role.USER, "message or context");

manager.submit(req);
```

---

## ⚙️ Configuration (LLMConfigGroup)

```xml
<module name="llmConfig">
    <param name="backendType" value="OLLAMA"/>
    <param name="llmHost" value="localhost"/>
    <param name="llmPort" value="11434"/>
    <param name="llmPath" value="/v1/chat/completions"/>
    <param name="embeddingModelName" value="text-embedding-3-small"/>
    <param name="cleanVectorDbUponCompletion" value="dynamic_only"/>
</module>
```

---

## 📁 Project Structure

| Package         | Description           |
| --------------- | --------------------- |
| `chatcommons`   | Core interfaces       |
| `chatrequest`   | Request builders      |
| `chatresponse`  | Response parsers      |
| `tools`         | Tool framework        |
| `rag`           | Vector DB / retrieval |
| `matsimBinding` | MATSim integration    |

---

## 📦 Dependencies

* Java 21 
* MATSim 2025.0 
* Gson
* Guice
* LangChain4j
* OkHttp
* Qdrant

---

## ⚠️ Notes

* Ollama uses OpenAI-compatible API — no separate implementation needed
* Authorization header should be omitted for local backends
* Some OpenAI fields may not be supported by all local models

---

## 📜 License

MIT License
