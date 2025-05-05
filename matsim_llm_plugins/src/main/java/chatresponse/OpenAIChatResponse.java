package chatresponse;


import com.google.gson.annotations.SerializedName;
import java.util.List;

public class OpenAIChatResponse implements IChatCompletionResponse {

    private String id;
    private String object;
    private long created;
    private String model;
    private List<Choice> choices;
    private Usage usage;
    public static final String NAME = "openai";


    public static class Choice implements IChoice {
        private int index;
        private Message message;

        @SerializedName("finish_reason")
        private String finishReason;

        @Override
        public IMessage getMessage() {
            return message;
        }

        public String getFinishReason() {
            return finishReason;
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public void setMessage(Message message) {
            this.message = message;
        }

        public void setFinishReason(String finishReason) {
            this.finishReason = finishReason;
        }
    }

    public static class Message implements IMessage {
        private String role;
        private String content;

        @SerializedName("tool_calls")
        private List<ToolCall> toolCalls;

        @Override
        public String getRole() {
            return role;
        }

        @Override
        public String getContent() {
            return content;
        }

        @Override
        public List<IToolCall> getToolCalls() {
            return toolCalls != null ? List.copyOf(toolCalls) : null;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public void setToolCalls(List<ToolCall> toolCalls) {
            this.toolCalls = toolCalls;
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

        @Override
        public String getType() {
            return type;
        }

        @Override
        public IFunction getFunction() {
            return function;
        }

        public void setId(String id) {
            this.id = id;
        }

        public void setType(String type) {
            this.type = type;
        }

        public void setFunction(Function function) {
            this.function = function;
        }
    }

    public static class Function implements IFunction {
        private String name;
        private String arguments;

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getArguments() {
            return arguments;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setArguments(String arguments) {
            this.arguments = arguments;
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

        public void setPromptTokens(int promptTokens) {
            this.promptTokens = promptTokens;
        }

        public void setCompletionTokens(int completionTokens) {
            this.completionTokens = completionTokens;
        }

        public void setTotalTokens(int totalTokens) {
            this.totalTokens = totalTokens;
        }
    }

    @Override
    public List<Choice> getChoices() {
        return choices;
    }

    @Override
    public String getModel() {
        return model;
    }

    @Override
    public IUsage getUsage() {
        return usage;
    }

    public String getId() {
        return id;
    }

    public String getObject() {
        return object;
    }

    public long getCreated() {
        return created;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setObject(String object) {
        this.object = object;
    }

    public void setCreated(long created) {
        this.created = created;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public void setChoices(List<Choice> choices) {
        this.choices = choices;
    }

    public void setUsage(Usage usage) {
        this.usage = usage;
    }
}

