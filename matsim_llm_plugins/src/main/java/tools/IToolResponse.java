package tools;

public interface IToolResponse<T> {
    String getToolCallId();  // optional
    String getName();
    String getResponseJson();
    T getToolCallOutputContainer();
    boolean isForLLM();
}
