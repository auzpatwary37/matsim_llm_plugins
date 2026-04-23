package org.matsim.contrib.llm.tools;


import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.matsim.contrib.llm.rag.IVectorDB;

/**
 * A functional tool that can be called by a language model.
 * Each tool should implement this interface and be registered with a ToolManager.
 * the type argument T is the class the object class that the tool call sends back as part of the ResponseMessage container for MATSim Consumption. 
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
        JsonArray required = new JsonArray();

        for (Map.Entry<String, ToolArgument<?, ? extends ToolArgumentDTO<?>>> entry : getRegisteredArguments().entrySet()) {
            String argName = entry.getKey();
            properties.add(argName, entry.getValue().getDTOSchema());
            required.add(argName);
        }

        parameters.add("properties", properties);
        parameters.add("required", required);

        schema.add("parameters", parameters);
        return schema;
    }



    /**
     * Parses incoming tool_call arguments into DTOs and then base objects, then delegates to callTool.
     * Allows the tool to add document to the vectorDB. 
     */
    default IToolResponse<T> call(String argumentsJson, String toolCallId, IVectorDB vectorDB, Map<String, Object> context) {
        Gson gson = new Gson();
        Map<String, Object> baseObjects = new HashMap<>();
        ErrorMessages em = new ErrorMessages();
        try {
            JsonObject parsed = gson.fromJson(argumentsJson, JsonObject.class);

            for (Map.Entry<String, ToolArgument<?, ? extends ToolArgumentDTO<?>>> entry : getRegisteredArguments().entrySet()) {
                String key = entry.getKey();
                if (parsed.has(key)) {
                    ToolArgument<?, ? extends ToolArgumentDTO<?>> arg = entry.getValue();
                    Object base = arg.fromJson(parsed.get(key).toString(), gson, em, context);
                    baseObjects.put(key, base);
                }
            }
            
            this.verifyArguments(baseObjects, context,em);
            return callTool(toolCallId, baseObjects, vectorDB, context);
        } catch (Exception ex) {
            return handleErrorMessage(toolCallId, ex, em);
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
    IToolResponse<T> callTool(String id, Map<String, Object> arguments, IVectorDB vectorDB,  Map<String, Object> context);
    
    
    /**
     * Handles deserialization or execution errors by producing a structured response
     * that can be sent back to the LLM. Even for dummy tools, this allows LLM recovery. Make sure the forLLM flag is turned on inside the IResponseMessage for LLM feedback
     *
     * @param callId The original tool call ID
     * @param ex The exception that occurred during parsing or execution
     * @return A tool response suitable for LLM feedback
     */
    default IToolResponse<T> handleErrorMessage(String callId, Exception ex, ErrorMessages em) {
        JsonObject errorJson = new JsonObject();
        errorJson.addProperty("status", "ERROR");
        errorJson.addProperty("message", "Tool invocation failed: " + ex.getMessage() + " because: " + em.getCombinedErrorMessages());

        return new DefaultToolResponse<T>(
            callId,
            getName(),
            errorJson.toString(), // for logging/LLM tracing if needed
            null,
            false // always sent to LLM
        );
    }
    
    
    public void verifyArguments(Map<String,Object> arguments, Map<String,Object> context, ErrorMessages em) throws VerificationFailedException;


    

    
}

