package tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * A simple ToolArgumentDTO for a single double value.
 * Expects JSON of the form: { "value": 3.14 }
 */
public class SimpleDoubleDTO extends ToolArgumentDTO<Double> {

    public double value;

    public SimpleDoubleDTO() {}

    public SimpleDoubleDTO(Double value) {
        this.value = value;
    }

    @Override
    public Double toBaseClass() {
        return value;
    }

    @Override
    public boolean isVerified() {
        return true; // or apply bounds check if needed
    }

    /**
     * Returns the OpenAI-compatible schema: { "type": "object", "properties": { "value": { "type": "number" } }, "required": ["value"] }
     */
    public static final JsonObject STATIC_SCHEMA;
    static {
        JsonObject valueProp = new JsonObject();
        valueProp.addProperty("type", "number");

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
     * Convenience method to create a ToolArgument for a simple double parameter.
     */
    public static ToolArgument<Double, SimpleDoubleDTO> forArgument(String name) {
        return new ToolArgument<>(
            name,
            SimpleDoubleDTO.class,
            SimpleDoubleDTO::new,
            STATIC_SCHEMA
        );
    }
}