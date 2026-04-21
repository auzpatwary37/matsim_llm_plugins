package chatrequest;

import java.util.List;

import chatcommons.Role;
import tools.IToolResponse;

public class SimpleRequestMessage implements IRequestMessage {

    private final Role role;
    private final String content;
    private final List<IToolResponse<?>> toolResponses;
    private final boolean enableThinking;

    public SimpleRequestMessage(Role role, String content) {
        this(role, content, null,false);
    }
    
    public SimpleRequestMessage(Role role, String content, boolean enableThinking) {
        this(role, content, null, enableThinking);
    }

    public SimpleRequestMessage(Role role, String content, List<IToolResponse<?>> toolResponses, boolean enableThinking) {
        this.role = role;
        this.content = content;
        this.toolResponses = toolResponses;
        this.enableThinking = enableThinking;
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
        return "{" + role + ": " + content + "}" + (enableThinking?"no_think":"");
    }

	@Override
	public boolean ifEnableThinking() {
		return this.enableThinking;
	}
}
