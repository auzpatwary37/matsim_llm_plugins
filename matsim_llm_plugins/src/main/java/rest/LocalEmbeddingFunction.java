package rest;


import java.util.List;
import java.util.stream.Collectors;

import tech.amikos.chromadb.EmbeddingFunction;
import tech.amikos.chromadb.OpenAIEmbeddingFunction;
import tech.amikos.openai.CreateEmbeddingRequest;
import tech.amikos.openai.CreateEmbeddingResponse;
import tech.amikos.openai.OpenAIClient;

public class LocalEmbeddingFunction implements EmbeddingFunction {

    private final String modelName;

    private final OpenAIClient client;

    public LocalEmbeddingFunction(String openAIAPIKey) {
       this(openAIAPIKey, "text-embedding-ada-002", null);
    }

    public LocalEmbeddingFunction(String openAIAPIKey, String modelName) {
        this(openAIAPIKey, modelName, null);
    }

    public LocalEmbeddingFunction(String openAIAPIKey, String modelName, String apiEndpoint) {
        this.modelName = modelName;
        this.client = new OpenAIClient();
        this.client.apiKey(openAIAPIKey).baseUrl(apiEndpoint);
    }

    @Override
    public List<List<Float>> createEmbedding(List<String> documents) {
        CreateEmbeddingRequest req = new CreateEmbeddingRequest().model(this.modelName);
        req.input(new CreateEmbeddingRequest.Input(documents.toArray(new String[0])));
        CreateEmbeddingResponse response = this.client.createEmbedding(req);
        return response.getData().stream().map(emb -> emb.getEmbedding()).collect(Collectors.toList());
    }

    @Override
    public List<List<Float>> createEmbedding(List<String> documents, String model) {
        CreateEmbeddingRequest req = new CreateEmbeddingRequest().model(model);
        req.input(new CreateEmbeddingRequest.Input(documents.toArray(new String[0])));
        CreateEmbeddingResponse response = this.client.createEmbedding(req);
        return response.getData().stream().map(emb -> emb.getEmbedding()).collect(Collectors.toList());
    }


    public static LocalEFBuilder Instance() {
        return new LocalEFBuilder();
    }

    public static class LocalEFBuilder {
        private String openAIAPIKey;
        private String modelName = "text-embedding-ada-002";
        private String apiEndpoint = null;

        public LocalEmbeddingFunction build() {
            return new LocalEmbeddingFunction(openAIAPIKey, modelName, apiEndpoint);
        }

        public LocalEFBuilder withOpenAIAPIKey(String openAIAPIKey) {
            this.openAIAPIKey = openAIAPIKey;
            return this;
        }

        public LocalEFBuilder withModelName(String modelName) {
            this.modelName = modelName;
            return this;
        }

        public LocalEFBuilder withApiEndpoint(String apiEndpoint) {
            this.apiEndpoint = apiEndpoint;
            return this;
        }

    }
}

