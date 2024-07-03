package nlprocessor;

import java.lang.reflect.Type;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.MatsimFacilitiesReader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

public class GsonTrial {
	public static void main(String[] args) {
		Population pop  = PopulationUtils.readPopulation("NLP/output_plans.xml.gz");
		Scenario scn = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimFacilitiesReader(scn).readFile("NLP/output_facilities.xml.gz");
		ActivityFacilities facs = scn.getActivityFacilities();
		GsonBuilder gsonBuilder = new GsonBuilder()
				.registerTypeAdapter(PlanElement.class, new PlanElementDeserializer());
		gsonBuilder.serializeNulls();
	    gsonBuilder.serializeSpecialFloatingPointValues();
	    gsonBuilder.setPrettyPrinting();
	    Gson gson = gsonBuilder.create();
		for(Person p:pop.getPersons().values()){
			String att = gson.toJson(p.getAttributes());
			String planAtt = gson.toJson(p.getSelectedPlan().getAttributes());
			String planStr = gson.toJson(p.getSelectedPlan());
			System.out.println(att);
			System.out.println(planStr);
			System.out.println(planAtt);
			

			// Deserialize the JSON string into an array of PlanElements
			PlanElement[] planElements = gson.fromJson(planStr, PlanElement[].class);
		};
		
//	    Id<Person> pId = Id.createPersonId("1000522");
//	    
//	    Person person = pop.getPersons().get(pId);
//	    Plan plan = person.getSelectedPlan();
//	    Plan evPlan = person.getPlans().stream().filter(p->isEvPlan(p)).findAny().get();
//	    
//	    String personStr = PersonNLProcessor.buildPersonDescription(person, facs)+"\n";
//	    String planStr = PersonNLProcessor.buildPlanDescription(plan, facs);
//	    String planEVStr = PersonNLProcessor.buildPlanDescription(evPlan, facs);
//	    
//		System.out.println(personStr);
//		System.out.println(planStr);
//		System.out.println(planEVStr);
	}
	
	private static boolean isEvPlan(Plan plan) {
		for(PlanElement pe:plan.getPlanElements()) {
			if(pe instanceof Activity) {
				if(((Activity)pe).getType().equals("car plugin interaction"))return true;
			}
		}
		return false;
	}
	public static class PlanElementDeserializer implements JsonDeserializer<PlanElement> {
	    

		@Override
		public PlanElement deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
				throws JsonParseException {
			JsonObject jsonObject = json.getAsJsonObject();
	        JsonElement elementTypeElement = jsonObject.get("type");
//	        String elementType = elementTypeElement.getAsString();

	        // Check the type of PlanElement and instantiate the appropriate class
	        if (elementTypeElement != null &&  !elementTypeElement.isJsonNull()) {
	            // If "type" field is present, it's an activity
	            return context.deserialize(json, ActivityWrapper.class);
	        } else {
	            // If "type" field is not present, it's a leg
	            return context.deserialize(json, LegWrapper.class);
	        }
		}
	}
	public class ActivityWrapper {
	    private double endTime;
	    private double startTime;
	    private double dur;
	    private String type;
	    private Coord coord;
	    private Id<Link> linkId;
	    private Id<ActivityFacility> facilityId;
	    private Map<String, Object> attributes;
	    
	    public Activity getActivity() {
	    	Activity act = PopulationUtils.createActivityFromCoordAndLinkId(type, coord, linkId);
	    	if(Double.isFinite(startTime))act.setEndTime(endTime);
	    	if(Double.isFinite(endTime))act.setStartTime(endTime);
	    	return act;
	    }
	    // Getters and Setters
	    // ...
	}

	public class LegWrapper {
	    private Route route;
	    private double depTime;
	    private double travTime;
	    private String mode;
	    private String routingMode;
	    private Map<String, Object> attributes;
	    
	    
	    public Leg getLeg() {
	    	Leg leg = PopulationUtils.createLeg(mode);
	    	leg.setDepartureTime(depTime);
	    	leg.setRoutingMode(routingMode);
	    	
	    	return leg;
	    }
	    // Getters and Setters
	    // ...
	}

	

}
