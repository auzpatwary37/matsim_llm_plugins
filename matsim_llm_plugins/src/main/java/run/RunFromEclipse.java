package run;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.matsim.core.config.ConfigUtils;

public class RunFromEclipse {
	public static void main(String[] args) {
		
//		ConfigUtils.loadConfig(null);
		
		String[] args1 = new String[] {
				"--iterations","500",
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
				"--output","output21.01_osm_EOD",
				"--vehicles","data/dataForLLM/outputODVehicle_21_0.01.xml.gz",
				// -------- LLM --------
		        "--llmHost","localhost",
		        "--llmPort","1234",
		        "--llmPath","/v1/chat/completions",
		        "--llmModelName","qwen/qwen3.5-9b",   // put model if needed (e.g. qwen)

		        // -------- Embedding --------
		        "--embeddingPath","/v1/embeddings",
		        "--embeddingModelName","text-embedding-granite-embedding-278m-multilingual",

		        // -------- Auth --------
		        "--authorization","lm-studio",

		        // -------- Vector DB --------
		        "--vectorDbHost","localhost",
		        "--vectorDbPort","6334",
		        "--vectorDbCollectionName","matsim_llm_run1",
		        "--vectorDbSourceFile","data/static.txt",
		        "--cleanVectorDbUponCompletion","ALL"
				
		};
		
		Run.main(args1);
		
		
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
