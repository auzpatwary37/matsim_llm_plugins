package matsimdtobjects;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.function.Function;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.population.PopulationUtils;
import org.matsim.facilities.ActivityFacility;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class ActivityDTO extends PlanElementDTO<Activity> {

    // Discriminator for nested polymorphic DTO parsing
    public String elementType = "activity";

    // Tool fields
    public String type;
    public String facilityId;
    public Double endTime; // seconds since midnight

    // Optional convenience flag; kept for compatibility, but derived from type
    public Boolean ifInteractionActivity;

    public ActivityDTO() {
        this.elementType = "activity";
    }

    public ActivityDTO(Activity activity) {
        this.elementType = "activity";

        if (activity == null) {
            return;
        }

        this.type = activity.getType();

        if (activity.getFacilityId() != null) {
            this.facilityId = activity.getFacilityId().toString();
        }

        this.endTime = safeGetTimeSeconds(activity, "getEndTime");

        if (this.type != null) {
            this.ifInteractionActivity =
                    "pt interaction".equals(this.type) || "bike interaction".equals(this.type);
        }
    }

    @Override
    public Activity toBaseClass(Map<String, Object> context) {
        if (!isVerified()) {
            return null;
        }

        Id<ActivityFacility> fid = Id.create(facilityId.trim(), ActivityFacility.class);
        Activity act = PopulationUtils.createActivityFromFacilityId(type.trim(), fid);

        if (endTime != null) {
            safeSetTimeSeconds(act, "setEndTime", endTime);
        }

        return act;
    }

    @Override
    public boolean isVerified() {
        if (!"activity".equals(elementType)) {
            return false;
        }

        if (type == null || type.trim().isEmpty()) {
            return false;
        }

        if (facilityId == null || facilityId.trim().isEmpty()) {
            return false;
        }

        String t = type.trim();
        if (!isAllowedType(t)) {
            return false;
        }


        if (endTime != null) {
            if (endTime < 0) {
                return false;
            }

        }

        return true;
    }

    public static Function<Activity, ActivityDTO> toDTOFromBaseObject() {
        return ActivityDTO::new;
    }

    public static JsonObject getJsonSchema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        schema.addProperty("additionalProperties", false);
        schema.addProperty(
                "description",
                "MATSim Activity definition (tool argument). Required: elementType, type, facilityId. "
                        + "Optional: endTime (seconds since midnight). "
        );

        JsonObject props = new JsonObject();

        JsonObject elementTypeProp = new JsonObject();
        elementTypeProp.addProperty("type", "string");
        elementTypeProp.addProperty("description", "Discriminator for the MATSim plan element subtype.");
        JsonArray elementTypeEnum = new JsonArray();
        elementTypeEnum.add("activity");
        elementTypeProp.add("enum", elementTypeEnum);
        props.add("elementType", elementTypeProp);

        JsonObject typeProp = new JsonObject();
        typeProp.addProperty("type", "string");
        typeProp.addProperty("description", "Activity type used in the MATSim plan.");
        JsonArray typeEnum = new JsonArray();
        typeEnum.add("work");
        typeEnum.add("home");
        typeEnum.add("shopping");
        typeEnum.add("leisure");
        typeEnum.add("education");
        typeEnum.add("restaurants");
        typeEnum.add("errands");
        typeEnum.add("pt interaction");
        typeEnum.add("bike interaction");
        typeProp.add("enum", typeEnum);
        props.add("type", typeProp);

        JsonObject facProp = new JsonObject();
        facProp.addProperty("type", "string");
        facProp.addProperty("description", "MATSim facility id where the activity takes place.");
        props.add("facilityId", facProp);

        JsonObject endProp = new JsonObject();
        endProp.addProperty("type", "number");
        endProp.addProperty(
                "description",
                "End time of the activity in seconds since midnight. Example: 28800 = 8:00 AM. "
                        + "Typically omit for the last activity and for interaction activities."
        );
        props.add("endTime", endProp);

//        JsonObject interactionProp = new JsonObject();
//        interactionProp.addProperty("type", "boolean");
//        interactionProp.addProperty(
//                "description",
//                "True if this is an interaction activity (type is 'pt interaction' or 'bike interaction'). "
//                        + "Optional; if provided it must match the type."
//        );
//        props.add("ifInteractionActivity", interactionProp);

        schema.add("properties", props);

        JsonArray required = new JsonArray();
        required.add("elementType");
        required.add("type");
        required.add("facilityId");
        schema.add("required", required);

        return schema;
    }

    private static boolean isAllowedType(String t) {
        return "work".equals(t)
                || "home".equals(t)
                || "shopping".equals(t)
                || "leisure".equals(t)
                || "education".equals(t)
                || "restaurants".equals(t)
                || "errands".equals(t)
                || "pt interaction".equals(t)
                || "bike interaction".equals(t);
    }

    private static Double safeGetTimeSeconds(Activity a, String getterName) {
        try {
            Method m = a.getClass().getMethod(getterName);
            Object v = m.invoke(a);
            if (v == null) {
                return null;
            }

            if (v instanceof Number) {
                return ((Number) v).doubleValue();
            }

            try {
                Method isDefined = v.getClass().getMethod("isDefined");
                Method seconds = v.getClass().getMethod("seconds");
                boolean defined = (boolean) isDefined.invoke(v);
                if (!defined) {
                    return null;
                }
                Object sec = seconds.invoke(v);
                if (sec instanceof Number) {
                    return ((Number) sec).doubleValue();
                }
            } catch (NoSuchMethodException ignored) {
            }

        } catch (Throwable ignored) {
        }
        return null;
    }

    private static void safeSetTimeSeconds(Activity a, String setterName, Double seconds) {
        if (seconds == null) {
            return;
        }
        try {
            Method m = a.getClass().getMethod(setterName, double.class);
            m.invoke(a, seconds.doubleValue());
        } catch (Throwable ignored) {
        }
    }

    @Override
    public String getElementType() {
        return elementType;
    }
}