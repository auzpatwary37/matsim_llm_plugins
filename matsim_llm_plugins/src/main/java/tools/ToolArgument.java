package tools;

import java.lang.reflect.Method;
import java.util.function.Function;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class ToolArgument<B, T extends ToolArgumentDTO<B>> {

    private final String name;
    private final Class<T> dtoClass;
    private final Function<B, T> toDTO;
    private JsonObject schema;
    

    public ToolArgument(String name, Class<T> dtoClass, Function<B, T> toDTO) {
        this.name = name;
        this.dtoClass = dtoClass;
        this.toDTO = toDTO;
    }
    
    public ToolArgument(String name, Class<T> dtoClass, Function<B, T> toDTO, JsonObject schema) {
        this.name = name;
        this.dtoClass = dtoClass;
        this.toDTO = toDTO;
        this.schema = schema;
    }

    public T toDTO(B baseObject) {
        return toDTO.apply(baseObject);
    }

    public B fromJson(String json, Gson gson, ErrorMessages em) {
        try {
        	JsonElement element = gson.fromJson(json, com.google.gson.JsonElement.class);
            // First parse into JsonObject once
            //JsonObject obj = gson.fromJson(json, JsonObject.class);
        	JsonObject obj = null;
        	if (element != null && element.isJsonObject()) {
                obj = element.getAsJsonObject();
            } else {
                // For primitive JSON like "car" or 28322, wrap into {"value": ...}
                obj = new JsonObject();
                obj.add("value", element);
            }
            T dto;

            try {
                // Look for optional static parser
                Method parser = dtoClass.getMethod("fromJsonObject", JsonObject.class, Gson.class);
                @SuppressWarnings("unchecked")
                T parsed = (T) parser.invoke(null, obj, gson);
                dto = parsed;

            } catch (NoSuchMethodException e) {
                // No custom parser -> use default Gson
                dto = gson.fromJson(obj, dtoClass);
            }

            if (dto == null) {
                throw new RuntimeException("Failed to parse DTO: " + dtoClass.getName());
            }

            //dto.afterJsonLoad(json, gson);

            if (!dto.isVerified(em)) {
                throw new RuntimeException("DTO verification failed: " + dtoClass.getName());
            }

            B base = dto.toBaseClass(null, em);

            if (base == null) {
                throw new RuntimeException("DTO toBaseClass returned null: " + dtoClass.getName());
            }

            return base;

        } catch (Exception e) {
        	
            em.addErrorMessages("Failed to parse tool argument for  " + json);
            return null;
        }
    }

    public String getName() {
        return name;
    }

    public Class<T> getDtoClass() {
        return dtoClass;
    }
    
    public JsonObject getDTOSchema() {
    	
    	return this.schema;
    }
    
    public void setSchema(JsonObject schema) {
    	this.schema = schema;
    }
}

