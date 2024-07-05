package gsonprocessor;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

public class PlanElementGsonDeserializer implements JsonDeserializer<PlanElementGson>  {

    @Override
    public PlanElementGson deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        PlanElementGson planElementGson;

        if (jsonObject.has("activityType")) {
            planElementGson = context.deserialize(jsonObject, ActivityGson.class);
        } else if (jsonObject.has("mode")) {
            planElementGson = context.deserialize(jsonObject, LegGson.class);
        } else {
            throw new JsonParseException("Unknown element type");
        }

        return planElementGson;
    }

	
}