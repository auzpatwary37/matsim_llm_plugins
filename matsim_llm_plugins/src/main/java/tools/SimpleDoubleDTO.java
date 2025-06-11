package tools;

import java.util.List;

import com.google.gson.JsonObject;

/**
 * A simple ToolArgumentDTO for a single double value.
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

    public static final JsonObject STATIC_SCHEMA;
    static {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "number");
        STATIC_SCHEMA = schema;
    }

    public static ToolArgument<Double, SimpleDoubleDTO> forArgument(String name) {
        return new ToolArgument<>(
            name,
            SimpleDoubleDTO.class,
            SimpleDoubleDTO::new,
            STATIC_SCHEMA
        );
    }

	@Override
	public boolean isVerified() {
		// TODO Auto-generated method stub
		return true;
	}
}

