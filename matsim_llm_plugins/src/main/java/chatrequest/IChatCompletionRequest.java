package chatrequest;

import java.util.List;

import com.google.gson.JsonObject;

import chatcommons.IChatMessage;

public interface IChatCompletionRequest {
    /**
     * Serializes the request to a JSON string for HTTP transmission.
     */
    String serializeToHttpBody(List<IChatMessage> messages,
                               List<JsonObject> tools,
                               String toolChoice,
                               double temperature,
                               int maxTokens,
                               String modelName,
                               boolean stream);
}

