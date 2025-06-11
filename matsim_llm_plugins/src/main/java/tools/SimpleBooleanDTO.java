package tools;

import java.util.List;

import com.google.gson.JsonObject;

/**
 * A simple ToolArgumentDTO for a single boolean value.
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

    public static final JsonObject STATIC_SCHEMA;
    static {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "boolean");
        STATIC_SCHEMA = schema;
    }

    public static ToolArgument<Boolean, SimpleBooleanDTO> forArgument(String name) {
        return new ToolArgument<>(
            name,
            SimpleBooleanDTO.class,
            SimpleBooleanDTO::new,
            STATIC_SCHEMA
        );
    }

	@Override
	public boolean isVerified() {
		// TODO Auto-generated method stub
		return true;
	}
}
