package chatcommons;


import java.util.List;

import com.google.gson.JsonObject;

import chatrequest.IRequestMessage;
import chatresponse.IChatCompletionResponse;
import matsimBinding.LLMConfigGroup;

public interface IChatCompletionClient {

    /**
     * Queries the LLM using the full conversation history and the new user message.
     * Responsible for constructing the API payload, sending it, and parsing the response.
     *
     * @param history      All prior messages in the conversation (excluding system message)
     * @param userMessage  The current user message to send
     * @return The assistant's response (may contain tool calls)
     */
    IChatCompletionResponse query(List<IChatMessage> history, IRequestMessage userMessage, List<JsonObject> tools);

    /**
     * Returns the name of the underlying LLM model used.
     */
    LLMConfigGroup getLLMConfig();
}

