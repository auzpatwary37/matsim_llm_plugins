package chatcommons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;

import com.google.gson.JsonObject;

import chatrequest.IRequestMessage;
import chatrequest.SimpleRequestMessage;
import chatresponse.IChatCompletionResponse;
import chatresponse.IResponseMessage;
import rag.IVectorDB;
import tools.DefaultToolManager;
import tools.DefaultToolResponse;
import tools.ErrorMessages;
import tools.ITool;
import tools.IToolCall;
import tools.IToolResponse;
import tools.SimpleBooleanDTO;
import tools.SimpleStringDTO;
import tools.ToolArgument;
import tools.ToolArgumentDTO;
import tools.VerificationFailedException;
import tools.Implement.ExtractPlanTool;

class ToolCallingAndConversionIT {

    @Test
    void extractPlanTool_parsesJson_andBuildsRealMatSimPlan() {
        DefaultToolManager manager = new DefaultToolManager();
        manager.registerTool(new ExtractPlanTool());
        
        String schema = manager.getAllToolSchemas().toString();

        String argumentsJson = """
        		{
        		  "plan": {
        		    "elements": [
        		      {
        		        "elementType": "activity",
        		        "type": "home",
        		        "facilityId": "1"
        		      },
        		      {
        		        "elementType": "leg",
        		        "mode": "car",
        		        "departureTimeSeconds": 28800,
        		        "travelTimeSeconds": 900,
        		        "route": {
        		          "routeType": "network",
        		          "startLinkId": "1",
        		          "endLinkId": "3",
        		          "linkIds": ["2"]
        		        }
        		      },
        		      {
        		        "elementType": "activity",
        		        "type": "work",
        		        "facilityId": "3"
        		      }
        		    ]
        		  }
        		}
        		""";

        IToolCall call = new SimpleToolCall("call_extract_plan", "extract_plan", argumentsJson);
        IToolResponse<?> response = manager.runToolCall(call, null, null);

        assertNotNull(response);
        assertEquals("extract_plan", response.getName());
        assertFalse(response.isForLLM(), "extract_plan is a dummy/internal tool.");
        assertNotNull(response.getToolCallOutputContainer());
        assertTrue(response.getToolCallOutputContainer() instanceof Plan);

        Plan plan = (Plan) response.getToolCallOutputContainer();
        assertEquals(3, plan.getPlanElements().size());
        assertTrue(plan.getPlanElements().get(0) instanceof Activity);
        assertTrue(plan.getPlanElements().get(1) instanceof Leg);
        assertTrue(plan.getPlanElements().get(2) instanceof Activity);

        Activity first = (Activity) plan.getPlanElements().get(0);
        Leg leg = (Leg) plan.getPlanElements().get(1);
        Activity last = (Activity) plan.getPlanElements().get(2);

        assertEquals("home", first.getType());
        assertEquals("car", leg.getMode());
        assertNotNull(leg.getRoute());
        assertEquals("work", last.getType());
    }

    @Test
    void chatManager_executesToolCall_andFeedsNonDummyToolBackIntoLoop() {
        DefaultToolManager toolManager = new DefaultToolManager();
        toolManager.registerTool(new EchoTool());

        FakeChatCompletionClient fakeClient = new FakeChatCompletionClient(List.of(
                FakeResponses.toolCallingAssistant(
                        new SimpleToolCall("call_echo_1", "echo_tool",
                                "{\"message\":{\"value\":\"matsim live\"},\"shout\":{\"value\":true}}")),
                FakeResponses.assistant("Tool result received and accepted.")));

        DefaultChatManager manager = new DefaultChatManager(
                Id.create("tool-loop-test", IChatManager.class),
                fakeClient,
                toolManager,
                null);
        manager.setSystemMessage("You are a test assistant.");

        Map<String, IToolResponse<?>> results = manager.submit(
                new SimpleRequestMessage(Role.USER, "Use the echo tool.")).toolResponses;

        assertEquals(1, results.size());
        IToolResponse<?> response = results.get("call_echo_1");
        assertNotNull(response);
        assertTrue(response.isForLLM(), "echo_tool should be sent back to the model.");
        assertEquals("MATSIM LIVE", response.getResponseJson());
        assertEquals("MATSIM LIVE", response.getToolCallOutputContainer());

        assertEquals(2, fakeClient.capturedMessages.size(),
                "The manager should have called the client once for the assistant tool call and once after tool execution.");

        IRequestMessage secondCallMessage = fakeClient.capturedMessages.get(1);
        assertEquals(Role.TOOL, secondCallMessage.getRole());
        assertNotNull(secondCallMessage.getToolResponses());
        assertEquals(1, secondCallMessage.getToolResponses().size());
        assertEquals("MATSIM LIVE", secondCallMessage.getToolResponses().get(0).getResponseJson());
    }
    
    private static class EchoTool implements ITool<String> {
        private final Map<String, ToolArgument<?, ? extends ToolArgumentDTO<?>>> arguments = new HashMap<>();
        private Map<String, Object> context = new HashMap<>();

        EchoTool() {
            registerArgument(SimpleStringDTO.forArgument("message"));
            registerArgument(SimpleBooleanDTO.forArgument("shout"));
        }
        

        @Override
        public String getName() {
            return "echo_tool";
        }

        @Override
        public Class<String> getOutputClass() {
            return String.class;
        }

        @Override
        public String getDescription() {
            return "Returns the input string, optionally upper-cased.";
        }

        @Override
        public boolean isDummy() {
            return false;
        }

        @Override
        public Map<String, ToolArgument<?, ? extends ToolArgumentDTO<?>>> getRegisteredArguments() {
            return arguments;
        }

        @Override
        public IToolResponse<String> callTool(String id, Map<String, Object> arguments, IVectorDB vectorDB, Map<String, Object> context) {
            String message = (String) arguments.get("message");
            boolean shout = Boolean.TRUE.equals(arguments.get("shout"));
            String result = shout ? message.toUpperCase() : message;
            return new DefaultToolResponse<>(id, getName(), result, result, false);
        }

        @Override
        public void verifyArguments(Map<String, Object> arguments, Map<String, Object> context, ErrorMessages em)
                throws VerificationFailedException {
            if (arguments.get("message") == null) {
                throw new VerificationFailedException(List.of("message is required"));
            }
        }

        
    }

    private static class FakeChatCompletionClient implements IChatCompletionClient {
        private final List<IChatCompletionResponse> scriptedResponses;
        private int index = 0;
        private final List<IRequestMessage> capturedMessages = new ArrayList<>();

        FakeChatCompletionClient(List<IChatCompletionResponse> scriptedResponses) {
            this.scriptedResponses = scriptedResponses;
        }

        @Override
        public IChatCompletionResponse query(List<IChatMessage> history, IRequestMessage userMessage, List<JsonObject> tools,
                Map<String, Boolean> ifToolDummy) {
            capturedMessages.add(userMessage);
            if (index >= scriptedResponses.size()) {
                throw new AssertionError("No more scripted responses available.");
            }
            return scriptedResponses.get(index++);
        }

        @Override
        public matsimBinding.LLMConfigGroup getLLMConfig() {
            return null;
        }
    }

    private static class SimpleToolCall implements IToolCall {
        private final String id;
        private final String name;
        private final String arguments;

        SimpleToolCall(String id, String name, String arguments) {
            this.id = id;
            this.name = name;
            this.arguments = arguments;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getArguments() {
            return arguments;
        }
    }

    private static class FakeResponses {
        static IChatCompletionResponse assistant(String content) {
            return new SimpleChatCompletionResponse(new SimpleResponseMessage(Role.ASSISTANT, content, List.of()));
        }

        static IChatCompletionResponse toolCallingAssistant(IToolCall toolCall) {
            return new SimpleChatCompletionResponse(new SimpleResponseMessage(Role.ASSISTANT, null, List.of(toolCall)));
        }
    }

    private static class SimpleChatCompletionResponse implements IChatCompletionResponse {
        private final IResponseMessage message;

        SimpleChatCompletionResponse(IResponseMessage message) {
            this.message = message;
        }

        @Override
        public IResponseMessage getMessage() {
            return message;
        }

        @Override
        public List<IToolCall> getToolCalls() {
            return message.getToolCalls();
        }

        @Override
        public chatresponse.IUsage getUsage() {
            return null;
        }

        @Override
        public String getModel() {
            return "fake";
        }

        @Override
        public Map<String, Object> getMetadata() {
            return Map.of();
        }

		@Override
		public void postBuildCleanup() {
			
		}

		@Override
		public String getReasoning() {
			return null;
		}
    }

    private static class SimpleResponseMessage implements IResponseMessage {
        private final Role role;
        private final String content;
        private final List<IToolCall> toolCalls;

        SimpleResponseMessage(Role role, String content, List<IToolCall> toolCalls) {
            this.role = role;
            this.content = content;
            this.toolCalls = toolCalls;
        }

        @Override
        public Role getRole() {
            return role;
        }

        @Override
        public String getContent() {
            return content;
        }

        @Override
        public List<IToolCall> getToolCalls() {
            return toolCalls;
        }

		@Override
		public boolean ifEnableThinking() {
			// TODO Auto-generated method stub
			return false;
		}
    }
}
