package matsimBinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.population.PopulationUtils;
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
import matsimdtobjects.PlanDTO;
import prompts.IndividualPrompt;
import rag.IVectorDB;
import tools.IToolManager;
import tools.IToolResponse;

public class LLMWithinDayListener implements MobsimInitializedListener, StartupListener {

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
	
	@Inject
	private org.matsim.core.controler.MatsimServices services;
	
	private Gson gson = new Gson();
	
	private int numberOfLLMAgent = 10;
	
	protected static final Logger log = Logger.getLogger(LLMWithinDayListener.class);

	private List<Id<Person>> LLMAgentsId = new ArrayList<>();
	


	
	
	private Map<String,Object> contextObject = new HashMap<>();
	
	
    private QSim qsim;

    @Inject
    public LLMWithinDayListener() {
    	
    	
    }

    @Override
    public void notifyMobsimInitialized(MobsimInitializedEvent e) {
    	
    	
        if (!(e.getQueueSimulation() instanceof QSim)) {
            throw new IllegalStateException(
                    LLMWithinDayListener.class.toString() + " only works with a mobsim of type " + QSim.class);
        }

        this.qsim = (QSim) e.getQueueSimulation();
        

        this.chatContainer.getAll().entrySet().forEach(chatEntry -> {
            IChatManager chatManager = chatEntry.getValue();

            Person person = (Person) chatManager.getContextObject().get("person");
            if (person == null) {
                return;
            }

            MobsimAgent mobsimAgent = this.qsim.getAgents().get(person.getId());
            if (mobsimAgent == null) {
                return;
            }

            Plan mutablePlan = WithinDayAgentUtils.getModifiablePlan(mobsimAgent);

            String basePlan = PlanDTO.toDTOFromBaseObject()
                    .apply(mutablePlan)
                    .toJsonObject(this.gson)
                    .toString();

            System.out.println("Sending within-day query for person Id " + person.getId());

            ChatResult result = chatManager.submit(
                    new SimpleRequestMessage(
                            Role.USER,
                            IndividualPrompt.planExtractPrompt + "\n" + basePlan
                    ),
                    null
            );
             
            Map<String, IToolResponse<?>> toolResponses = result.toolResponses;

            Plan extractedPlan = extractReturnedPlan(toolResponses);
            if (extractedPlan == null) {
                System.out.println("No extracted plan returned for person Id " + person.getId());
                return;
            }

            copyPlanIntoMutablePlan(extractedPlan, mutablePlan);

            System.out.println("Mutable plan updated for person Id " + person.getId());
        });
    }

    private Plan extractReturnedPlan(Map<String, IToolResponse<?>> toolResponses) {
        if (toolResponses == null || toolResponses.isEmpty()) {
            return null;
        }

        for (IToolResponse<?> response : toolResponses.values()) {
            if (response == null) {
                continue;
            }

            if (!"extract_plan".equals(response.getName())) {
                continue;
            }

            Object output = response.getToolCallOutputContainer();
            if (output instanceof Plan plan) {
                return plan;
            }
        }

        return null;
    }

    private void copyPlanIntoMutablePlan(Plan source, Plan target) {
        PopulationUtils.copyFromTo(source, target);
    }

	@Override
	public void notifyStartup(StartupEvent event) {
		
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
				//this.vectorDB.insert(context,metaData);//handled in Agent Experience handler
				IChatManager chatManager = new DefaultChatManager(Id.create(p.getKey().toString(), IChatManager.class), chatClient, toolManager, vectorDB, this.llmConfig);
				chatManager.setSystemMessage(IndividualPrompt.systemPrompt + " You are person "+ p.getKey().toString());
				chatManager.setPersonId(p.getKey());
				chatManager.setContextObject(new HashMap<>(this.contextObject));
				chatManager.getContextObject().put("person",p.getValue());
				this.chatContainer.add(chatManager);
			}
		});
	}
	
}
