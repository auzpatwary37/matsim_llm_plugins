# MATSim LLM Plugin

This plugin connects MATSim (Multi-Agent Transport Simulation) to Large Language Models (LLMs) via OpenAI-compatible APIs. It allows MATSim agents to use LLMs for dynamic decision-making with structured tool calls and optional retrieval-augmented generation (RAG).

---

## Overview

### Purpose

This plugin enables:

* Tool-based interaction between MATSim agents and LLMs
* Execution of Java-defined tools (e.g., with objects as tool arguments)
* Optional use of vector-based retrieval for context injection
* Optionally collect jsonl datasets for distillation of smaller LLM models through finetuning


### Key Features

* OpenAI-style tool calling in Java
* Modular backend support (OpenAI, LM Studio, Ollama)
* Pluggable tools with automatic schema generation via DTOs
* Retrieval integration with ChromaDB and HuggingFace embeddings

---

## ⚙️ Architecture

### 🧠 ChatManager (Core Coordinator)

The `ChatManager` handles LLM conversations and decision logic:

* Maintains full message history as `List<IChatMessage>`
* Sends requests to the selected `IChatCompletionClient`
* Receives tool calls and dispatches them to the `ToolManager`
* Injects tool responses into message history for a second LLM round
* Optionally prepends RAG context from the retrieval module

### 🛠 ToolManager & Tool Framework

* Tools are Java classes implementing `ITool`
* Tool arguments are defined using `ToolArgument<T>` and `ToolArgumentDTO<T>`
* DTOs expose a schema for LLM and parse responses into domain objects
* Two tool types:

  * **Real Tools**: LLM uses the returned result
  * **Dummy Tools**: Results are consumed by MATSim (not shown to LLM)
* Tools register themselves via the `ToolRegistry`

### 💬 Chat Clients

Implements `IChatCompletionClient` to interact with different LLM providers:

* **OpenAI**: via HTTPS and bearer token
* **LM Studio / Ollama**: via local OpenAI-compatible endpoints
* Each backend has its own `*ChatRequest` and `*ChatResponse` implementation

### 📚 Retrieval (RAG)

Optional module to provide context to LLMs:

* Uses ChromaDB (via Akimos Java client)
* HuggingFace model for embedding (e.g., `all-MiniLM-L6-v2`)
* Supports static and dynamic document insertion
* Controlled cleanup of database using `cleanVectorDbUponCompletion`

### 🧩 MATSim Integration

* Integrated using Guice modules
* Configuration exposed via `LLMConfigGroup`
* Global access to `ChatManager`, `ToolManager`, and `Retrieval` via providers

---

## 🚀 Server Setup Guide

### 🔌 Local LLM Server (LM Studio / Ollama)

#### LM Studio

1. Download and launch LM Studio
2. Load a model (e.g., `llama3`) in chat mode
3. Enable the **OpenAI-compatible API** in settings
4. Default endpoint: `http://localhost:1234/v1/chat/completions`

#### Ollama

```bash
ollama run llama3
```

* Default endpoint: `http://localhost:11434/v1/chat/completions`
* No key required

### 📚 Vector DB (ChromaDB)

To enable context injection via RAG:

```bash
docker run -p 8000:8000 ghcr.io/chroma-core/chroma:latest
```

* Default endpoint: `http://localhost:8000`
* Used by Akimos Java client for embedding and querying
* Requires no manual configuration beyond port exposure

---

## 🛠 How to Define and Use Tools

### Tool Definition Example

```java
public class EchoTool implements ITool {
    public EchoTool() {
        addArgument("message", ToolArgument.of(new SimpleStringDTO()));
        addArgument("shout", ToolArgument.of(new SimpleBooleanDTO()));
    }

    public IToolResponse<String> run(Map<String, ToolArgumentDTO<?>> args) {
        String msg = ((SimpleStringDTO) args.get("message")).getValue();
        boolean shout = ((SimpleBooleanDTO) args.get("shout")).getValue();
        return new DefaultToolResponse<>(shout ? msg.toUpperCase() : msg);
    }
}
```

### Register Tools

```java
ToolRegistry.getInstance().registerTool(new EchoTool());
```

---

## 🧠 Agent Usage in MATSim

Agents call the `ChatManager` to query LLMs with current state and context:

```java
ChatManager manager = GlobalRegistry.getChatManager();
IChatMessage reply = manager.query(messages);
```

This handles LLM request, tool execution, and second-round reply injection.

---

## 🧾 Configuration (LLMConfigGroup)

```xml
<module name="llmConfig">
    <param name="backendType" value="openai"/>
    <param name="host" value="api.openai.com"/>
    <param name="authorization" value="Bearer YOUR_API_KEY"/>
    <param name="embeddingModelName" value="all-MiniLM-L6-v2"/>
    <param name="cleanVectorDbUponCompletion" value="dynamic_only"/>
</module>
```

---

## 📁 Project Structure

| Module          | Description                                    |
| --------------- | ---------------------------------------------- |
| `chatcommons`   | Core interfaces and roles (e.g., IChatMessage) |
| `chatrequest`   | LLM-specific request serializers               |
| `chatresponse`  | LLM-specific response parsers                  |
| `toolmanager`   | Tool execution and schema framework            |
| `retrieval`     | RAG and ChromaDB integration                   |
| `matsimBinding` | Configuration and Guice module bindings        |

---

## 📦 Dependencies

* Java 11+
* MATSim
* Gson
* Optional: Docker (for ChromaDB)
* Akimos ChromaDB with chromadb docker version 0.4.9 (the java api does not work with rest api v2 of the latest chromadb) 
* LM Studio or Ollama for local LLMs or openai.

---

## 📜 License

To be specified.

---

See `doc/index.html` for JavaDoc-based API documentation.
