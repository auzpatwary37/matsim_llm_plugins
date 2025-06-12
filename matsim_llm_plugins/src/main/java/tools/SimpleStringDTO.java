package tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * A trivial DTO for simple string arguments.
 * Expects a JSON object of the form: { "value": "..." }
 */
public class SimpleStringDTO extends ToolArgumentDTO<String> {

    public String value;

    public SimpleStringDTO() {}

    public SimpleStringDTO(String value) {
        this.value = value;
    }

    @Override
    public String toBaseClass() {
        return value;
    }

    @Override
    public boolean isVerified() {
        return value != null;
    }

    /**
     * Returns the OpenAI-compatible JSON schema for this DTO:
     * {
     *   "type": "object",
     *   "properties": {
     *     "value": { "type": "string" }
     *   },
     *   "required": ["value"]
     * }
     */
    public static final JsonObject STATIC_SCHEMA;
    static {
        JsonObject valueProp = new JsonObject();
        valueProp.addProperty("type", "string");

        JsonObject props = new JsonObject();
        props.add("value", valueProp);

        JsonArray required = new JsonArray();
        required.add("value");

        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        schema.add("properties", props);
        schema.add("required", required);

        STATIC_SCHEMA = schema;
    }

    /**
     * Convenience method to create a ToolArgument for a simple string parameter.
     */
    public static ToolArgument<String, SimpleStringDTO> forArgument(String name) {
        return new ToolArgument<>(
            name,
            SimpleStringDTO.class,
            SimpleStringDTO::new,
            STATIC_SCHEMA
        );
    }
}
