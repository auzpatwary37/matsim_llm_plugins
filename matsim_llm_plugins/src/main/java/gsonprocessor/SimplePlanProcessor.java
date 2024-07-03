package gsonprocessor;

import java.util.stream.Collectors;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.PopulationUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import nlprocessor.GsonTrial.PlanElementDeserializer;

public class SimplePlanProcessor{
	public static final String PLUGIN = "plugin";
	public static final String PLUGOUT = "plugout";
	
	public static void main(String[] args) {
		Population pop = PopulationUtils.readPopulation("NLP/output_plans.xml.gz");
		Person person = pop.getPersons().values().stream().collect(Collectors.toList()).get(0);
		
		Plan plan = person.getSelectedPlan();
		
		PlanGson pg = PlanGson.createPlanGson(plan);
		GsonBuilder gsonBuilder = new GsonBuilder()
				.registerTypeAdapter(PlanElement.class, new PlanElementDeserializer());
		gsonBuilder.serializeNulls();
	    gsonBuilder.serializeSpecialFloatingPointValues();
	    gsonBuilder.setPrettyPrinting();
	    Gson gson = gsonBuilder.create();
	    
	    String jsonString = gson.toJson(pg);
		System.out.println(jsonString);
		PlanGson plan_gson = gson.fromJson(jsonString, PlanGson.class);
		
		System.out.print(gson.toJson(plan_gson));
		
	}
	
	

}
