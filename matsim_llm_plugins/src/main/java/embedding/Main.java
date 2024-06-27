package embedding;

import com.google.gson.internal.LinkedTreeMap;
import tech.amikos.chromadb.Client;
import tech.amikos.chromadb.Collection;
import tech.amikos.chromadb.EmbeddingFunction;
import tech.amikos.chromadb.OpenAIEmbeddingFunction;

import java.util.*;

public class Main {
    public static void main(String[] args) {
        try {
            Client client = new Client(System.getenv("CHROMA_URL"));
            String apiKey = System.getenv("OPENAI_API_KEY");
            EmbeddingFunction ef = new OpenAIEmbeddingFunction(apiKey,"text-embedding-3-small");
            Collection collection = client.createCollection("test-collection", null, true, ef);
            List<Map<String, String>> metadata = new ArrayList<>();
            metadata.add(new HashMap<String, String>() {{
                put("type", "scientist");
            }});
            metadata.add(new HashMap<String, String>() {{
                put("type", "spy");
            }});
            collection.add(null, metadata, Arrays.asList("Hello, my name is John. I am a Data Scientist.", "Hello, my name is Bond. I am a Spy."), Arrays.asList("1", "2"));
            Collection.QueryResponse qr = collection.query(Arrays.asList("Who is the spy"), 10, null, null, null);
            System.out.println(qr);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e);
        }
    }
}
