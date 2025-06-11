package chatrequest;

import java.util.List;

import chatcommons.Role;
import tools.IToolResponse;

public class SimpleRequestMessage implements IRequestMessage {

    private final Role role;
    private final String content;
    private final List<IToolResponse<?>> toolResponses;

    public SimpleRequestMessage(Role role, String content) {
        this(role, content, null);
    }

    public SimpleRequestMessage(Role role, String content, List<IToolResponse<?>> toolResponses) {
        this.role = role;
        this.content = content;
        this.toolResponses = toolResponses;
    }

    @Override
    public Role getRole() {
        return role;
    }

    @Override
    public String getContent() {
        return content;
    }

    @Override
    public List<IToolResponse<?>> getToolResponses() {
        return toolResponses;
    }

    @Override
    public String toString() {
        return "{" + role + ": " + content + "}";
    }
}
