package tools;


import java.util.List;

import com.google.gson.JsonObject;

/**
 * A trivial DTO for simple string arguments.
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

    public static final JsonObject STATIC_SCHEMA;

    static {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "string");
        STATIC_SCHEMA = schema;
    }

    /**
     * Convenience method to create a ToolArgument for a simple string parameter.
     */
    public static ToolArgument<String, SimpleStringDTO> forArgument(String name) {
        return new ToolArgument<String,SimpleStringDTO>(
            name,
            SimpleStringDTO.class,
            SimpleStringDTO::new,
            STATIC_SCHEMA
        );
    }

	@Override
	public boolean isVerified() {
		// TODO Auto-generated method stub
		return true;
	}
}

