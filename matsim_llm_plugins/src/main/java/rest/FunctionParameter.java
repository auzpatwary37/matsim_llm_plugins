package rest;

import java.util.Map;

public class FunctionParameter {
    private String type;
    private Map<String, Object> properties;
    private String[] required;
    private Map<String, Object> definitions;

    // Getters and setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    public String[] getRequired() {
        return required;
    }

    public void setRequired(String[] required) {
        this.required = required;
    }

    public Map<String, Object> getDefinitions() {
        return definitions;
    }

    public void setDefinitions(Map<String, Object> definitions) {
        this.definitions = definitions;
    }
}
