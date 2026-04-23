package matsimBinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.router.TripRouter;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Provider;

import chatcommons.ChatManagerContainer;
import chatcommons.DefaultChatManager;
import chatcommons.IChatCompletionClient;
import chatcommons.IChatManager;
import chatcommons.Role;
import chatrequest.SimpleRequestMessage;
import chatresponse.ChatResult;
import dev.langchain4j.model.chat.response.ChatResponse;
import matsimdtobjects.PlanDTO;
import prompts.IndividualPrompt;
import rag.IVectorDB;
import tools.IToolManager;
import tools.IToolResponse;
import tools.Implement.ExtractPlanTool;

public class LLMControllerListener implements StartupListener, IterationEndsListener, IterationStartsListener,BeforeMobsimListener, ShutdownListener{

	@Inject
	private Config config;
	
	@Inject
	private Scenario scenario;
	
	@Inject
	private IVectorDB vectorDB;
	
	@Inject
	private ChatManagerContainer chatContainer;
	
	@Inject
	private IChatCompletionClient chatClient;
	
	@Inject
	private IToolManager toolManager;
	
	@Inject
	private Provider<TripRouter> tripRouterProvider;
	
	private LLMConfigGroup llmConfig;
	
	private Gson gson = new Gson();
	
	private int numberOfLLMAgent = 10;

	
	
	private Map<String,Object> contextObject = new HashMap<>();
	
	
	@Inject
	public LLMControllerListener() {
		
	}
	
	private List<Id<Person>> LLMAgentsId = new ArrayList<>();
	
	
	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		//Lets try sending a query and check if the tool calling and Rag is working as expected. 
		
		this.chatContainer.getAll().entrySet().forEach(chat->{
			Person person = (Person) chat.getValue().getContextObject().get("person");
			Plan plan = person.getSelectedPlan();
			String basePlan = PlanDTO.toDTOFromBaseObject().apply(plan).toJsonObject(this.gson).toString();
			System.out.println("Sending querry for person Id "+ person.getId());
			ChatResult result = chat.getValue().submit(new SimpleRequestMessage(
			        Role.USER,
			        IndividualPrompt.planExtractPrompt+"\n"+basePlan
			    ), null);
			Map<String, IToolResponse<?>> output = result.toolResponses;
			Plan outPlan = null;
			
			for(IToolResponse<?> response:output.values()) {
				if(response.getName().equals(ExtractPlanTool.Name)) {
					outPlan = (Plan) response.getToolCallOutputContainer();
				}
			}
			
			
			
		});
		
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		this.llmConfig = (LLMConfigGroup)this.config.getModules().get(LLMConfigGroup.GROUP_NAME);
		int aiAgents = this.llmConfig.getNumberOfAIAgents();
		if(aiAgents<0) {
			this.numberOfLLMAgent = this.scenario.getPopulation().getPersons().size();
		}else {
			this.numberOfLLMAgent = aiAgents;
		}
		this.contextObject.put("tripRoutersProvider", this.tripRouterProvider);
		this.contextObject.put("activityFacilities", scenario.getActivityFacilities());
		this.contextObject.put("scenario", this.scenario);
		double prob = ((double)numberOfLLMAgent)/this.scenario.getPopulation().getPersons().size();
		this.scenario.getPopulation().getPersons().entrySet().forEach(p->{
			if(MatsimRandom.getLocalInstance().nextDouble()<prob) {
				this.LLMAgentsId.add(p.getKey());
				String context = LLMControllerListener.extract(p.getValue()).ragText;
				Map<String,String> metaData = new HashMap<>();
				metaData.put("personId", p.getKey().toString());
				metaData.put("type", "attribute");
				//this.vectorDB.insert(context,metaData);//inserted in Agent Experience Handler
				IChatManager chatManager = new DefaultChatManager(Id.create(p.getKey().toString(), IChatManager.class), chatClient, toolManager, vectorDB, 
						(LLMConfigGroup)this.config.getModules().get(LLMConfigGroup.GROUP_NAME)
						);
				chatManager.setSystemMessage(IndividualPrompt.systemPrompt + " You are person "+ p.getKey().toString());
				chatManager.setPersonId(p.getKey());
				chatManager.setContextObject(new HashMap<>(this.contextObject));
				chatManager.getContextObject().put("person",p.getValue());
				this.chatContainer.add(chatManager);
			}
		});
		
	}
	
	public static ExtractedPersonContext extract(Person person) {

	    Map<String, Object> attrs = person.getAttributes().getAsMap();
	    Map<String, String> structured = new LinkedHashMap<>();

	    putIfPresent(structured, "personId", firstNonNull(
	            attrs.get("person_id"),
	            person.getId() != null ? person.getId().toString() : null
	    ));

	    putIfPresent(structured, "ageRange", decodeAge(attrs.get("age")));
	    putIfPresent(structured, "sex", decodeSex(attrs.get("sex")));
	    putIfPresent(structured, "hasLicense", decodeYesNo(attrs.get("hasLicense")));
	    putIfPresent(structured, "carAvailability", attrs.get("carAvail"));
	    putIfPresent(structured, "bikeAvailability", attrs.get("bikeAvailability"));
	    putIfPresent(structured, "isCarPassenger", attrs.get("isCarPassenger"));
	    putIfPresent(structured, "householdId", attrs.get("household_id"));
	    putIfPresent(structured, "householdIncomeClass", attrs.get("household_income"));
	    putIfPresent(structured, "economicSector", attrs.get("economic_sector"));

	    StringBuilder sb = new StringBuilder();
	    sb.append("Person profile:\n");

	    if (structured.containsKey("personId"))
	        sb.append("- Person ID: ").append(structured.get("personId")).append("\n");

	    if (structured.containsKey("ageRange"))
	        sb.append("- Age group: ").append(structured.get("ageRange")).append("\n");

	    if (structured.containsKey("sex"))
	        sb.append("- Sex: ").append(structured.get("sex")).append("\n");

	    if (structured.containsKey("hasLicense"))
	        sb.append("- Has driving license: ").append(structured.get("hasLicense")).append("\n");

	    if (structured.containsKey("carAvailability"))
	        sb.append("- Car availability: ").append(structured.get("carAvailability")).append("\n");

	    if (structured.containsKey("bikeAvailability"))
	        sb.append("- Bike availability: ").append(structured.get("bikeAvailability")).append("\n");

	    if (structured.containsKey("isCarPassenger"))
	        sb.append("- Can act as car passenger: ").append(structured.get("isCarPassenger")).append("\n");

	    if (structured.containsKey("householdIncomeClass"))
	        sb.append("- Household income class: ").append(structured.get("householdIncomeClass")).append("\n");

	    if (structured.containsKey("economicSector"))
	        sb.append("- Economic sector code: ").append(structured.get("economicSector")).append("\n");

	    if (structured.containsKey("householdId"))
	        sb.append("- Household ID: ").append(structured.get("householdId")).append("\n");

	    return new ExtractedPersonContext(structured, sb.toString().trim());
	}


	/* ================= HELPERS ================= */

	private static void putIfPresent(Map<String, String> map, String key, Object value) {
	    if (value == null) return;
	    String s = value.toString().trim();
	    if (!s.isEmpty()) {
	        map.put(key, s);
	    }
	}

	private static Object firstNonNull(Object a, Object b) {
	    return a != null ? a : b;
	}

	private static String decodeYesNo(Object value) {
	    if (value == null) return null;
	    String s = value.toString().trim().toLowerCase();
	    if (s.equals("yes")) return "yes";
	    if (s.equals("no")) return "no";
	    return s;
	}

	private static String decodeSex(Object value) {
	    if (value == null) return null;
	    String s = value.toString().trim();
	    if (s.equals("1")) return "male";   // adjust if needed
	    if (s.equals("2")) return "female";
	    return s;
	}

	private static String decodeAge(Object value) {
	    if (value == null) return null;

	    int bucket = Integer.parseInt(value.toString());

//	    if (bucket == 16) {
//	        return "80+";
//	    }
//
//	    int lower = bucket * 5;
//	    int upper = lower + 4;

	    return Integer.toString(bucket);
	}


	/* ================= DTO ================= */

	public static class ExtractedPersonContext {
	    final Map<String, String> structured;
	    final String ragText;

	    public ExtractedPersonContext(Map<String, String> structured, String ragText) {
	        this.structured = structured;
	        this.ragText = ragText;
	    }

	    public Map<String, String> getStructured() {
	        return structured;
	    }

	    public String getRagText() {
	        return ragText;
	    }

	    @Override
	    public String toString() {
	        return ragText;
	    }
	}


	@Override
	public void notifyShutdown(ShutdownEvent event) {
		this.vectorDB.clearDynamicDocuments();
		this.vectorDB.clearStaticDocuments();
	}

}
