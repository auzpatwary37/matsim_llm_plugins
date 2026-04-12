package chatcommons;



import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

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
    private Map<String,Object> context = new HashMap<>();
    private Id<Person> personId;
    private boolean retrievalFlag = true;

    private final List<IChatMessage> history = new ArrayList<>();

    public DefaultChatManager(Id<IChatManager>id , IChatCompletionClient llmClient, IToolManager toolManager, IVectorDB vectorDB) {
        this.id = id;
    	this.llmClient = llmClient;
        this.toolManager = toolManager;
        this.vectorDB = vectorDB;
    }
    
    private Map<String, String> buildMetadataFilter() {
        Map<String, String> filter = new HashMap<>();

        if (this.personId != null) {
            filter.put("personId", this.personId.toString());
        }

//        // Optional: add more filters if you stored them
//        if (this.context != null) {
//            Object iteration = context.get("iteration");
//            if (iteration != null) {
//                filter.put("iteration", iteration.toString());
//            }
//        }

        return filter;
    }

    @Override
    public Map<String, IToolResponse<?>> submit(IRequestMessage message) {
//        history.add(message);
        Map<String, IToolResponse<?>> toolResponses = new HashMap<>();

        int noToolCallRetryCount = 0;
        final int maxNoToolCallRetries = 3;
        
        final int maxIteration = 12;
        int i = 0;
        
        while (true) {
            IResponseMessage response = submitInternal(message);
            history.add(response);

            if (response.getToolCalls() == null || response.getToolCalls().isEmpty()) {
            	noToolCallRetryCount++;

                if (noToolCallRetryCount >= maxNoToolCallRetries) {
                    System.out.println("Warning: model failed to produce a valid tool call after "
                            + maxNoToolCallRetries + " attempts.");
                    retrievalFlag = true;
                    break;
                }
                IRequestMessage noToolMessage = new SimpleRequestMessage(Role.USER,"Your previous response was invalid because it did not contain any valid tool call. "
                		+ "You must respond with a valid tool call only.",null,false);
                message = noToolMessage;
                retrievalFlag = false;
            }else {
            	noToolCallRetryCount = 0;
            	retrievalFlag = true;
	            List<IToolResponse<?>> newResponses = new ArrayList<>();
	            boolean ifNonDummy = false;
	            for (var call : response.getToolCalls()) {
	                
	                IToolResponse<?> toolResult = toolManager.runToolCall(call, this.vectorDB, this.context);
	                newResponses.add(toolResult);
	                toolResponses.put(call.getId(), toolResult);
	                if(toolResult.isForLLM()) {
	                	ifNonDummy = true;
	                }
	                
	            }
	
	            IRequestMessage toolMessage = new SimpleRequestMessage(Role.TOOL,"",newResponses,false);
	//            history.add(toolMessage);
	            message = toolMessage;
	            
	            if(!ifNonDummy) {
	            	break;
	            }
            }
            i++;
            if(i>maxIteration)break;
        }

        return toolResponses;
    }


    @Override
    public IResponseMessage submitInternal(IRequestMessage message) {
        IRequestMessage enrichedMessage = message;
        Map<String, String> metaDataFilter = this.buildMetadataFilter();
        // Inject context only for the initial USER message
        if (this.retrievalFlag && vectorDB != null && message.getRole() == Role.USER && message.getContent() != null && !message.getContent().isBlank()) {
            List<RetrievedDocument> docs = vectorDB.query(message.getContent(), 3, metaDataFilter);

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

	@Override
	public Map<String, Object> getContextObject() {
		// TODO Auto-generated method stub
		return this.context;
	}

	@Override
	public void setContextObject(Map<String, Object> context) {
		this.context = context;
	}

	@Override
	public Id<Person> getPersonId() {
		// TODO Auto-generated method stub
		return this.personId;
	}

	@Override
	public void setPersonId(Id<Person> person) {
		this.personId = person;
	}
}

