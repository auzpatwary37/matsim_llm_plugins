package matsimBinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.router.TripRouter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import com.google.inject.Provider;

import chatcommons.ChatManagerContainer;
import chatcommons.DefaultChatManager;
import chatcommons.IChatCompletionClient;
import chatcommons.IChatManager;
import chatcommons.Role;
import chatrequest.SimpleRequestMessage;
import matsimdtobjects.PlanDTO;
import prompts.IndividualPrompt;
import rag.IVectorDB;
import tools.IToolManager;
import tools.IToolResponse;


public class LLMReplanningStrategyModule implements StartupListener, PlanStrategyModule{
	
	public static final String StrategyName = "LLMPlanner";
	
	@Inject
	LLMConfigGroup llmConfig;
	
	@Inject
	Scenario scenario;
	
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
	
	private Gson gson = new GsonBuilder().setPrettyPrinting().create();
	
	private int numberOfLLMAgent = 100;
	
	protected static final Logger log = Logger.getLogger(LLMReplanningStrategyModule.class);

	private List<Id<Person>> LLMAgentsId = new ArrayList<>();
	
	private List<Plan> planToReplan = new ArrayList<>();

	
	
	private Map<String,Object> contextObject = new HashMap<>();
	
	@Inject
	LLMReplanningStrategyModule(){
		
	}

	@Override
	public void prepareReplanning(ReplanningContext replanningContext) {
		
	}

	@Override
	public void handlePlan(Plan plan) {
		this.planToReplan.add(plan);
	}

	@Override
	public void finishReplanning() {
		
		this.planToReplan.forEach(plan->{
			Person person = plan.getPerson();
			IChatManager chat = this.chatContainer.getChatManagerForPerson(person.getId());
			if(chat==null)return;
			String basePlan = PlanDTO.toDTOFromBaseObject().apply(plan).toJsonObject(this.gson).toString();
			System.out.println("Sending querry for person Id "+ person.getId());
			System.out.println(IndividualPrompt.planExtractPrompt+"\n"+basePlan);
			Map<String, IToolResponse<?>> output = chat.submit(new SimpleRequestMessage(
			        Role.USER,
			        IndividualPrompt.chatGPTPlanExtractionPrompt+"\n"+basePlan
			    ));
			Plan outPlan = null;
			for(IToolResponse<?> response: output.values()) {
				if(response.getName().equals("extract_plan")) {
					outPlan = (Plan) response.getToolCallOutputContainer();
				}
			}
			if (outPlan == null) {
	            log.warn("No extracted plan returned for person " + person.getId());
	            return;
	        }
			PopulationUtils.copyFromTo(outPlan, plan);
		});
		this.planToReplan.clear();
		
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		this.contextObject.put("tripRoutersProvider", this.tripRouterProvider);
		this.contextObject.put("activityFacilities", scenario.getActivityFacilities());
		int totalRealPerson = 0;
		for(Entry<Id<Person>, ? extends Person> p:this.scenario.getPopulation().getPersons().entrySet()){
			if(p.getValue().getSelectedPlan().getPlanElements().size()<=3) {
				continue;
			}
			totalRealPerson++;
		}
		double prob = ((double)numberOfLLMAgent)/totalRealPerson;
		int totalLLMAgent = 0;
		for(Entry<Id<Person>, ? extends Person> p:this.scenario.getPopulation().getPersons().entrySet()){
			if(p.getValue().getSelectedPlan().getPlanElements().size()<=3) {
				continue;
			}
			if(MatsimRandom.getLocalInstance().nextDouble()<prob) {
				this.LLMAgentsId.add(p.getKey());
				String context = LLMControllerListener.extract(p.getValue()).ragText;
				Map<String,String> metaData = new HashMap<>();
				metaData.put("personId", p.getKey().toString());
				metaData.put("type", "attribute");
				//this.vectorDB.insert(context,metaData);//inserted in agent experience handler
				IChatManager chatManager = new DefaultChatManager(Id.create(p.getKey().toString(), IChatManager.class), chatClient, toolManager, vectorDB);
				chatManager.setSystemMessage(IndividualPrompt.chatGPTSystemPrompt+ " You are person "+ p.getKey().toString());
				chatManager.setPersonId(p.getKey());
				p.getValue().getAttributes().putAttribute("isAI", true);
				chatManager.setContextObject(new HashMap<>(this.contextObject));
				chatManager.getContextObject().put("person",p.getValue());
				this.chatContainer.add(chatManager);
				totalLLMAgent++;
			}
		}
		System.out.println("Total LLM agent = "+ totalLLMAgent);
	}

}
