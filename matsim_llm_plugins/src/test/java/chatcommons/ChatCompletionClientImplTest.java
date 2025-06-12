package chatcommons;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;

import apikeys.APIKeys;
import chatrequest.IRequestMessage;
import chatrequest.SimpleRequestMessage;
import chatresponse.IChatCompletionResponse;
import matsimBinding.LLMConfigGroup;
import rag.IVectorDB;
import rag.VectorDBImplement;
import tools.DefaultToolManager;
import tools.DefaultToolResponse;
import tools.ITool;
import tools.IToolManager;
import tools.IToolResponse;
import tools.SimpleBooleanDTO;
import tools.SimpleDoubleDTO;
import tools.SimpleStringDTO;
import tools.ToolArgument;
import tools.ToolArgumentDTO;

class ChatCompletionClientImplTest {
	
	LLMConfigGroup config;
	IChatManager chatManager;
	IVectorDB vectorDB;
	IToolManager tools; 
	IChatCompletionClient client;

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
	}

	@BeforeEach
	void setUp() throws Exception {
		
		
	}

	
	public void setupLMStudio() {
		LLMConfigGroup llmConfig = new LLMConfigGroup();
    	llmConfig.setBackend(LLMConfigGroup.BackendType.LM_STUDIO);
    	llmConfig.setAuthorization("lm-studio");
//    	llmConfig.setProject(APIKeys.PROJECT_ID);
//    	llmConfig.setOrganization(APIKeys.ORGANIZATION_ID);
    	llmConfig.setModelName("qwen/qwen3-14b");
    	llmConfig.setLlmHost("localhost");
    	llmConfig.setLlmPort(1234);
    	llmConfig.setEmbeddingModelName("text-embedding-nomic-embed-text-v1.5@q4_k_m:2");
    	llmConfig.setEnableContextRetrieval(true);
    	llmConfig.setUseHttps(false);
    	llmConfig.setLlmPath("/v1/chat/completions");
    	llmConfig.setVectorDBSourceFile("src/test/resources/Chromadb/chromaBase.txt");
    	llmConfig.setVectorDbHost("localhost");
    	llmConfig.setVectorDbPort(8000);
    	llmConfig.setEmbeddingPath("/v1/embeddings");
    	llmConfig.setVectorDbCollectionName("newLmStudioCollections");
    	
    	
    	this.config = llmConfig;
    
    	
    	IChatCompletionClient chatClient = new ChatCompletionClientImpl(llmConfig);
    	this.client = chatClient;
    	IRequestMessage systemMessage = new SimpleRequestMessage(Role.SYSTEM, "You are a helpful AI assistant integrated inside MATSim. Say Hi to MATSim if you are live!!! There is not human involved and you are talking directly to MATSim. So always respond with tool calls. \no_think");
    	ArrayList<IChatMessage> history = new ArrayList<IChatMessage>();
    	
    	
    	history.add(systemMessage);
    	IRequestMessage userMessage = new SimpleRequestMessage(Role.USER,"Hello from MATSim! This is a test handshake!!! Acknowledge");
    	IChatCompletionResponse response = chatClient.query(history, userMessage, null, new HashMap<>());
    	System.out.println(response.getMessage().getContent());
    	
    	
    	setupToolmanager(config);
    	
    	
    	this.chatManager = new DefaultChatManager(Id.create("openai",IChatManager.class), this.client, tools, vectorDB);
    	
    	this.chatManager.setSystemMessage(systemMessage.getContent());
    	
    	

    	
		
		
	}
	public void setupOpenAi() {
		LLMConfigGroup llmConfig = new LLMConfigGroup();
    	llmConfig.setBackend(LLMConfigGroup.BackendType.OPENAI);
    	llmConfig.setAuthorization(APIKeys.GPT_KEY);
    	llmConfig.setProject(APIKeys.PROJECT_ID);
    	llmConfig.setOrganization(APIKeys.ORGANIZATION_ID);
    	llmConfig.setModelName("gpt-4-turbo");
    	llmConfig.setLlmHost("api.openai.com");
    	llmConfig.setUseHttps(true);
    	llmConfig.setLlmPath("/v1/chat/completions");
    	llmConfig.setVectorDBSourceFile("src/test/resources/Chromadb/chromaBase.txt");
    	llmConfig.setVectorDbHost("localhost");
    	llmConfig.setVectorDbPort(8000);
    	
    	this.config = llmConfig;
    
    	
    	IChatCompletionClient chatClient = new ChatCompletionClientImpl(llmConfig);
    	this.client = chatClient;
    	IRequestMessage systemMessage = new SimpleRequestMessage(Role.SYSTEM, "You are a helpful AI assistant integrated inside MATSim. Say Hi to MATSim if you are live!!! There is not human involved and you are talking directly to MATSim. So always respond with tool calls. \no_think");
    	ArrayList<IChatMessage> history = new ArrayList<IChatMessage>();
    	
    	
    	history.add(systemMessage);
    	IRequestMessage userMessage = new SimpleRequestMessage(Role.USER,"Hello from MATSim! This is a test handshake!!! Acknowledge");
    	IChatCompletionResponse response = chatClient.query(history, userMessage, null, new HashMap<>());
    	System.out.println(response.getMessage().getContent());
    	
    	
    	setupToolmanager(config);
    	
    	
    	this.chatManager = new DefaultChatManager(Id.create("openai",IChatManager.class), this.client, tools, vectorDB);
    	
    	this.chatManager.setSystemMessage(systemMessage.getContent());
    	
    	

    	
    	
	}
	public void setupOllama() {
		//for now will fail
	};
	
	public void setupVectorDB(LLMConfigGroup config) {
		this.vectorDB =  new VectorDBImplement(this.config);
	};
	public void setupToolmanager(LLMConfigGroup config) {
		this.tools = new DefaultToolManager();
    	
    	ITool<String> tool_1 = new EchoTool();
    	ITool<Double> tool_2 = new ArithmeticTool();
    	
    	this.tools.registerTool(tool_1);
    	this.tools.registerTool(tool_2);
	};
	
	@Test
	void testQuery() {
		this.setupLMStudio();
		//this.setupOpenAi();
		this.setupVectorDB(config);
		Map<String, IToolResponse<?>> toolResponses = this.chatManager.submit(new SimpleRequestMessage(Role.USER,"Try out the tools we have. Give a shout if the result is correct. If not give a shout that it is wrong!!!!"));
		toolResponses.entrySet().forEach(tr->{
			System.out.println("for tool "+tr.getValue().getName()+" and id "+ tr.getKey()+" the response is "+tr.getValue().getResponseJson());
		});
	}
	


	


}

class EchoTool implements ITool<String> {

    private Map<String, ToolArgument<?, ? extends ToolArgumentDTO<?>>> arguments = new HashMap<>();

    public EchoTool() {
        // Register arguments
        registerArgument(SimpleStringDTO.forArgument("message"));
        registerArgument(SimpleBooleanDTO.forArgument("shout"));
    }


    @Override
    public String getName() {
        return "echo_tool";
    }

    @Override
    public String getDescription() {
        return "A dummy tool that echoes the input message. Optionally transforms it to uppercase if \'shout\' is true.";
    }

    @Override
    public boolean isDummy() {
        return true;
    }

    @Override
    public Map<String, ToolArgument<?, ? extends ToolArgumentDTO<?>>> getRegisteredArguments() {
        return this.arguments;
    }


	@Override
	public Class<String> getOutputClass() {
		return String.class;
	}


	@Override
	public IToolResponse<String> callTool(String id, Map<String, Object> arguments, IVectorDB vectorDB) {
		
		String msg = (String) arguments.getOrDefault("message", "");
        boolean shout = Boolean.TRUE.equals(arguments.getOrDefault("shout", ""));

        String result = shout ? msg.toUpperCase() : msg;
		
		
		return new IToolResponse<String>() {

			@Override
			public String getToolCallId() {
				
				return id;
			}

			@Override
			public String getName() {
				
				return EchoTool.this.getName();
			}

			@Override
			public String getResponseJson() {
				
				return result;
			}

			@Override
			public String getToolCallOutputContainer() {
				
				return result;
			}

			@Override
			public boolean isForLLM() {
				
				return false;
			}
			
		};
	}

}


class ArithmeticTool implements ITool<Double> {

    private final Map<String, ToolArgument<?, ? extends ToolArgumentDTO<?>>> arguments = new HashMap<>();

    public ArithmeticTool() {
        registerArgument(SimpleDoubleDTO.forArgument("a"));
        registerArgument(SimpleDoubleDTO.forArgument("b"));
        registerArgument(SimpleDoubleDTO.forArgument("scale")); // optional multiplier
    }

    @Override
    public String getName() {
        return "arithmetic_tool";
    }

    @Override
    public String getDescription() {
        return "Adds two numbers (a + b) and optionally multiplies the result by 'scale' (default: 1.0).";
    }

    @Override
    public boolean isDummy() {
        return false;  // this is a real tool
    }

    @Override
    public Map<String, ToolArgument<?, ? extends ToolArgumentDTO<?>>> getRegisteredArguments() {
        return this.arguments;
    }

    @Override
    public Class<Double> getOutputClass() {
        return Double.class;
    }

    @Override
    public IToolResponse<Double> callTool(String id, Map<String, Object> args, IVectorDB vectorDB) {
        double a = toDouble(args.get("a"));
        double b = toDouble(args.get("b"));
        double scale = args.containsKey("scale") ? toDouble(args.get("scale")) : 1.0;

        double result = (a + b) * scale;

        return new DefaultToolResponse<>(id, getName(), Double.toString(result), result, false); // isForLLM = true
    }

    private double toDouble(Object obj) {
        if (obj instanceof Number) return ((Number) obj).doubleValue();
        if (obj instanceof String) return Double.parseDouble((String) obj);
        throw new IllegalArgumentException("Expected numeric input but got: " + obj);
    }

}

