package tools;

import java.util.function.Function;

import com.google.gson.Gson;
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

    public B fromJson(String json, Gson gson) {
        T dto = gson.fromJson(json, dtoClass);
        dto.isVerified();
        return dto.toBaseClass();
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

