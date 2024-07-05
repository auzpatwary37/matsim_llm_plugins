package rest;

import com.google.gson.annotations.SerializedName;

public class Function {
    private String name;
    private String description;
    private FunctionParameter parameters;

    // Getters and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public FunctionParameter getParameters() {
        return parameters;
    }

    public void setParameters(FunctionParameter parameters) {
        this.parameters = parameters;
    }
}
