package chatcommons;

import java.net.http.HttpClient;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.http.client.jdk.JdkHttpClient;
import dev.langchain4j.http.client.jdk.JdkHttpClientBuilder;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
import dev.langchain4j.data.embedding.Embedding;

public class QdrantTest {

    public static void main(String[] args) {

        try {
            // -------------------------------
            // 1. Build JDK HTTP client for LM Studio
            // IMPORTANT:
            // - force HTTP/1.1
            // - use 127.0.0.1 instead of localhost
            // -------------------------------
            HttpClient.Builder httpClientBuilder = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1);

            JdkHttpClientBuilder jdkHttpClientBuilder = JdkHttpClient.builder()
                    .httpClientBuilder(httpClientBuilder);

            // -------------------------------
            // 2. Build embedding model
            // -------------------------------
            EmbeddingModel embeddingModel = OpenAiEmbeddingModel.builder()
                    .baseUrl("http://127.0.0.1:1234/v1")
                    .apiKey("lm-studio") // dummy key for LM Studio local server
                    .modelName("text-embedding-granite-embedding-278m-multilingual")
                    .httpClientBuilder(jdkHttpClientBuilder)
                    .logRequests(true)
                    .logResponses(true)
                    .build();

            // -------------------------------
            // 3. Test text
            // -------------------------------
            String text = "This is a test embedding for Qdrant.";

            System.out.println("Before embed...");
            Embedding embedding = embeddingModel.embed(text).content();
            System.out.println("After embed.");
            System.out.println("Embedding dimension = " + embedding.vector().length);

            // -------------------------------
            // 4. Build text segment
            // -------------------------------
            TextSegment segment = TextSegment.from(
                    text,
                    Metadata.from("source", "qdrant-smoke-test")
            );

            // -------------------------------
            // 5. Connect to Qdrant
            // IMPORTANT:
            // port 6334 = gRPC
            // -------------------------------
            EmbeddingStore<TextSegment> embeddingStore = QdrantEmbeddingStore.builder()
                    .host("localhost")
                    .port(6334)
                    .collectionName("test_collection")
                    .build();

            // -------------------------------
            // 6. Store embedding + segment
            // -------------------------------
            String id = embeddingStore.add(embedding, segment);

            System.out.println("Stored successfully in Qdrant.");
            System.out.println("Qdrant point id = " + id);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}