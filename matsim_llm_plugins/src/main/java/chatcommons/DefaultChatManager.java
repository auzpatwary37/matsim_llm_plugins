package chatcommons;



import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;

import chatrequest.IRequestMessage;
import chatrequest.SimpleRequestMessage;
import chatresponse.IChatCompletionResponse;
import chatresponse.IResponseMessage;
import rag.IVectorDB;
import rag.IVectorDB.RetrievedDocument;
import tools.IToolManager;
import tools.IToolResponse;

public class DefaultChatManager implements IChatManager {
	
	private IChatMessage systemMessage;
    private final IChatCompletionClient llmClient;
    private final IToolManager toolManager;
    private final IVectorDB vectorDB; // Direct ChromaDB client
    private final Id<IChatManager> id;

    private final List<IChatMessage> history = new ArrayList<>();

    public DefaultChatManager(Id<IChatManager>id , IChatCompletionClient llmClient, IToolManager toolManager, IVectorDB vectorDB) {
        this.id = id;
    	this.llmClient = llmClient;
        this.toolManager = toolManager;
        this.vectorDB = vectorDB;
    }

    @Override
    public Map<String, IToolResponse<?>> submit(IRequestMessage message) {
//        history.add(message);
        Map<String, IToolResponse<?>> toolResponses = new HashMap<>();

        while (true) {
            IResponseMessage response = submitInternal(message);
            history.add(response);

            if (response.getToolCalls() == null || response.getToolCalls().isEmpty()) {
                break;
            }

            List<IToolResponse<?>> newResponses = new ArrayList<>();
            boolean ifNonDummy = false;
            for (var call : response.getToolCalls()) {
                try {
                    IToolResponse<?> toolResult = toolManager.runToolCall(call, this.vectorDB);
                    newResponses.add(toolResult);
                    toolResponses.put(call.getId(), toolResult);
                    if(toolResult.isForLLM()) {
                    	ifNonDummy = true;
                    }
                } catch (Exception ex) {
//                    IToolResponse<?> errorResponse = toolManager.handleError(call, ex); the error is handled in tool call internally 
                	// a tool response is gnenerated anyway with the error message instead of the actual response. 
//                    newResponses.add(errorResponse);
//                    toolResponses.put(call.getId(), errorResponse);
                }
            }

            IRequestMessage toolMessage = new SimpleRequestMessage(Role.TOOL,"",newResponses);
//            history.add(toolMessage);
            message = toolMessage;
            if(!ifNonDummy) {
            	break;
            }
        }

        return toolResponses;
    }


    @Override
    public IResponseMessage submitInternal(IRequestMessage message) {
        IRequestMessage enrichedMessage = message;

        // Inject context only for the initial USER message
        if (vectorDB != null && message.getRole() == Role.USER && message.getContent() != null && !message.getContent().isBlank()) {
            List<RetrievedDocument> docs = vectorDB.query(message.getContent(), 3);

            if (!docs.isEmpty()) {
                StringBuilder contextBlock = new StringBuilder("[Retrieved Context]\n");
                for (RetrievedDocument doc : docs) {
                    contextBlock.append("- ").append(doc.content()).append("\n");
                }

                contextBlock.append("\n[User Message]\n").append(message.getContent());

                // Wrap new content in a new SimpleRequestMessage
                enrichedMessage = new SimpleRequestMessage(Role.USER, contextBlock.toString());
            }
        }

        IChatCompletionResponse completion = llmClient.query(history, enrichedMessage, toolManager.getAllToolSchemas(), toolManager.getIfToolDummy());
        return completion.getMessage();
    }



    @Override
    public void append(IChatMessage... messages) {
        history.addAll(Arrays.asList(messages));
    }

    public List<IChatMessage> getHistory() {
        return history;
    }

    public IVectorDB getChromaClient() {
        return vectorDB;
    }



	@Override
	public String getSystemMessage() {
		return this.systemMessage.getContent();
	}

	@Override
	public void setSystemMessage(String systemMessage) {
		this.systemMessage = new SimpleRequestMessage(Role.SYSTEM,systemMessage);
		this.history.add(this.systemMessage);
	}

	@Override
	public IChatMessage getLastMessage() {
		
		return this.history.get(this.history.size()-1);
	}

	@Override
	public void clear() {
		this.history.clear();
		this.history.add(systemMessage);
	}

	@Override
	public Id<IChatManager> getId() {
		// TODO Auto-generated method stub
		return this.id;
	}
}

