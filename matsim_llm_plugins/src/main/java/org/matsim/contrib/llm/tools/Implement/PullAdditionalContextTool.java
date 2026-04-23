package org.matsim.contrib.llm.tools.Implement;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.*;

import org.matsim.contrib.llm.rag.IVectorDB;
import org.matsim.contrib.llm.rag.IVectorDB.RetrievedDocument;
import org.matsim.contrib.llm.tools.DefaultToolResponse;
import org.matsim.contrib.llm.tools.ErrorMessages;
import org.matsim.contrib.llm.tools.ITool;
import org.matsim.contrib.llm.tools.IToolResponse;
import org.matsim.contrib.llm.tools.ToolArgument;
import org.matsim.contrib.llm.tools.ToolArgumentDTO;
import org.matsim.contrib.llm.tools.VerificationFailedException;
import org.matsim.contrib.llm.tools.SimpleStringDTO;

public class PullAdditionalContextTool implements ITool<List<RetrievedDocument>> {

    private final Map<String, ToolArgument<?, ? extends ToolArgumentDTO<?>>> arguments = new HashMap<>();

    public PullAdditionalContextTool() {
        registerArgument(SimpleStringDTO.forArgument("text"));

        registerArgument(new ToolArgument<>(
                "metadataFilters",
                KeyValuePairDTO.class,
                KeyValuePairDTO::fromBaseClass,
                buildArraySchema()
        ));
    }

    @Override
    public String getName() {
        return "pull_additional_context";
    }

    @Override
    public String getDescription() {
        return "Retrieve relevant context using text query and optional metadata filters.";
    }

    @Override
    public boolean isDummy() {
        return false;
    }

    @Override
    public Map<String, ToolArgument<?, ? extends ToolArgumentDTO<?>>> getRegisteredArguments() {
        return arguments;
    }

    @Override
    public Class<List<RetrievedDocument>> getOutputClass() {
        @SuppressWarnings("unchecked")
        Class<List<RetrievedDocument>> clazz = (Class<List<RetrievedDocument>>) (Class<?>) List.class;
        return clazz;
    }

    @Override
    public JsonObject getJsonSchema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("name", getName());
        schema.addProperty("description", getDescription());

        JsonObject parameters = new JsonObject();
        parameters.addProperty("type", "object");

        JsonObject properties = new JsonObject();
        properties.add("text", SimpleStringDTO.STATIC_SCHEMA);
        properties.add("metadataFilters", buildArraySchema());

        JsonArray required = new JsonArray();
        required.add("text");

        parameters.add("properties", properties);
        parameters.add("required", required);

        schema.add("parameters", parameters);
        return schema;
    }

    @Override
    public IToolResponse<List<RetrievedDocument>> callTool(
            String id,
            Map<String, Object> arguments,
            IVectorDB vectorDB,
            Map<String, Object> context) {

        String text = (String) arguments.get("text");

        @SuppressWarnings("unchecked")
        List<KeyValuePairDTO> filters = (List<KeyValuePairDTO>) arguments.get("metadataFilters");

        Map<String, String> metadataMap = new HashMap<>();
        if (filters != null) {
            for (KeyValuePairDTO f : filters) {
                if (f != null && f.key != null && f.value != null) {
                    metadataMap.put(f.key, f.value);
                }
            }
        }

        List<RetrievedDocument> docs = metadataMap.isEmpty()
                ? vectorDB.query(text, 5)
                : vectorDB.query(text, 5, metadataMap);

        JsonObject out = new JsonObject();
        out.addProperty("query", text);

        JsonArray arr = new JsonArray();
        for (RetrievedDocument d : docs) {
            JsonObject o = new JsonObject();
            o.addProperty("id", d.id());
            o.addProperty("content", d.content());
            arr.add(o);
        }
        out.add("documents", arr);

        return new DefaultToolResponse<>(
                id,
                getName(),
                new Gson().toJson(out),
                docs,
                false
        );
    }

    @Override
    public void verifyArguments(Map<String, Object> arguments, Map<String, Object> context, ErrorMessages em)
            throws VerificationFailedException {

        if (arguments == null || arguments.get("text") == null) {
            throw new VerificationFailedException(List.of("Missing text"));
        }
    }

    private static JsonObject buildArraySchema() {
        JsonObject array = new JsonObject();
        array.addProperty("type", "array");
        array.add("items", KeyValuePairDTO.STATIC_SCHEMA);
        return array;
    }

    // =========================
    // INNER DTO (simple + clean)
    // =========================
    public static class KeyValuePairDTO extends ToolArgumentDTO<Map.Entry<String, String>> {

        public String key;
        public String value;

        public KeyValuePairDTO() {}

        public static KeyValuePairDTO fromBaseClass(Map.Entry<String, String> base) {
            KeyValuePairDTO dto = new KeyValuePairDTO();
            if (base != null) {
                dto.key = base.getKey();
                dto.value = base.getValue();
            }
            return dto;
        }

        @Override
        public Map.Entry<String, String> toBaseClass(Map<String, Object> context, ErrorMessages em) {
            return Map.entry(key, value);
        }

        @Override
        public boolean isVerified(ErrorMessages em, Map<String,Object> context) {
            if (key == null || key.isBlank()) return false;
            if (value == null) return false;
            return true;
        }

        public static final JsonObject STATIC_SCHEMA;
        static {
            JsonObject schema = new JsonObject();
            schema.addProperty("type", "object");

            JsonObject props = new JsonObject();

            JsonObject k = new JsonObject();
            k.addProperty("type", "string");
            props.add("key", k);

            JsonObject v = new JsonObject();
            v.addProperty("type", "string");
            props.add("value", v);

            JsonArray required = new JsonArray();
            required.add("key");
            required.add("value");

            schema.add("properties", props);
            schema.add("required", required);

            STATIC_SCHEMA = schema;
        }
    }
}