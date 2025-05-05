package chatresponse;

public interface IToolCall {
    String getId();
    String getType();
    IFunction getFunction();
}
