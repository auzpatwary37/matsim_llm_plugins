package chatresponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.annotations.SerializedName;
import tools.IToolCall;
import chatcommons.Role;

public class OpenAiChatResponse implements IChatCompletionResponse {

    private String id;
    private String object;
    private long created;
    private String model;
    private List<Choice> choices;
    private Usage usage;

    @SerializedName("system_fingerprint")
    private String systemFingerprint;

    public static class Choice implements IChoice {
        private int index;
        private Message message;

        @SerializedName("finish_reason")
        private String finishReason;

        @Override
        public IResponseMessage getMessage() {
            return message;
        }
    }

    public static class Message implements IResponseMessage {
        private Role role;
        private String content;

        @SerializedName("tool_calls")
        private List<ToolCall> toolCalls;

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
            return (List<IToolCall>)(List<?>) toolCalls;
        }
    }

    public static class ToolCall implements IToolCall {
        private String id;
        private String type;
        private Function function;

        @Override
        public String getId() {
            return id;
        }

        public String getType() {
            return type;
        }

        public Function getFunction() {
            return function;
        }

        @Override
        public String getName() {
            return function.getName();
        }

        @Override
        public String getArguments() {
            return function.getArguments();
        }
    }

    public static class Function {
        private String name;
        private String arguments;

        public String getName() {
            return name;
        }

        public String getArguments() {
            return arguments;
        }
    }

    public static class Usage implements IUsage {
        @SerializedName("prompt_tokens")
        private int promptTokens;

        @SerializedName("completion_tokens")
        private int completionTokens;

        @SerializedName("total_tokens")
        private int totalTokens;

        @Override
        public int getPromptTokens() {
            return promptTokens;
        }

        @Override
        public int getCompletionTokens() {
            return completionTokens;
        }

        @Override
        public int getTotalTokens() {
            return totalTokens;
        }
    }

    @Override
    public IResponseMessage getMessage() {
        return choices != null && !choices.isEmpty() ? choices.get(0).getMessage() : null;
    }

    @Override
    public List<IToolCall> getToolCalls() {
        IResponseMessage msg = getMessage();
        return msg != null ? msg.getToolCalls() : List.of();
    }

    @Override
    public IUsage getUsage() {
        return usage;
    }

    @Override
    public String getModel() {
        return model;
    }

    @Override
    public Map<String, Object> getMetadata() {
        Map<String, Object> meta = new HashMap<>();
        meta.put("id", id);
        meta.put("object", object);
        meta.put("created", created);
        meta.put("system_fingerprint", systemFingerprint);
        return meta;
    }
}
