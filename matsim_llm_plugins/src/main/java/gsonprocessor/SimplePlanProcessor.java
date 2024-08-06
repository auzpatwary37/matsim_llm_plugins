package gsonprocessor;

import java.util.stream.Collectors;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.PopulationUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


public class SimplePlanProcessor{
	public static final String PLUGIN = "plugin";
	public static final String PLUGOUT = "plugout";
	
	public static void main(String[] args) {
		Population pop = PopulationUtils.readPopulation("data\\1p daily\\40.plans.xml.gz");
		Person person = pop.getPersons().values().stream().collect(Collectors.toList()).get(0);
		
		Plan plan = person.getSelectedPlan();
		
		PlanGson pg = PlanGson.createPlanGson(plan);
		GsonBuilder gsonBuilder = new GsonBuilder()
				.registerTypeAdapter(PlanElementGson.class, new PlanElementGsonDeserializer())
				.serializeNulls()
				.serializeSpecialFloatingPointValues()
				.setPrettyPrinting();
	    Gson gson = gsonBuilder.create();
	    System.out.println(plan.getPerson().getId());
	    String jsonString = gson.toJson(pg);
		System.out.println(jsonString);
		PlanGson plan_gson = gson.fromJson(jsonString, PlanGson.class);
		
		System.out.print(gson.toJson(plan_gson));
		String response = "{\"activitiesAndLegs\":[{\"carLocation\":\"home\",\"endTime\":0,\"activityType\":\"home\"},{\"mode\":\"car\"},{\"carLocation\":\"work\",\"endTime\":0,\"activityType\":\"work\"},{\"mode\":\"car_passenger\"},{\"carLocation\":\"home\",\"endTime\":0,\"activityType\":\"leisure\"},{\"mode\":\"car_passenger\"},{\"carLocation\":\"home\",\"endTime\":0,\"activityType\":\"home\"}]}";
		
		PlanGson planFromGpt = gson.fromJson(response, PlanGson.class);
		System.out.println(planFromGpt);
		Plan plan_ = planFromGpt.getPlan();
		System.out.println(plan_);
	}
	
	

}
