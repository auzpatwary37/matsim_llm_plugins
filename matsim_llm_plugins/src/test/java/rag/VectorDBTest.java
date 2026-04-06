package rag;

import java.net.MalformedURLException;
import java.nio.file.Paths;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import matsimBinding.LLMConfigGroup;
import matsimBinding.LLMIntegrationModule;

public class VectorDBTest {
	
	public static void main(String[] args) throws MalformedURLException {
		LLMConfigGroup config = buildConfig("my collection",null);
		System.out.println(config.getFullLlmUrl());
		System.out.println(config.getFullEmbeddingUrl());
		System.out.println(config.getFullVectorDbBaseUrl());
		
		VectorDBImplement vectorDB = new VectorDBImplement(config);
		vectorDB.insert("Hi, test!!!", null);
		var obj = vectorDB.query("asking about past history", 5);
		System.out.println(obj);
		vectorDB.clearDynamicDocuments();
		
		obj = vectorDB.query("asking about past history", 5);
		System.out.println("after clearing "+obj);
		
		vectorDB.clearStaticDocuments();
		obj = vectorDB.query("asking about past history", 5);
		System.out.println("after clearing "+obj);
		
		Config configMain = ConfigUtils.createConfig();
		ConfigUtils.loadConfig(configMain,"data/config_with_calibrated_parameters.xml");
		configMain.plans().setInputFile("plan.xml");
		configMain.transit().setTransitScheduleFile("montreal_transit_schedules.xml");
		configMain.transit().setVehiclesFile("montreal_transit_vehicles.xml");
		configMain.setContext(Paths.get("data/").toUri().toURL());
		configMain.addModule(config);
		Scenario scn= ScenarioUtils.loadScenario(configMain);
		Controler controller = new Controler(scn);
		controller.addOverridingModule(new LLMIntegrationModule());
		controller.run();
		
	}
	
	private static LLMConfigGroup buildConfig(String collectionName, String staticSourcePath) {
	    LLMConfigGroup config = new LLMConfigGroup();
	    config.setVectorDbHost("localhost");
	    config.setVectorDbPort(6334);
	    config.setVectorDbCollectionName(collectionName);
	    config.setLlmHost("localhost");
	    config.setLlmPort(1234);
	    config.setEmbeddingPath("/v1/embeddings");
	    config.setEmbeddingModelName("text-embedding-granite-embedding-278m-multilingual");
	    config.setAuthorization("lm-studio");
	    config.setVectorDBSourceFile(staticSourcePath != null ? staticSourcePath : "data/static.txt");
	    config.setCleanVectorDbUponCompletion("ALL");
	    return config;
	}
	
	public static String createUUID() { return java.util.UUID.randomUUID().toString(); }

}
