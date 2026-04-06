package matsimBinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.gbl.MatsimRandom;

import com.google.inject.Inject;

import chatcommons.ChatManagerContainer;
import chatcommons.DefaultChatManager;
import chatcommons.IChatCompletionClient;
import chatcommons.IChatManager;
import rag.IVectorDB;
import tools.IToolManager;

public class LLMControllerListener implements StartupListener, IterationEndsListener, IterationStartsListener,BeforeMobsimListener{

	@Inject
	private LLMConfigGroup llmConfig;
	
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
	
	private int numberOfLLMAgent = 10;

	
	
	private Map<String,Object> contextObject = new HashMap<>();
	
	
	@Inject
	public LLMControllerListener() {
		
	}
	
	private List<Id<Person>> LLMAgentsId = new ArrayList<>();
	
	
	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		// TODO Auto-generated method stub
		
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
		double prob = ((double)numberOfLLMAgent)/this.scenario.getPopulation().getPersons().size();
		this.scenario.getPopulation().getPersons().entrySet().forEach(p->{
			if(MatsimRandom.getLocalInstance().nextDouble()<prob) {
				this.LLMAgentsId.add(p.getKey());
				String context = LLMControllerListener.extract(p.getValue()).ragText;
				Map<String,String> metaData = new HashMap<>();
				metaData.put("personId", p.getKey().toString());
				metaData.put("type", "attribute");
				this.vectorDB.insert(context,metaData);
				IChatManager chatManager = new DefaultChatManager(Id.create(p.getKey().toString(), IChatManager.class), chatClient, toolManager, vectorDB);
				chatManager.setSystemMessage("You are an AI agent simulated inside MATSim. There is no human present and your handshake is purely automated inside MATSim runtime. So always answer using the available tools.");
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

	    if (bucket == 16) {
	        return "80+";
	    }

	    int lower = bucket * 5;
	    int upper = lower + 4;

	    return lower + "-" + upper;
	}


	/* ================= DTO ================= */

	public static class ExtractedPersonContext {
	    private final Map<String, String> structured;
	    private final String ragText;

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

}
