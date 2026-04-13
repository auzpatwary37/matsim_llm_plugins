package matsimBinding;

import java.io.BufferedWriter;
import java.io.FileWriter;
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
import org.matsim.core.controler.MatsimServices;
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
import chatcommons.ChatResult;
import chatcommons.ChatStats;
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
	private MatsimServices matsimServices;

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

	private int numberOfLLMAgent = 0;

	protected static final Logger log = Logger.getLogger(LLMReplanningStrategyModule.class);

	private List<Id<Person>> LLMAgentsId = new ArrayList<>();

	private List<Plan> planToReplan = new ArrayList<>();


	private Map<String,Object> contextObject = new HashMap<>();

	private BufferedWriter csvWriter = null;
	
	private BufferedWriter combinedCsvWriter = null;
	
	private int currentIteration = 0;

	@Inject
	LLMReplanningStrategyModule(){

	}

	@Override
	public void prepareReplanning(ReplanningContext replanningContext) {
		setUpLogger(this.matsimServices,replanningContext.getIteration());
		this.currentIteration = replanningContext.getIteration();
		
		
	}

	@Override
	public void handlePlan(Plan plan) {
		this.planToReplan.add(plan);
	}

	private void setUpLogger(MatsimServices services, int iteration) {
	    try {
	        String iterFilePath = services.getControlerIO()
	                .getIterationFilename(iteration, "llm_person_stats.csv");

	        String combinedFilePath = services.getControlerIO()
	                .getOutputFilename("llm_person_stats_combined.csv");

	        this.csvWriter = new BufferedWriter(new FileWriter(iterFilePath));

	        csvWriter.write(String.join(",",
	                "iteration",
	                "personId",
	                "success",
	                "failureType",
	                "llmRounds",
	                "totalToolCalls",
	                "toolParsingFailures",
	                "toolVerificationFailures",
	                "toolExecutionFailures",
	                "noToolCallRetries",
	                "hitMaxIterations",
	                "returnedExtractPlan",
	                "planApplied",
	                "durationMs",
	                "promptTokens",
	                "completionTokens",
	                "reasoningTokens",
	                "totalTokens",
	                "avgTokensPerRound",
	                "tokensPerSecond"
	        ));
	        csvWriter.newLine();

	        boolean writeCombinedHeader = !(new java.io.File(combinedFilePath).exists());

	        this.combinedCsvWriter = new BufferedWriter(new FileWriter(combinedFilePath, true));

	        if (writeCombinedHeader) {
	            combinedCsvWriter.write(String.join(",",
	                    "iteration",
	                    "personId",
	                    "success",
	                    "failureType",
	                    "llmRounds",
	                    "totalToolCalls",
	                    "toolParsingFailures",
	                    "toolVerificationFailures",
	                    "toolExecutionFailures",
	                    "noToolCallRetries",
	                    "hitMaxIterations",
	                    "returnedExtractPlan",
	                    "planApplied",
	                    "durationMs",
	                    "promptTokens",
	                    "completionTokens",
	                    "reasoningTokens",
	                    "totalTokens",
	                    "avgTokensPerRound",
	                    "tokensPerSecond"
	            ));
	            combinedCsvWriter.newLine();
	        }

	    } catch (Exception e) {
	        throw new RuntimeException("Failed to setup CSV logger", e);
	    }
	}

	private void writeRow(ChatStats stats, Person person, Plan outPlan) {
	    try {
	        boolean returnedExtractPlan = (outPlan != null);
	        boolean planApplied = (outPlan != null);

	        boolean success = stats.success && planApplied;
	        String failureType = stats.failureType;

	        if (!returnedExtractPlan && failureType == null) {
	            failureType = "EXTRACT_PLAN_MISSING";
	        }

	        double avgTokensPerRound = stats.llmRounds == 0 ? 0.0
	                : (double) stats.totalTokens / stats.llmRounds;

	        double tokensPerSecond = stats.durationMs == 0 ? 0.0
	                : stats.totalTokens / (stats.durationMs / 1000.0);

	        String row = String.join(",",
	                String.valueOf(this.currentIteration),
	                person.getId().toString(),
	                String.valueOf(success),
	                "\"" + (failureType == null ? "" : failureType) + "\"",
	                String.valueOf(stats.llmRounds),
	                String.valueOf(stats.totalToolCalls),
	                String.valueOf(stats.toolParsingFailures),
	                String.valueOf(stats.toolVerificationFailures),
	                String.valueOf(stats.toolExecutionFailures),
	                String.valueOf(stats.noToolCallRetries),
	                String.valueOf(stats.hitMaxIterations),
	                String.valueOf(returnedExtractPlan),
	                String.valueOf(planApplied),
	                String.valueOf(stats.durationMs),
	                String.valueOf(stats.promptTokens),
	                String.valueOf(stats.completionTokens),
	                String.valueOf(stats.reasoningTokens),
	                String.valueOf(stats.totalTokens),
	                String.valueOf(avgTokensPerRound),
	                String.valueOf(tokensPerSecond)
	        );

	        if (csvWriter != null) {
	            csvWriter.write(row);
	            csvWriter.newLine();
	        }

	        if (combinedCsvWriter != null) {
	            combinedCsvWriter.write(row);
	            combinedCsvWriter.newLine();
	        }

	    } catch (Exception e) {
	        throw new RuntimeException("Failed to write CSV row", e);
	    }
	}

	@Override
	public void finishReplanning() {
		
		if(this.currentIteration<this.llmConfig.getIterationToStartAIActivity()) {
			this.planToReplan.clear();
			return;
		}
		
		List<ChatStats> allstats = new ArrayList<>();
		this.planToReplan.forEach(plan->{
			Person person = plan.getPerson();
			IChatManager chat = this.chatContainer.getChatManagerForPerson(person.getId());
			if(chat==null)return;
			chat.clear();
			String basePlan = PlanDTO.toDTOFromBaseObject().apply(plan).toJsonObject(this.gson).toString();
			System.out.println("Sending querry for person Id "+ person.getId());
			System.out.println(IndividualPrompt.planExtractPrompt+"\n"+basePlan);
			ChatResult result = chat.submit(new SimpleRequestMessage(
					Role.USER,
					IndividualPrompt.chatGPTPlanExtractionPrompt+"\n"+basePlan
					));

			Map<String, IToolResponse<?>> output = result.toolResponses;
			ChatStats stats = result.stats;
			allstats.add(stats);
			Plan outPlan = null;
			for(IToolResponse<?> response: output.values()) {
				if(response.getName().equals("extract_plan")) {
					outPlan = (Plan) response.getToolCallOutputContainer();
				}
			}
			if (outPlan == null) {
				log.warn("No extracted plan returned for person " + person.getId());
				writeRow(stats, person, null);
				return;
			}
			PopulationUtils.copyFromTo(outPlan, plan);
			writeRow(stats,plan.getPerson(),outPlan);
		});
		IterationStats stats = compute(allstats);
		
		this.planToReplan.clear();

		try {
		    if (csvWriter != null) {
		        csvWriter.flush();
		        csvWriter.close();
		    }
		    if (combinedCsvWriter != null) {
		        combinedCsvWriter.flush();
		        combinedCsvWriter.close();
		    }
		} catch (Exception e) {
		    e.printStackTrace();
		}

	}
	
	public static IterationStats compute(List<ChatStats> allStats) {

	    IterationStats out = new IterationStats();

	    for (ChatStats s : allStats) {
	        out.totalAgents++;

	        if (s.success) out.success++;
	        else out.failed++;

	        out.totalToolCalls += s.totalToolCalls;
	        out.parsingFail += s.toolParsingFailures;
	        out.verificationFail += s.toolVerificationFailures;
	        out.executionFail += s.toolExecutionFailures;

	        out.totalDurationMs += s.durationMs;

	        out.promptTokens += s.promptTokens;
	        out.completionTokens += s.completionTokens;
	        out.reasoningTokens += s.reasoningTokens;
	        out.totalTokens += s.totalTokens;
	    }

	    out.avgDurationMs = out.totalAgents == 0 ? 0 :
	            (double) out.totalDurationMs / out.totalAgents;

	    out.avgTokensPerReplan = out.success == 0 ? 0 :
	            (double) out.totalTokens / out.success;

	    out.tokensPerSec = out.totalDurationMs == 0 ? 0 :
	            out.totalTokens / (out.totalDurationMs / 1000.0);

	    return out;
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
				IChatManager chatManager = new DefaultChatManager(Id.create(p.getKey().toString(), IChatManager.class), chatClient, toolManager, vectorDB, this.llmConfig);
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
