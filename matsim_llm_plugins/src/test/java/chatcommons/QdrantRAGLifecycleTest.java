package chatcommons;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Collections.VectorParams;

import java.util.List;

public class QdrantRAGLifecycleTest {

    public static void main(String[] args) {
        String host = "127.0.0.1";
        int port = 6334;
        String collectionName = "matsim_test_collection";
        int dimension = 4;

        try (QdrantClient client = new QdrantClient(QdrantGrpcClient.newBuilder(host, port, false).build())) {
            
            // --- STEP 1: INFRASTRUCTURE (Create Collection) ---
            System.out.println("Cleaning and creating collection: " + collectionName);
            
            // Delete if exists to ensure a fresh test
            if (client.listCollectionsAsync().get().contains(collectionName)) {
                client.deleteCollectionAsync(collectionName).get();
            }

            // Create the collection with required parameters
            client.createCollectionAsync(collectionName, 
                VectorParams.newBuilder()
                    .setDistance(Distance.Cosine)
                    .setSize(dimension)
                    .build()).get();
            
            System.out.println("Collection created successfully.");

            // --- STEP 2: SAVE (Push Data) ---
            EmbeddingStore<TextSegment> store = QdrantEmbeddingStore.builder()
                    .host(host)
                    .port(port)
                    .useTls(false)
                    .collectionName(collectionName)
                    .build();

            float[] vector = new float[] {0.9f, 0.8f, 0.7f, 0.6f};
            TextSegment segment = TextSegment.from(
                "MATSim Agent 505: Rerouting due to congestion on Link 10.",
                Metadata.from("agent_id", "505")
            );

            System.out.println("\nPushing simulation data...");
            String id = store.add(Embedding.from(vector), segment);
            System.out.println("Data saved. Point ID: " + id);

            // --- STEP 3: SEARCH (Pull/Inference) ---
            System.out.println("\nPerforming Vector Search (Pulling context)...");
            
            // Query with a slightly different vector to test similarity
            float[] queryVector = new float[] {0.89f, 0.81f, 0.69f, 0.61f};
            
            EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                    .queryEmbedding(Embedding.from(queryVector))
                    .maxResults(1)
                    .build();

            List<EmbeddingMatch<TextSegment>> matches = store.search(searchRequest).matches();

            if (!matches.isEmpty()) {
                EmbeddingMatch<TextSegment> match = matches.get(0);
                System.out.println("Successfully pulled context!");
                System.out.println("Matched Text: " + match.embedded().text());
                System.out.println("Similarity Score: " + match.score());
                System.out.println("Agent ID from Metadata: " + match.embedded().metadata().getString("agent_id"));
            } else {
                throw new RuntimeException("Test Failed: No vector match found!");
            }

            System.out.println("\n--- FULL TEST PASSED ---");

        } catch (Exception e) {
            System.err.println("\n--- TEST FAILED ---");
            e.printStackTrace();
        }
    }
}