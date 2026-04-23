package org.matsim.contrib.llm.run;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class RunFromEclipse {
	
	private static String loadApiKey() throws IOException {
		Properties props = new Properties();
		Path p = Path.of("src/main/resources/llm_api_keys.properties");
		if (Files.exists(p)) {
			props.load(Files.newInputStream(p));
		}
		return props.getProperty("openai.apiKey", "lm-studio");
	}
	
	private static String[] appendArg(String[] args, String key, String value) {
		for (int i = 0; i < args.length - 1; i += 2) {
			if (key.equals(args[i])) {
				String[] result = args.clone();
				result[i + 1] = value;
				return result;
			}
		}

		String[] result = new String[args.length + 2];
		System.arraycopy(args, 0, result, 0, args.length);
		result[args.length] = key;
		result[args.length + 1] = value;
		return result;
	}
	
	public static void main(String[] args) {
		String apiKey;
		try {
			apiKey = loadApiKey();
		} catch (IOException e) {
			apiKey = "lm-studio";
		}

		String[] args1 = new String[] {
				"--iterations","150",
				"--thread", "8",
				"--scale", ".01", 
				"--config", "data/dataForLLM/configMine.xml",
				"--network", "data/dataForLLM/osmMultimodal_laneFixed.xml",
				"--ts", "data/dataForLLM/osmTsMapped.xml", 
				"--tv", "data/dataForLLM/osmVehicles.xml",  
				"--plan", "data/dataForLLM/outputODPopulation_21_0.01.xml.gz",
				"--facilities", "data/dataForLLM/outputODFacilities21_0.01.xml.gz", 
				"--household","data/dataForLLM/outputODHouseholds_21_0.01.xml.gz",
				"--paramfile","src/main/resources/paramReaderTrial1_newData REsult (1).csv",
				"--clearplan", "true",
				"--ifCheckLanes", "true",
				"--output","output21.01_osmBase",
				"--vehicles","data/dataForLLM/outputODVehicle_21_0.01.xml.gz",
				// -------- LLM --------
		        "--llmHost","localhost",
		        "--llmPort","1234",
		        "--llmPath","/v1/chat/completions",
		        "--llmModelName","qwen/qwen3.5-9b",   // put model if needed (e.g. qwen)

		        // -------- Embedding --------
		        "--embeddingPath","/v1/embeddings",
		        "--embeddingModelName","text-embedding-nomic-embed-text-v1.5-embedding",

		        // -------- Auth --------
		        "--authorization","lm-studio",

		        // -------- Vector DB --------
		        "--vectorDbHost","localhost",
		        "--vectorDbPort","6334",
		        "--vectorDbCollectionName","matsim_llm_run1",
		        "--vectorDbSourceFile","data/static.txt",
		        "--cleanVectorDbUponCompletion","ALL",
		        "--LLMConnectionOption","replanning",
		        
		     // -------- LLM CONTROL --------
		        "--maxOutputToken","10000",
		        "--numberOfAIAgent","10",
		        "--iterationToStartAIAgent","0",
		        "--maxToolIteration","10",
				
		};
		
		String[] args2 = new String[] {
				"--iterations","50",
				"--thread", "8",
				"--scale", ".01", 
				"--config", "data\\dataForLLMFromBase\\output_config.xml",
				"--network", "data\\dataForLLMFromBase\\output_network.xml.gz",
				"--ts", "data\\dataForLLMFromBase\\output_transitSchedule.xml.gz", 
				"--tv", "data\\dataForLLMFromBase\\output_transitVehicles.xml.gz",  
				"--plan", "data\\dataForLLMFromBase\\output_plans.xml.gz",
				"--facilities", "data\\dataForLLMFromBase\\output_facilities.xml.gz", 
				"--household","data\\dataForLLMFromBase\\output_households.xml.gz",
				"--paramfile","src/main/resources/paramReaderTrial1_newData REsult (1).csv",
				"--clearplan", "false",
		       "--ifCheckLanes", "true",
				"--output","output21.01_osm_10AIAgent",
				"--vehicles","data\\dataForLLMFromBase\\output_vehicles.xml.gz",
				// -------- LLM --------
		        "--llmHost","localhost",
		        "--llmPort","1234",
		        "--llmPath","/v1/chat/completions",
		        "--llmModelName","qwen/qwen3.5-9b",   // put model if needed (e.g. qwen)

		        // -------- Embedding --------
		        "--embeddingPath","/v1/embeddings",
		        "--embeddingModelName","text-embedding-nomic-embed-text-v1.5-embedding",

		        // -------- Auth --------
		        "--authorization","lm-studio",

		        // -------- Vector DB --------
		        "--vectorDbHost","localhost",
		        "--vectorDbPort","6334",
		        "--vectorDbCollectionName","matsim_llm_run1",
		        "--vectorDbSourceFile","data/static.txt",
		        "--cleanVectorDbUponCompletion","NONE",
		        "--LLMConnectionOption","replanning",
		        
		     // -------- LLM CONTROL --------
		        "--maxOutputToken","10000",
		        "--numberOfAIAgent","10",
		        "--iterationToStartAIAgent","0",
		        "--maxToolIteration","10",
		};
		
		
		String[] args3 = new String[] {
				"--iterations","50",
				"--thread", "8",
				"--scale", ".01", 
				"--config", "data\\dataForLLMFromBase\\output_config.xml",
				"--network", "data\\dataForLLMFromBase\\output_network.xml.gz",
				"--ts", "data\\dataForLLMFromBase\\output_transitSchedule.xml.gz", 
				"--tv", "data\\dataForLLMFromBase\\output_transitVehicles.xml.gz",  
				"--plan", "data\\dataForLLMFromBase\\output_plans.xml.gz",
				"--facilities", "data\\dataForLLMFromBase\\output_facilities.xml.gz", 
				"--household","data\\dataForLLMFromBase\\output_households.xml.gz",
				"--paramfile","src/main/resources/paramReaderTrial1_newData REsult (1).csv",
				"--clearplan", "false",
		       "--ifCheckLanes", "true",
				"--output","output21.01_osm_10AIAgent_run2",
				"--vehicles","data\\dataForLLMFromBase\\output_vehicles.xml.gz",
				// -------- LLM --------
		        "--llmHost","localhost",
		        "--llmPort","1234",
		        "--llmPath","/v1/chat/completions",
		        "--llmModelName","qwen/qwen3.5-9b",   // put model if needed (e.g. qwen)

		        // -------- Embedding --------
		        "--embeddingPath","/v1/embeddings",
		        "--embeddingModelName","text-embedding-nomic-embed-text-v1.5-embedding",

		        // -------- Auth --------
		        "--authorization","lm-studio",

		        // -------- Vector DB --------
		        "--vectorDbHost","localhost",
		        "--vectorDbPort","6334",
		        "--vectorDbCollectionName","matsim_llm_run1",
		        "--vectorDbSourceFile","data/static.txt",
		        "--cleanVectorDbUponCompletion","NONE",
		        "--LLMConnectionOption","replanning",
		        
		     // -------- LLM CONTROL --------
		        "--maxOutputToken","10000",
		        "--numberOfAIAgent","10",
		        "--iterationToStartAIAgent","0",
		        "--maxToolIteration","10",
		};
		
		Run.main(appendArg(args3, "--authorization", apiKey));
		
		
	}
	
	public static String fixTravelTimeCalculatorParam(String originalConfigPath) throws IOException {

	    String xml = Files.readString(Path.of(originalConfigPath), StandardCharsets.UTF_8);

	    // remove ONLY the invalid param inside travelTimeCalculator module
	    xml = xml.replaceAll(
	            "(?s)(<module\\s+name=\"travelTimeCalculator\"[^>]*>.*?)(<param\\s+name=\"travelTimeCalculator\"\\s+value=\".*?\"\\s*/>)(.*?</module>)",
	            "$1$3"
	    );

	    Path temp = Files.createTempFile("config-fixed-", ".xml");
	    Files.writeString(temp, xml, StandardCharsets.UTF_8);

	    return temp.toString();
	}

}
