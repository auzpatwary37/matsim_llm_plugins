package chatrequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import chatcommons.IChatMessage;
import chatresponse.IResponseMessage;

/**
 * LmStudio-compatible request builder.
 * Produces OpenAI-compatible JSON format, excluding tool_choice and streaming.
 */
public class LmStudioChatRequest implements IChatCompletionRequest {

    private static final Gson gson = new Gson();

    @Override
    public String serializeToHttpBody(List<IChatMessage> messages,
                                      List<JsonObject> tools,
                                      String toolChoice,
                                      double temperature,
                                      int maxTokens,
                                      String modelName,
                                      boolean stream) {

        Map<String, Object> payload = new HashMap<>();
        payload.put("model", modelName);
        payload.put("temperature", temperature);
        payload.put("max_tokens", maxTokens);
        payload.put("stream", false); // LM Studio doesn't support streaming properly

        // Convert messages
        payload.put("messages", messages.stream().flatMap(m -> {
            List<Map<String, Object>> outputMessages = new java.util.ArrayList<>();

            // Base message
            Map<String, Object> baseMessage = new HashMap<>();
            baseMessage.put("role", m.getRole().name().toLowerCase());
            baseMessage.put("content", m.getContent());

            // --- Handle tool calls if it's a response ---
            if (m instanceof IResponseMessage response && response.getToolCalls() != null) {
                List<Map<String, Object>> toolCalls = response.getToolCalls().stream().map(tc -> {
                    Map<String, Object> toolCallMap = new HashMap<>();
                    toolCallMap.put("id", tc.getId());
                    toolCallMap.put("type", "function");
                    toolCallMap.put("function", Map.of(
                        "name", tc.getName(),
                        "arguments", tc.getArguments()
                    ));
                    return toolCallMap;
                }).toList();
                baseMessage.put("tool_calls", toolCalls);
            }

            outputMessages.add(baseMessage);

            // --- Handle tool responses if it's a request ---
            if (m instanceof IRequestMessage request && request.getToolResponses() != null) {
                for (var tr : request.getToolResponses()) {
                    Map<String, Object> toolResponse = new HashMap<>();
                    toolResponse.put("role", "tool");
                    toolResponse.put("tool_call_id", tr.getToolCallId());
                    toolResponse.put("name", tr.getName());
                    toolResponse.put("content", tr.getResponseJson());
                    outputMessages.add(toolResponse);
                }
            }

            return outputMessages.stream();
        }).toList());


        // Wrap each tool in { "type": "function", "function": tool }
        if (tools != null && !tools.isEmpty()) {
            List<Map<String, Object>> toolList = tools.stream().map(tool -> {
                Map<String, Object> wrapper = new HashMap<>();
                wrapper.put("type", "function");
                wrapper.put("function", tool);
                return wrapper;
            }).collect(Collectors.toList());

            payload.put("tools", toolList);
        }

        return gson.toJson(payload);
    }
}
