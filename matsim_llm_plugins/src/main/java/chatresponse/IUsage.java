package chatresponse;

public interface IUsage {
    int getPromptTokens();
    int getCompletionTokens();
    int getTotalTokens();
}
