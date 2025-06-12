package tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * A simple ToolArgumentDTO for a single boolean value.
 * Expects JSON of the form: { "value": true }
 */
public class SimpleBooleanDTO extends ToolArgumentDTO<Boolean> {

    public boolean value;

    public SimpleBooleanDTO() {}

    public SimpleBooleanDTO(Boolean value) {
        this.value = value;
    }

    @Override
    public Boolean toBaseClass() {
        return value;
    }

    @Override
    public boolean isVerified() {
        return true; // or: return true if you want to allow default false
    }

    /**
     * Returns the OpenAI-compatible schema for: { "value": true }
     */
    public static final JsonObject STATIC_SCHEMA;
    static {
        JsonObject valueProp = new JsonObject();
        valueProp.addProperty("type", "boolean");

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
     * Convenience method to create a ToolArgument for a simple boolean parameter.
     */
    public static ToolArgument<Boolean, SimpleBooleanDTO> forArgument(String name) {
        return new ToolArgument<>(
            name,
            SimpleBooleanDTO.class,
            SimpleBooleanDTO::new,
            STATIC_SCHEMA
        );
    }
}
