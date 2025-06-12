package chatrequest;

import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import chatcommons.IChatMessage;
import chatresponse.IResponseMessage;
import tools.IToolCall;
import tools.IToolResponse;

/**
 * LmStudio-compatible request builder.
 * Produces OpenAI-compatible JSON format, excluding tool_choice and streaming.
 */
public class LmStudioChatRequest implements IChatCompletionRequest {

    private static final Gson gson = new Gson();

    @Override
    public String serializeToHttpBody(List<IChatMessage> messages,
                                      List<JsonObject> tools,
                                      Map<String, Boolean> toolIfDummy,
                                      String toolChoice,
                                      double temperature,
                                      int maxTokens,
                                      String modelName,
                                      boolean stream) {

        JsonObject payload = new JsonObject();
        payload.addProperty("model", modelName);
        payload.addProperty("temperature", temperature);
        payload.addProperty("max_tokens", maxTokens);
        payload.addProperty("stream", false); // LM Studio doesn't support streaming

        JsonArray messageArray = new JsonArray();

        for (IChatMessage m : messages) {

            // === Case 3 or 4: Response message (assistant)
            if (m instanceof IResponseMessage response) {
                JsonObject mJson = new JsonObject();
                mJson.addProperty("role", "assistant");
                mJson.addProperty("content", response.getContent());

                if (response.getToolCalls() != null && !response.getToolCalls().isEmpty()) {
                    JsonArray toolCalls = new JsonArray();
                    for (IToolCall tc : response.getToolCalls()) {
                        if (!toolIfDummy.getOrDefault(tc.getName(), false)) {
                            JsonObject toolCall = new JsonObject();
                            toolCall.addProperty("id", tc.getId());
                            toolCall.addProperty("type", "function");

                            JsonObject functionObj = new JsonObject();
                            functionObj.addProperty("name", tc.getName());
                            functionObj.addProperty("arguments", tc.getArguments());

                            toolCall.add("function", functionObj);
                            toolCalls.add(toolCall);
                        }
                    }
                    if (toolCalls.size() > 0) {
                        mJson.add("tool_calls", toolCalls);
                    }
                }

                messageArray.add(mJson);
                continue;
            }

            // === Case 2: Tool responses only
            if (m instanceof IRequestMessage request && request.getToolResponses() != null && !request.getToolResponses().isEmpty()) {
                for (IToolResponse<?> tr : request.getToolResponses()) {
                    if (tr.isForLLM()) {
                        JsonObject toolResponse = new JsonObject();
                        toolResponse.addProperty("role", "tool");
                        toolResponse.addProperty("tool_call_id", tr.getToolCallId());
                        toolResponse.addProperty("name", tr.getName());
                        toolResponse.addProperty("content", tr.getResponseJson());
                        messageArray.add(toolResponse);
                    }
                }
                continue; // skip base message
            }

            // === Case 1: Normal system/user message
            JsonObject mJson = new JsonObject();
            mJson.addProperty("role", m.getRole().name().toLowerCase());
            mJson.addProperty("content", m.getContent());
            messageArray.add(mJson);
        }

        payload.add("messages", messageArray);

        // === Tools ===
        if (tools != null && !tools.isEmpty()) {
            JsonArray toolArray = new JsonArray();
            for (JsonObject tool : tools) {
                JsonObject wrapper = new JsonObject();
                wrapper.addProperty("type", "function");
                wrapper.add("function", tool);
                toolArray.add(wrapper);
            }
            payload.add("tools", toolArray);
        }

        return gson.toJson(payload);
    }
}
