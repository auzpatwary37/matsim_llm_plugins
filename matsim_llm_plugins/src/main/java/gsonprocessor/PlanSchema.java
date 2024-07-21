package gsonprocessor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import rest.Function;
import rest.FunctionParameter;
import rest.Tool;

public class PlanSchema {
public static final String PlanGson_Schema = "{\n"
		+ "    \"type\": \"function\",\n"
		+ "    \"function\": {\n"
		+ "        \"name\": \"dummy_plan\",\n"
		+ "        \"description\": \"Create a Plan Object for MATSim execution.\",\n"
		+ "        \"parameters\": {\n"
		+ "            \"type\": \"Object\",\n"
		+ "            \"properties\": {\n"
		+ "                \"activitiesAndLegs\": {\n"
		+ "                    \"type\": \"array\",\n"
		+ "                    \"description\": \"Alternates between ActivityGson and LegGson starting and ending with ActivityGson.\",\n"
		+ "                    \"items\": {\n"
		+ "                        \"oneOf\": [\n"
		+ "                            {\n"
		+ "                                \"$ref\": \"#/definitions/ActivityGson\"\n"
		+ "                            },\n"
		+ "                            {\n"
		+ "                                \"$ref\": \"#/definitions/LegGson\"\n"
		+ "                            }\n"
		+ "                        ]\n"
		+ "                    }\n"
		+ "                }\n"
		+ "            },\n"
		+ "            \"required\": [\n"
		+ "                \"activitiesAndLegs\"\n"
		+ "            ],\n"
		+ "            \"definitions\": {\n"
		+ "                \"ActivityGson\": {\n"
		+ "                    \"type\": \"object\",\n"
		+ "                    \"properties\": {\n"
		+ "                        \"activityType\": {\n"
		+ "                            \"type\": \"string\",\n"
		+ "                            \"description\": \"Type of the activity performed\",\n"
		+ "                            \"enum\": [\n"
		+ "                                \"plugin\",\n"
		+ "                                \"plugout\",\n"
		+ "                                \"leisure\",\n"
		+ "                                \"education\",\n"
		+ "                                \"work\",\n"
		+ "                                \"errands\",\n"
		+ "                                \"shop\",\n"
		+ "                                \"home\"\n"
		+ "                            ]\n"
		+ "                        },\n"
		+ "                        \"endTime\": {\n"
		+ "                            \"type\": \"number\",\n"
		+ "                            \"description\": \"End time of this activity in seconds. Ranges from 0 to 86400.\"\n"
		+ "                        },\n"
		+ "                        \"carLocation\": {\n"
		+ "                            \"type\": \"string\",\n"
		+ "                            \"description\": \"Current location of the agent's car. Should be same as the activity type.\"\n"
		+ "                        }\n"
		+ "                    },\n"
		+ "                    \"required\": [\n"
		+ "                        \"activityType\",\n"
		+ "                        \"endTime\",\n"
		+ "                        \"carLocation\"\n"
		+ "                    ]\n"
		+ "                },\n"
		+ "                \"LegGson\": {\n"
		+ "                    \"type\": \"object\",\n"
		+ "                    \"properties\": {\n"
		+ "                        \"mode\": {\n"
		+ "                            \"type\": \"string\",\n"
		+ "                            \"description\": \"Mode of the leg.\",\n"
		+ "                            \"enum\": [\n"
		+ "                                \"car\",\n"
		+ "                                \"car_passenger\",\n"
		+ "                                \"walk\",\n"
		+ "                                \"pt\",\n"
		+ "                                \"bike\"\n"
		+ "                            ]\n"
		+ "                        }\n"
		+ "                    },\n"
		+ "                    \"required\": [\n"
		+ "                        \"mode\"\n"
		+ "                    ]\n"
		+ "                }\n"
		+ "            }\n"
		+ "        }\n"
		+ "    }\n"
		+ "}";
	
	public static void main(String[] args) {
		GsonBuilder gsonBuilder = new GsonBuilder()
				.registerTypeAdapter(PlanElementGson.class, new PlanElementGsonDeserializer());
		gsonBuilder.serializeNulls();
	    gsonBuilder.serializeSpecialFloatingPointValues();
	    gsonBuilder.setPrettyPrinting();
	    Gson gson = gsonBuilder.create();
	    String jsonSchema = gson.toJson(getPlanGsonSchemaAsFunctionTool());
	    try {
			FileWriter fw = new FileWriter(new File("planSchema.json"));
			fw.append(jsonSchema);
			fw.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static Tool getPlanGsonSchemaAsFunctionTool(){
		// Create the tools array
        Tool tool = new Tool();
        tool.setType("function");

        Function function = new Function();
        function.setName("dummy_plan");
        function.setDescription("Create a Plan Object for MATSim execution.");

        FunctionParameter parameters = new FunctionParameter();
        parameters.setType("object");

        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> activitiesAndLegs = new HashMap<>();
        activitiesAndLegs.put("type", "array");
        activitiesAndLegs.put("description", "Alternates between ActivityGson and LegGson starting and ending with ActivityGson.");
        
        Map<String, Object> items = new HashMap<>();
        items.put("oneOf", Arrays.asList(
            Map.of("$ref", "#/definitions/ActivityGson"),
            Map.of("$ref", "#/definitions/LegGson")
        ));
        
        activitiesAndLegs.put("items", items);
        properties.put("activitiesAndLegs", activitiesAndLegs);

        parameters.setProperties(properties);
        parameters.setRequired(new String[]{"activitiesAndLegs"});

        Map<String, Object> definitions = new HashMap<>();
        Map<String, Object> activityGson = new HashMap<>();
        activityGson.put("type", "object");
        
        Map<String, Object> activityProperties = new HashMap<>();
        activityProperties.put("activityType", Map.of(
            "type", "string",
            "description", "Type of the activity performed",
            "enum", List.of("plugin", "plugout", "leisure", "education", "work", "errands", "shop", "home")
        ));
        activityProperties.put("endTime", Map.of(
            "type", "number",
            "description", "End time of this activity in seconds. Ranges from 0 to 86400."
        ));
        activityProperties.put("carLocation", Map.of(
            "type", "string",
            "description", "Current location of the agent's car. Should be same as the activity type."
        ));
        activityProperties.put("id", Map.of(
                "type", "string",
                "description", "Id to identify a specific activity in a plan. The id should be the activity type + \"___\"+the order of occurance of this type of activity in the plan."
            ));
        activityGson.put("properties", activityProperties);
        activityGson.put("required", List.of("id","activityType", "endTime", "carLocation"));

        definitions.put("ActivityGson", activityGson);

        Map<String, Object> legGson = new HashMap<>();
        legGson.put("type", "object");
        
        Map<String, Object> legProperties = new HashMap<>();
        legProperties.put("mode", Map.of(
            "type", "string",
            "description", "Mode of the leg.",
            "enum", List.of("car", "car_passenger", "walk", "pt", "bike")
        ));
        legGson.put("properties", legProperties);
        legGson.put("required", List.of("mode"));

        definitions.put("LegGson", legGson);

        parameters.setDefinitions(definitions);

        function.setParameters(parameters);
        tool.setFunction(function);
        return tool;
	}
}
