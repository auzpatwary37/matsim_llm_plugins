package tools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.gson.JsonObject;

import chatcommons.Role;
import chatrequest.IRequestMessage;
import chatrequest.SimpleRequestMessage;
import rag.IVectorDB;

public class DefaultToolManager implements IToolManager {

    private final Map<String, ITool<?>> toolRegistry = new HashMap<>();

    @Override
    public void registerTool(ITool<?> tool) {
        Objects.requireNonNull(tool.getName(), "Tool name must not be null");
        toolRegistry.put(tool.getName(), tool);
    }

    @Override
    public ITool<?> getByName(String name) {
        ITool<?> tool = toolRegistry.get(name);
        if (tool == null) {
            throw new IllegalArgumentException("No tool registered with name: " + name);
        }
        return tool;
    }

    @Override
    public List<JsonObject> getAllToolSchemas() {
        List<JsonObject> schemas = new ArrayList<>();
        for (ITool<?> tool : toolRegistry.values()) {
            schemas.add(tool.getJsonSchema());
        }
        return schemas;
    }

    @Override
    public <T> IToolResponse<T> runToolCall(IToolCall call, IVectorDB vectorDB) {
        @SuppressWarnings("unchecked")
        ITool<T> tool = (ITool<T>) getByName(call.getName());
        return tool.call(call.getArguments(),call.getId(), vectorDB);
    }

    @Override
    public List<IToolResponse<?>> runToolCalls(List<IToolCall> calls, IVectorDB vectorDB) {
        List<IToolResponse<?>> results = new ArrayList<>();
        for (IToolCall call : calls) {
            IToolResponse<?> response = runToolCall(call, vectorDB);
            results.add(response);  // include both dummy and real tool responses
        }
        return results;
    }

    @Override
    public IRequestMessage buildToolResponseMessage(List<IToolCall> calls, IVectorDB vectorDB) {
        List<IToolResponse<?>> allResponses = runToolCalls(calls, vectorDB);

        // Only include tool responses that should be sent back to the LLM
        List<IToolResponse<?>> forLLM = allResponses.stream()
                .filter(IToolResponse::isForLLM)
                .toList();

        if (forLLM.isEmpty()) {
            return null;
        }

        return new SimpleRequestMessage(Role.TOOL, null, forLLM);
    }
}
