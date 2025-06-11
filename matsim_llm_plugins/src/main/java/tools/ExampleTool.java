package tools;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonObject;

import rag.IVectorDB;

public class ExampleTool implements ITool<ExampleTool.ExampleOutput> {

    private final Map<String, ToolArgument<?, ? extends ToolArgumentDTO<?>>> arguments = new HashMap<>();

    public ExampleTool() {
        registerArgument(new ToolArgument<>(
            "customInput",
            CustomInputDTO.class,
            CustomInputDTO::fromBaseClass,
            CustomInputDTO.STATIC_SCHEMA
        ));

        registerArgument(SerializableClassWrapperDTO.forClass("person", Person.class));
        registerArgument(SimpleStringDTO.forArgument("message"));
        registerArgument(SimpleBooleanDTO.forArgument("enabled"));
        registerArgument(SimpleDoubleDTO.forArgument("threshold"));
    }

    @Override
    public String getName() { return "example_tool"; }

    @Override
    public String getDescription() { return "Tool showing all argument types."; }

    @Override
    public boolean isDummy() { return true; }

    @Override
    public Map<String, ToolArgument<?, ? extends ToolArgumentDTO<?>>> getRegisteredArguments() {
        return arguments;
    }
  
 
	@Override
    public IToolResponse<ExampleOutput> callTool(String id, Map<String, Object> inputs, IVectorDB vectorDB) {
        CustomInput custom = (CustomInput) inputs.get("customInput");
        Person person = (Person) inputs.get("person");
        String message = (String) inputs.get("message");
        boolean enabled = (Boolean) inputs.get("enabled");
        double threshold = (Double) inputs.get("threshold");

        System.out.println("Dummy tool called with:");
        System.out.println(custom);
        System.out.println(person);
        System.out.println(message);
        System.out.println(enabled);
        System.out.println(threshold);

        ExampleOutput output = new ExampleOutput();
        output.status = "OK";
        output.message = "This function is dummy and has no return.";
        return new DefaultToolResponse<ExampleOutput>(id, getName(), null, output, true); // isForLLM = false
    }

    // === Output DTO ===
    static class ExampleOutput {
        public String status;
        public String message;
    }

    // === Nested Input DTOs and Classes ===

    static class CustomInput {
        public String id;
        public int count;
    }

    static class CustomInputDTO extends ToolArgumentDTO<CustomInput> {
        public String id;
        public int count;

        @Override public CustomInput toBaseClass() {
            CustomInput c = new CustomInput();
            c.id = id;
            c.count = count;
            return c;
        }

        public static CustomInputDTO fromBaseClass(CustomInput input) {
            CustomInputDTO dto = new CustomInputDTO();
            dto.id = input.id;
            dto.count = input.count;
            return dto;
        }

        public static final JsonObject STATIC_SCHEMA;
        static {
            JsonObject props = new JsonObject();
            JsonObject idField = new JsonObject(); idField.addProperty("type", "string");
            JsonObject countField = new JsonObject(); countField.addProperty("type", "number");
            props.add("id", idField);
            props.add("count", countField);
            JsonObject schema = new JsonObject();
            schema.addProperty("type", "object");
            schema.add("properties", props);
            STATIC_SCHEMA = schema;
        }
		@Override
		public boolean isVerified() {//no additional verification for this test case. 
			return true;
		}
    }

    static class Person {
        public String name;
        public int age;

        public Person() {}
    }

	@Override
	public Class<ExampleOutput> getOutputClass() {
		return ExampleOutput.class;
	}

	


	
}
