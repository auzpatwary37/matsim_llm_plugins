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
 * OpenAI-compatible request builder.
 * Supports tool_choice, streaming, and tool call serialization.
 */
public class OpenAiChatRequest implements IChatCompletionRequest {

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
        payload.put("stream", stream);

        // === Serialize messages with tool calls and responses ===
        payload.put("messages", messages.stream().flatMap(m -> {
            List<Map<String, Object>> output = new java.util.ArrayList<>();

            // Base message
            Map<String, Object> mMap = new HashMap<>();
            mMap.put("role", m.getRole().name().toLowerCase());
            mMap.put("content", m.getContent());

            // --- Handle assistant tool calls ---
            if (m instanceof IResponseMessage response && response.getToolCalls() != null) {
                List<Map<String, Object>> toolCalls = response.getToolCalls().stream().map(tc -> {
                    Map<String, Object> toolCall = new HashMap<>();
                    toolCall.put("id", tc.getId());
                    toolCall.put("type", "function");
                    toolCall.put("function", Map.of(
                        "name", tc.getName(),
                        "arguments", tc.getArguments()
                    ));
                    return toolCall;
                }).toList();
                mMap.put("tool_calls", toolCalls);
            }

            output.add(mMap);

            // --- Handle user-side tool responses ---
            if (m instanceof IRequestMessage request && request.getToolResponses() != null) {
                request.getToolResponses().forEach(tr -> {
                    Map<String, Object> toolResponse = new HashMap<>();
                    toolResponse.put("role", "tool");
                    toolResponse.put("tool_call_id", tr.getToolCallId());
                    toolResponse.put("name", tr.getName());
                    toolResponse.put("content", tr.getResponseJson());
                    output.add(toolResponse);
                });
            }

            return output.stream();
        }).toList());

        // === Tools ===
        if (tools != null && !tools.isEmpty()) {
            List<Map<String, Object>> wrappedTools = tools.stream().map(tool -> {
                Map<String, Object> wrapper = new HashMap<>();
                wrapper.put("type", "function");
                wrapper.put("function", tool);
                return wrapper;
            }).collect(Collectors.toList());

            payload.put("tools", wrappedTools);
        }

        // === Tool Choice ===
        if (toolChoice != null && !toolChoice.isBlank()) {
            payload.put("tool_choice", toolChoice);
        }

        return gson.toJson(payload);
    }
}
