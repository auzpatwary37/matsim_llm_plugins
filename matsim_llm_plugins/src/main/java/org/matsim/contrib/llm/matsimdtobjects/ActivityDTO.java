package org.matsim.contrib.llm.matsimdtobjects;

import java.util.Map;
import java.util.function.Function;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.population.PopulationUtils;
import org.matsim.facilities.ActivityFacility;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.matsim.contrib.llm.tools.ErrorMessages;

public class ActivityDTO extends PlanElementDTO<Activity> {

    // Discriminator for nested polymorphic DTO parsing
    public String elementType = "activity";

    // Tool fields
    public String type;
    public String facilityId;
    public String linkId;
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
        
        if (activity.getLinkId() != null) {
            this.linkId = activity.getLinkId().toString();
        }

        if(activity.getEndTime().isDefined())this.endTime = activity.getEndTime().seconds();

        if (this.type != null) {
            this.ifInteractionActivity =
                    "pt interaction".equals(this.type) || "bike interaction".equals(this.type);
        }
    }

    @Override
    public Activity toBaseClass(Map<String, Object> context, ErrorMessages em) {
        if (!isVerified(em, context)) {
            return null;
        }

        Activity act;

        if (facilityId != null && !facilityId.trim().isEmpty()) {
            Id<ActivityFacility> fid = Id.create(facilityId.trim(), ActivityFacility.class);
            act = PopulationUtils.createActivityFromFacilityId(type.trim(), fid);
        } else {
            Id<Link> lid = Id.create(linkId.trim(), Link.class);
            act = PopulationUtils.createActivityFromLinkId(type.trim(), lid);
        }

        if (endTime != null) {
            act.setEndTime(endTime);
        }

        return act;
    }

    @Override
    public boolean isVerified(ErrorMessages em, Map<String,Object> context) {
    	boolean outcome = true;
        if (!"activity".equals(elementType)) {
            outcome = false;
            em.addErrorMessages("elementType is not activity.");
        }

        if (type != null) {
            String t = type.trim();
            if (!t.isEmpty() && !isAllowedType(t)) {
                em.addErrorMessages("Unknown activity type.");
                outcome = false;
            }
        }

        boolean hasFacility = facilityId != null && !facilityId.trim().isEmpty();
        boolean hasLink = linkId != null && !linkId.trim().isEmpty();

        if (!hasFacility && !hasLink) {
            em.addErrorMessages("Either facilityId or linkId must be present for activity type " + type);
            outcome = false;
        }

//        if (hasFacility && hasLink) {
//            em.addErrorMessages("Only one of facilityId or linkId should be present for activity type " + type);
//            outcome = false;
//        }

        String t = type.trim();
        if (!isAllowedType(t)) {
        	em.addErrorMessages("Unknown activity type.");
            outcome =  false;
        }


        if (endTime != null) {
            if (endTime < 0||endTime>100000) {
            	em.addErrorMessages("End time is negative or not in seconds.");
                outcome = false;
            }

        }

        return outcome;
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
                "MATSim Activity definition (tool argument). Required: elementType, type, and at least one of facilityId or linkId. "
                        + "If both are present, facilityId is prioritized when reconstructing the MATSim Activity. "
                        + "Optional: endTime (seconds since midnight)."
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
        typeEnum.add("home");
        typeEnum.add("work");
        typeEnum.add("education");
        typeEnum.add("shop");
        typeEnum.add("leisure");
        typeEnum.add("other");
        typeEnum.add("errands");


        // interaction / stage activities
        typeEnum.add("pt interaction");
        typeEnum.add("car interaction");
        typeEnum.add("bike interaction");
        typeEnum.add("car_passenger interaction");

        typeProp.add("enum", typeEnum);
        props.add("type", typeProp);

        JsonObject facProp = new JsonObject();
        facProp.addProperty("type", "string");
        facProp.addProperty("description", "MATSim facility id where the activity takes place. Preferred over linkId if both are provided.");
        props.add("facilityId", facProp);

        JsonObject linkProp = new JsonObject();
        linkProp.addProperty("type", "string");
        linkProp.addProperty("description", "MATSim link id where the activity takes place. Used when facilityId is absent.");
        props.add("linkId", linkProp);

        JsonObject endProp = new JsonObject();
        endProp.addProperty("type", "number");
        endProp.addProperty(
                "description",
                "End time of the activity in seconds since midnight. Example: 28800 = 8:00 AM. "
                        + "Typically omit for the last activity and for interaction activities."
        );
        props.add("endTime", endProp);

        schema.add("properties", props);

        JsonArray required = new JsonArray();
        required.add("elementType");
        required.add("type");
        schema.add("required", required);

        JsonArray anyOf = new JsonArray();

        JsonObject requireFacility = new JsonObject();
        JsonArray requireFacilityArr = new JsonArray();
        requireFacilityArr.add("facilityId");
        requireFacility.add("required", requireFacilityArr);
        anyOf.add(requireFacility);

        JsonObject requireLink = new JsonObject();
        JsonArray requireLinkArr = new JsonArray();
        requireLinkArr.add("linkId");
        requireLink.add("required", requireLinkArr);
        anyOf.add(requireLink);

        schema.add("anyOf", anyOf);

        return schema;
    }
    
    public static ActivityDTO fromJsonObject(JsonObject obj, Gson gson) {
        return gson.fromJson(obj, ActivityDTO.class);
    }

    private static boolean isAllowedType(String t) {
        return "home".equals(t)
                || "work".equals(t)
                || "education".equals(t)
                || "shop".equals(t)
                || "leisure".equals(t)
                || "other".equals(t)
                || "errands".equals(t)


                // stage / interaction activities
                || "pt interaction".equals(t)
                || "car interaction".equals(t)
                || "bike interaction".equals(t)
                || "car_passenger interaction".equals(t);
    }

    

    @Override
    public String getElementType() {
        return elementType;
    }
}