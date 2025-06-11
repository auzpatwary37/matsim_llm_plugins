package tools;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.function.Function;

import com.google.gson.JsonObject;

/**
 * A generic DTO wrapper for POJOs that allows registering simple Java classes
 * as tool-callable arguments without requiring a custom ToolArgumentDTO implementation.
 *
 * Requirements for the POJO class:
 * - Must have a public no-argument constructor
 * - Must have at least one non-static field
 *
 * This class provides a static factory method to wrap a POJO class as a ToolArgument,
 * and auto-generates an OpenAI-compatible JSON Schema using reflection.
 *
 * Example:
 * <pre>{@code
 * ToolArgument<Person, PojoWrapperDTO<Person>> arg =
 *     PojoWrapperDTO.forPojo("person", Person.class);
 * }</pre>
 *
 * @param <T> the POJO type (base class)
 */
public class SerializableClassWrapperDTO<T> extends ToolArgumentDTO<T> {

    private final T instance;

    /**
     * Constructs a DTO wrapper holding the deserialized POJO instance.
     *
     * @param instance the actual POJO object
     */
    public SerializableClassWrapperDTO(T instance) {
        this.instance = instance;
    }

    /**
     * Returns the original base class instance.
     */
    @Override
    public T toBaseClass() {
        return instance;
    }

    /**
     * Creates a ToolArgument from a POJO class using reflection.
     * This avoids needing a separate DTO class.
     *
     * @param name      the name of the argument
     * @param pojoClass the POJO class (must have no-arg constructor)
     * @param <T>       the base class type
     * @return a ToolArgument that can deserialize into the POJO
     * @throws IllegalArgumentException if the class is not structurally valid
     */
    public static <T> ToolArgument<T, SerializableClassWrapperDTO<T>> forClass(String name, Class<T> pojoClass) {
    	ensureSerializable(pojoClass);
        return new ToolArgument<>(
            name,
            (Class<SerializableClassWrapperDTO<T>>) (Class<?>) SerializableClassWrapperDTO.class, // safe due to use of wrapper
            (Function<T, SerializableClassWrapperDTO<T>>) SerializableClassWrapperDTO::new,
            generateSchema(pojoClass)
        );
    }

    /**
     * Checks that the given class is POJO-compatible:
     * - has a public no-arg constructor
     * - has at least one non-static field
     */
    private static void ensureSerializable(Class<?> clazz) {
        try {
            clazz.getDeclaredConstructor(); // check no-arg constructor exists
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Class " + clazz.getName() + " must have a public no-arg constructor.");
        }

        for (Field field : clazz.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())) {
                return; // valid non-static field found
            }
        }

        throw new IllegalArgumentException("Class " + clazz.getName() + " must have at least one non-static field.");
    }

    /**
     * Generates a simple JSON schema from a POJO's public fields.
     * Maps Java types to OpenAI-compatible schema types.
     */
    public static JsonObject generateSchema(Class<?> clazz) {
        JsonObject properties = new JsonObject();

        for (Field field : clazz.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) continue;

            JsonObject fieldSchema = new JsonObject();
            Class<?> type = field.getType();

            if (type == String.class) fieldSchema.addProperty("type", "string");
            else if (type == int.class || type == Integer.class ||
                     type == double.class || type == Double.class ||
                     type == float.class || type == Float.class ||
                     type == long.class || type == Long.class) {
                fieldSchema.addProperty("type", "number");
            } else if (type == boolean.class || type == Boolean.class) {
                fieldSchema.addProperty("type", "boolean");
            } else {
                fieldSchema.addProperty("type", "object"); // fallback for complex types
            }

            properties.add(field.getName(), fieldSchema);
        }

        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        schema.add("properties", properties);
        return schema;
    }

	@Override
	public boolean isVerified() {
		// TODO Auto-generated method stub
		return true;
	}
}

