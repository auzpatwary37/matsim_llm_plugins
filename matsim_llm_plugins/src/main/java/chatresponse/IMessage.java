package chatresponse;

import java.util.List;

public interface IMessage {
    String getRole();
    String getContent();
    List<IToolCall> getToolCalls(); // can return null
}

