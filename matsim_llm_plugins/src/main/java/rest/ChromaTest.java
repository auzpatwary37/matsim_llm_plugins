package rest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tech.amikos.chromadb.Client;
import tech.amikos.chromadb.Collection;
import tech.amikos.chromadb.Collection.QueryResponse;
import tech.amikos.chromadb.handler.ApiException;
import tech.amikos.chromadb.model.QueryEmbedding.IncludeEnum;

public class ChromaTest {
public static void main(String[] args) {
	 Client client = new Client("http://localhost:8000");
     try {
		Map<String, BigDecimal> hb = client.heartbeat();
		System.out.println(hb);
		
		LocalEmbeddingFunction func = LocalEmbeddingFunction.Instance()
				.withApiEndpoint("http://localhost:1234/v1/embeddings")
				.withModelName("nomic-ai/nomic-embed-text-v1.5-GGUF")
				.withOpenAIAPIKey("lm-studio")
				.build();
		
//		var out = func.createEmbedding(List.of("Hi, I am Ashraf."));
//		Collection collection = client.createCollection("MATSimtrial", null, false, func);
		Collection collection = client.getCollection("MATSim_trial", func);
		
		
		List<String>documents = List.of(
		             "Mars, often called the 'Red Planet', has captured the imagination of scientists and space enthusiasts alike.",
		             "The Hubble Space Telescope has provided us with breathtaking images of distant galaxies and nebulae.",
		             "The concept of a black hole, where gravity is so strong that nothing can escape it, was first theorized by Albert Einstein's theory of general relativity.",
		             "The Renaissance was a pivotal period in history that saw a flourishing of art, science, and culture in Europe.",
		             "The Industrial Revolution marked a significant shift in human society, leading to urbanization and technological advancements.",
		             "The ancient city of Rome was once the center of a powerful empire that spanned across three continents.",
		             "Dolphins are known for their high intelligence and social behavior, often displaying playful interactions with humans.",
		             "The chameleon is a remarkable creature that can change its skin color to blend into its surroundings or communicate with other chameleons.",
		             "The migration of monarch butterflies spans thousands of miles and involves multiple generations to complete.",
		             "Christopher Nolan's 'Inception' is a mind-bending movie that explores the boundaries of reality and dreams.",
		             "The 'Lord of the Rings' trilogy, directed by Peter Jackson, brought J.R.R. Tolkien's epic fantasy world to life on the big screen.",
		             "Pixar's 'Toy Story' was the first feature-length film entirely animated using computer-generated imagery (CGI).",
		             "Superman, known for his incredible strength and ability to fly, is one of the most iconic superheroes in comic book history.",
		             "Black Widow, portrayed by Scarlett Johansson, is a skilled spy and assassin in the Marvel Cinematic Universe.",
		             "The character of Iron Man, played by Robert Downey Jr., kickstarted the immensely successful Marvel movie franchise in 2008."
		         );
		List<Map<String, String>> metadatas = new ArrayList<>();

        Map<String, String> metadata1 = new HashMap<>();
        metadata1.put("source", "Space");
        metadatas.add(metadata1);

        Map<String, String> metadata2 = new HashMap<>();
        metadata2.put("source", "Space");
        metadatas.add(metadata2);

        Map<String, String> metadata3 = new HashMap<>();
        metadata3.put("source", "Space");
        metadatas.add(metadata3);

        Map<String, String> metadata4 = new HashMap<>();
        metadata4.put("source", "History");
        metadatas.add(metadata4);

        Map<String, String> metadata5 = new HashMap<>();
        metadata5.put("source", "History");
        metadatas.add(metadata5);

        Map<String, String> metadata6 = new HashMap<>();
        metadata6.put("source", "History");
        metadatas.add(metadata6);

        Map<String, String> metadata7 = new HashMap<>();
        metadata7.put("source", "Animals");
        metadatas.add(metadata7);

        Map<String, String> metadata8 = new HashMap<>();
        metadata8.put("source", "Animals");
        metadatas.add(metadata8);

        Map<String, String> metadata9 = new HashMap<>();
        metadata9.put("source", "Animals");
        metadatas.add(metadata9);

        Map<String, String> metadata10 = new HashMap<>();
        metadata10.put("source", "Movies");
        metadatas.add(metadata10);

        Map<String, String> metadata11 = new HashMap<>();
        metadata11.put("source", "Movies");
        metadatas.add(metadata11);

        Map<String, String> metadata12 = new HashMap<>();
        metadata12.put("source", "Movies");
        metadatas.add(metadata12);

        Map<String, String> metadata13 = new HashMap<>();
        metadata13.put("source", "Superheroes");
        metadatas.add(metadata13);

        Map<String, String> metadata14 = new HashMap<>();
        metadata14.put("source", "Superheroes");
        metadatas.add(metadata14);

        Map<String, String> metadata15 = new HashMap<>();
        metadata15.put("source", "Superheroes");
        metadatas.add(metadata15);
		List<String>ids = List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15");
		List<List<Float>>embeddings = func.createEmbedding(documents);
		
		collection.add(embeddings, metadatas, documents, ids);
		
		QueryResponse results = collection.query(List.of("Give me some facts about animals"),3, null, null, List.of(IncludeEnum.DOCUMENTS));
		
		System.out.println(results.getDocuments().get(0));
		
		ChatCompletionClient chatClient = new ChatCompletionClient.Builder()
    			.setChatAPI_URL("http://localhost:1234/v1/chat/completions")
    			.setEmbeddingAPI_URL("http://localhost:1234/v1/embeddings")
    			.setIfStream(false)
    			.setMaxToken(-1)
    			.setTemperature(.7)
    			.build();
		String prompt = "Here is a document about some fun facts about different topics. Which one of them is your favourite?";
		var doc = collection.query(List.of(prompt),3, null, null, List.of(IncludeEnum.DOCUMENTS));
		System.out.println(doc);
		String response = chatClient.getResponse("Provide answer from the attached document, if you do not know, simply say I do not know.",
				"Here is a document about some fun facts about different topics. Which one of them is your favourite?"+doc
		).getContent();
		System.out.println(response);
	} catch (ApiException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
}
}
