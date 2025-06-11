package tools;


import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import rag.IVectorDB;

/**
 * A functional tool that can be called by a language model.
 * Each tool should implement this interface and be registered with a ToolManager.
 */
public interface ITool<T>{
	
	/**
     * Register a ToolArgument (name must be unique within this tool).
     */
	default ITool<T> registerArgument(ToolArgument<?, ? extends ToolArgumentDTO<?>> argument) {
    	getRegisteredArguments().put(argument.getName(), argument);
        return this;
    }
	
    /**
     * Gets the unique name of this tool (e.g., "get_fastest_route").
     * This must match the name in the tool_specification file or tool_call.function.name.
     */
    String getName();

    /**
     * 
     * @return the class type of T
     */
    Class<T> getOutputClass(); 
    /**
     * A human-readable description of the tool's purpose.
     */
    String getDescription();

    /**
     * Provides the OpenAI-compatible tool specification schema.
     * This is used to serialize into the "tools" section of the API payload.
     * You may return a hardcoded JSON object or generate it via reflection.
     */
    
    default JsonObject getJsonSchema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("name", getName());
        schema.addProperty("description", getDescription());

        JsonObject parameters = new JsonObject();
        parameters.addProperty("type", "object");

        JsonObject properties = new JsonObject();
        for (Map.Entry<String, ToolArgument<?, ? extends ToolArgumentDTO<?>>> entry : getRegisteredArguments().entrySet()) {
            properties.add(entry.getKey(), entry.getValue().getDTOSchema());
        }

        parameters.add("properties", properties);
        schema.add("parameters", parameters);

        return schema;
    }


    /**
     * Parses incoming tool_call arguments into DTOs and then base objects, then delegates to callTool.
     * Allows the tool to add document to the vectorDB. 
     */
    default IToolResponse<T> call(String argumentsJson, String toolCallId, IVectorDB vectorDB) {
        Gson gson = new Gson();
        Map<String, Object> baseObjects = new HashMap<>();

        try {
            JsonObject parsed = gson.fromJson(argumentsJson, JsonObject.class);

            for (Map.Entry<String, ToolArgument<?, ? extends ToolArgumentDTO<?>>> entry : getRegisteredArguments().entrySet()) {
                String key = entry.getKey();
                if (parsed.has(key)) {
                    ToolArgument<?, ? extends ToolArgumentDTO<?>> arg = entry.getValue();
                    Object base = arg.fromJson(parsed.get(key).toString(), gson);
                    baseObjects.put(key, base);
                }
            }
            

            return callTool(toolCallId, baseObjects, vectorDB);
        } catch (Exception ex) {
            return handleErrorMessage(toolCallId, ex);
        }
    }

    
    /**
     * 
     * @returns if the tool is a dummy tool without any return expected by the LLM. 
     */
    boolean isDummy();

    /**
     * 
     * @return all registered tools 
     */
    Map<String, ToolArgument<?, ? extends ToolArgumentDTO<?>>> getRegisteredArguments();

    
    /**
     * Executes the tool using fully parsed base-class arguments.
     */
    IToolResponse<T> callTool(String id, Map<String, Object> arguments, IVectorDB vectorDB);
    
    
    /**
     * Handles deserialization or execution errors by producing a structured response
     * that can be sent back to the LLM. Even for dummy tools, this allows LLM recovery. Make sure the forLLM flag is turned on inside the IResponseMessage for LLM feedback
     *
     * @param callId The original tool call ID
     * @param ex The exception that occurred during parsing or execution
     * @return A tool response suitable for LLM feedback
     */
    default IToolResponse<T> handleErrorMessage(String callId, Exception ex) {
        JsonObject errorJson = new JsonObject();
        errorJson.addProperty("status", "ERROR");
        errorJson.addProperty("message", "Tool invocation failed: " + ex.getMessage());

        return new DefaultToolResponse<T>(
            callId,
            getName(),
            errorJson.toString(), // for logging/LLM tracing if needed
            null,
            true // always sent to LLM
        );
    }



    
    
}

