# MATSim LLM Plugin

This plugin connects MATSim (Multi-Agent Transport Simulation) to Large Language Models (LLMs) via OpenAI-compatible APIs. It allows MATSim agents to use LLMs for dynamic decision-making with structured tool calls and optional retrieval-augmented generation (RAG).

---

## Overview

### 🧠 Purpose

This plugin integrates MATSim with Large Language Models (LLMs) via an OpenAI-compatible tool-calling interface, enabling rich, context-aware, object-driven decision-making within mobility simulations. It supports:

- **Agent-specific conversational threads** via `ChatManager`, allowing each MATSim agent (person or vehicle) to maintain its own chat state, invoke tools, and receive responses across time and decisions.

- **Multi-step, parallel tool execution**: The LLM may invoke multiple tools in one response, which are executed and resolved automatically before continuing the conversation.

- **Real-time and modular context enrichment** using Retrieval-Augmented Generation (RAG), with support for both:
  - **Static context**: Large datasets like link properties, travel times, charging prices, or facility data.
  - **Dynamic context**: Per-agent states or runtime knowledge inserted into vector databases and queried on the fly.

- **Declarative tool calling with full object support**:
  - Tools are defined with typed Java DTOs that serialize into LLM-visible schemas.
  - Arguments are automatically parsed back into DTOs, verified, and converted into rich Java domain objects.
  - Tool responses can be returned to the LLM or directly consumed by MATSim as simulation inputs.

- **Separation of real vs dummy tools**: Tools can either provide LLM-visible outputs or simply reconstruct internal objects (like a charging plan), enabling flexible chaining of decisions and postprocessing.

- **Pluggable modular architecture**: The system is backend-agnostic and supports:
  - LLM inference via OpenAI, LM Studio, Ollama
  - Embedding models for RAG (e.g., HuggingFace)
  - Swappable retrieval databases (e.g., ChromaDB)
  - Extensible tool registry and DTO system

- **Support for dataset generation and finetuning**: JSONL logging (planned) can capture multi-turn decision traces and tool inputs/outputs, enabling fine-tuning or distillation of smaller models.



### Key Features

* OpenAI-style multi step tool calling in Java
* Tool register via tool manager that automatically handles iterative tool calls (multi step communications with LLM)
* Individual chat thread management with tool calling. 
* Modular backend support (OpenAI, LM Studio, Ollama)
* Pluggable tools with automatic schema generation via DTOs
* Under the hood Retrieval integration with ChromaDB and HuggingFace embeddings with support for static and runtime embeddings.
* MATSim integration (ChatManagerContiner, ChatCompletionClient, VectorDB, ToolManager available anywhere within MATSim).

---

## ⚙️ Architecture

### 🧠 ChatManager (Core Coordinator for individual chat thread)

The `ChatManager` handles LLM conversations and decision logic for a single chat thread:

* Maintains full message history as `List<IChatMessage>`
* Sends requests to the selected `IChatCompletionClient` that manages context retreival under the hood.
* Receives tool calls and dispatches them to the `ToolManager`
* Injects tool responses into message history for multiple LLM rounds for relevant tools
* Prepares objects at the same time to be consumed by MATSim. 


### 🛠 ToolManager & Tool Framework

* Tools are Java classes implementing `ITool`
* Tool arguments are defined using `ToolArgument<T>` with T as the matsim consumable and `ToolArgumentDTO<T>` as the POJO class for GSON serialization
* DTOs expose a schema for LLM and parse responses into domain objects which then automatically is merged to generate ToolSchema.
* Allows for:
  * **Real Tools**: LLM uses the returned result
  * **Dummy Tools**: Results are consumed by MATSim (not shown to LLM)
* Tools register themselves via the `ToolRegistry`

### 💬 Chat Clients

Implements `IChatCompletionClient` to interact with different LLM providers:

* **OpenAI**: via HTTPS and bearer token
* **LM Studio / Ollama**: via local OpenAI-compatible endpoints
* Each backend has its own `*ChatRequest` and `*ChatResponse` implementation for serializing and desrializing. 

### 📚 Retrieval (RAG)

Optional module to provide context to LLMs:

* Uses ChromaDB (via Akimos Java client)
* HuggingFace model for embedding (e.g., `all-MiniLM-L6-v2`)
* Supports static and dynamic document insertion
* Controlled cleanup of database using `cleanVectorDbUponCompletion`

### 🧩 MATSim Integration

* Integrated using LLMModule. 
* Configuration exposed via `LLMConfigGroup`
* Global access to `ChatManagerContainer`, `ToolManager`, and `VectorDB` via injection.

---

## 🚀 Server Setup Guide

### 🔌 Local LLM Server (LM Studio / Ollama)

#### LM Studio

1. Download and launch LM Studio
2. Load a model (e.g., `llama3`) and an embedding model in server mode 
3. Enable the **OpenAI-compatible API** in settings
4. Default endpoint: `http://localhost:1234/v1/chat/completions`

#### Ollama

```bash
ollama run llama3
```

* Default endpoint: `http://localhost:11434/v1/chat/completions`
* (currently not implemented)

### 📚 Vector DB (ChromaDB)

To enable context injection via RAG:
download and install docker.

```bash
docker run -p 8000:8000 princepspolaris/chroma (this is a old version of chroma with v1 rest api. The akimos java client for chromadb only supports v1 api. V2 support is planned.)
```

* Default endpoint: `http://localhost:8000`
* Used by Akimos Java client for embedding and querying
* Requires no manual configuration beyond port exposure

---

## 🛠 How to Define and Use Tools

Look into the test cases and the ExampleTools class for detailed tool generations

### Tool Definition Example

```java
class EchoTool implements ITool<String> {

    private Map<String, ToolArgument<?, ? extends ToolArgumentDTO<?>>> arguments = new HashMap<>();

    public EchoTool() {
        // Register arguments
        registerArgument(SimpleStringDTO.forArgument("message"));
        registerArgument(SimpleBooleanDTO.forArgument("shout"));
    }

	@Override
	public IToolResponse<String> callTool(String id, Map<String, Object> arguments, IVectorDB vectorDB) {
		
		String msg = (String) arguments.getOrDefault("message", "");
        boolean shout = Boolean.TRUE.equals(arguments.getOrDefault("shout", ""));

        String result = shout ? msg.toUpperCase() : msg;
		
		
		return new IToolResponse<String>() {

			@Override
			public String getToolCallOutputContainer() {
				
				return result;
			}

			@Override
			public boolean isForLLM() {
				
				return false;// dummy tool
			}
			
		};
	}

}

```

### Register Tools

```java
toolManager.registerTool(new EchoTool()); // toolManager is binded through LLMModule directly from the config.
```

---

## 🧠 Agent Usage in MATSim

Agents use the `ChatManager` to query LLMs with current state and context:

```java
ChatManager manager = chatManagerContainer.getChatManagers().get(Id<> agentOrVehicleId);
IRequestMessage req = new SimpleRequestMessage(Role.USER,"message or info goes here");
manager.submit(req);
```

this function internally handles multi step and parallel tool calls and context retreival.

---

## 🧾 Configuration (LLMConfigGroup)

All settings (LLM server side, Vector database server side, LLM and embedding models and model parameters are configured using LLMConfigGroup)

```xml
<module name="llmConfig">
    <param name="backendType" value="openai"/>
    <param name="host" value="api.openai.com"/>
    <param name="authorization" value="Bearer YOUR_API_KEY"/>
    <param name="embeddingModelName" value="all-MiniLM-L6-v2"/>
    <param name="cleanVectorDbUponCompletion" value="dynamic_only"/>
...
...
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

This project is licensed under the [MIT License](https://opensource.org/licenses/MIT).

---
