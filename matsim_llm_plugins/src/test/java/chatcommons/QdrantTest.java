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

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Collections.VectorParams;

public class QdrantTest {

    public static void main(String[] args) {

        try {
            // 1. HTTP client
            HttpClient.Builder httpClientBuilder = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1);

            JdkHttpClientBuilder jdkHttpClientBuilder = JdkHttpClient.builder()
                    .httpClientBuilder(httpClientBuilder);

            // 2. Embedding model
            EmbeddingModel embeddingModel = OpenAiEmbeddingModel.builder()
                    .baseUrl("http://127.0.0.1:1234/v1")
                    .apiKey("lm-studio")
                    .modelName("text-embedding-granite-embedding-278m-multilingual")
                    .httpClientBuilder(jdkHttpClientBuilder)
                    .logRequests(true)
                    .logResponses(true)
                    .build();

            // 3. Text
            String text = "This is a test embedding for Qdrant.";

            System.out.println("Before embed...");
            Embedding embedding = embeddingModel.embed(text).content();
            System.out.println("After embed.");
            System.out.println("Embedding dimension = " + embedding.vector().length);

            // 4. Segment
            TextSegment segment = TextSegment.from(
                    text,
                    Metadata.from("source", "qdrant-smoke-test")
            );

            // 5. CREATE COLLECTION (FIX)
            QdrantClient client = new QdrantClient(
                    QdrantGrpcClient.newBuilder("127.0.0.1", 6334, false).build()
            );

            boolean exists = client.listCollectionsAsync().get()
                    .stream()
                    .anyMatch(c -> c.equals("test_collection"));

            if (!exists) {
                client.createCollectionAsync(
                        "test_collection",
                        VectorParams.newBuilder()
                                .setDistance(Distance.Cosine)
                                .setSize(embedding.vector().length)
                                .build()
                ).get();
            }

            // 6. Store
            EmbeddingStore<TextSegment> embeddingStore = QdrantEmbeddingStore.builder()
                    .host("localhost")
                    .port(6334)
                    .collectionName("test_collection")
                    .build();

            String id = embeddingStore.add(embedding, segment);

            System.out.println("Stored successfully in Qdrant.");
            System.out.println("Qdrant point id = " + id);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}