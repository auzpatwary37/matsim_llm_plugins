package matsimdtobjects;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.PopulationUtils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import tools.ErrorMessages;
import tools.ToolArgumentDTO;

public class PlanDTO extends ToolArgumentDTO<Plan> {

    public List<PlanElementDTO<?>> elements = new ArrayList<>();

    public PlanDTO() {
    }

    public PlanDTO(Plan plan) {
        if (plan == null || plan.getPlanElements() == null) return;

        plan.getPlanElements().forEach(pe -> {
            if (pe instanceof Activity activity) {
                elements.add(new ActivityDTO(activity));
            } else if (pe instanceof Leg leg) {
                elements.add(new LegDTO(leg));
            }
        });
    }
    
//    @Override
//    public void afterJsonLoad(String json, Gson gson) {
//        JsonObject obj = gson.fromJson(json, JsonObject.class);
//
//        this.elements = new ArrayList<>();
//
//        JsonArray arr = obj.getAsJsonArray("elements");
//        for (JsonElement el : arr) {
//            JsonObject elemObj = el.getAsJsonObject();
//
//            if (!elemObj.has("elementType")) {
//                throw new RuntimeException("Missing elementType in plan element");
//            }
//
//            String elementType = elemObj.get("elementType").getAsString();
//
//            switch (elementType) {
//                case "activity":
//                    this.elements.add(gson.fromJson(elemObj, ActivityDTO.class));
//                    break;
//                case "leg":
//                    LegDTO leg = gson.fromJson(elemObj, LegDTO.class);
//                    leg.afterJsonLoad(elemObj.toString(), gson);
//                    this.elements.add(leg);
//                    break;
//                default:
//                    throw new RuntimeException("Unknown elementType: " + elementType);
//            }
//        }
//    }
    
    public static PlanDTO fromJsonObject(JsonObject obj, Gson gson) {
        PlanDTO dto = new PlanDTO();

        // Initialize list
        dto.elements = new ArrayList<>();

        // Validate elements
        if (!obj.has("elements") || !obj.get("elements").isJsonArray()) {
            throw new RuntimeException("PlanDTO: missing or invalid 'elements' array");
        }

        JsonArray arr = obj.getAsJsonArray("elements");

        for (JsonElement el : arr) {
            if (!el.isJsonObject()) {
                throw new RuntimeException("PlanDTO: element is not a JSON object");
            }

            JsonObject elemObj = el.getAsJsonObject();

            if (!elemObj.has("elementType")) {
                throw new RuntimeException("PlanDTO: missing elementType in plan element");
            }

            String elementType = elemObj.get("elementType").getAsString();

            switch (elementType) {

                case "activity":
                    dto.elements.add(ActivityDTO.fromJsonObject(elemObj, gson));
                    break;

                case "leg":
                    dto.elements.add(LegDTO.fromJsonObject(elemObj, gson));
                    break;

                default:
                    throw new RuntimeException("PlanDTO: unknown elementType: " + elementType);
            }
        }
        checkAndFixRoutingMode(dto);
        return dto;
    }
    
    private static void checkAndFixRoutingMode(PlanDTO dto) {

        if (dto.elements == null || dto.elements.isEmpty()) return;

        for (int i = 0; i < dto.elements.size(); i++) {

            if (!(dto.elements.get(i) instanceof LegDTO leg)) continue;

            String mode = leg.mode;
            boolean isPtChain = false;

            if (i > 0 && dto.elements.get(i - 1) instanceof ActivityDTO prevAct) {
                if ("pt interaction".equals(prevAct.type)) {
                    isPtChain = true;
                }
            }

            if (i < dto.elements.size() - 1 && dto.elements.get(i + 1) instanceof ActivityDTO nextAct) {
                if ("pt interaction".equals(nextAct.type)) {
                    isPtChain = true;
                }
            }

            if ("pt".equals(mode)) {
                leg.routingMode = "pt";
            } else if ("walk".equals(mode) && isPtChain) {
                leg.routingMode = "pt";
            } else if (leg.routingMode == null) {
                leg.routingMode = mode;
            }
        }
    }
    
    @Override
    public JsonObject toJsonObject(Gson gson) {
        JsonObject obj = new JsonObject();
        JsonArray arr = new JsonArray();

        if (elements != null) {
            for (PlanElementDTO<?> element : elements) {
                if (element == null) {
                    continue;
                }
                arr.add(element.toJsonObject(gson));
            }
        }

        obj.add("elements", arr);
        return obj;
    }

    @Override
    public Plan toBaseClass(Map<String, Object> context, ErrorMessages em) {
        if (!isVerified(em)) return null;

        Plan plan = PopulationUtils.createPlan();

        for (PlanElementDTO<?> elementDTO : elements) {
            if (elementDTO == null || !elementDTO.isVerified(em)) {
                return null;
            }

            Object baseObj = elementDTO.toBaseClass(context,em);

            if (baseObj instanceof Activity activity) {
                plan.addActivity(activity);
            } else if (baseObj instanceof Leg leg) {
                plan.addLeg(leg);
            } else {
                return null;
            }
        }

        return plan;
    }

    @Override
    public boolean isVerified(ErrorMessages em) {
        boolean outcome = true;

        if (elements == null || elements.isEmpty()) {
            outcome = false;
            em.addErrorMessages("Plan elements are null or empty.");
            return outcome;
        }

        for (int i = 0; i < elements.size(); i++) {
            PlanElementDTO<?> element = elements.get(i);

            if (element == null) {
                outcome = false;
                em.addErrorMessages("Plan element at position " + i + " is null.");
            } else if (!element.isVerified(em)) {
                outcome = false;
            }
        }

        // MATSim plans should usually start and end with activities
        if (!(elements.get(0) instanceof ActivityDTO)) {
            outcome = false;
            em.addErrorMessages("Plan must start with an ActivityDTO.");
        }

        if (!(elements.get(elements.size() - 1) instanceof ActivityDTO)) {
            outcome = false;
            em.addErrorMessages("Plan must end with an ActivityDTO.");
        }

        // Must alternate Activity-Leg-Activity-Leg...
        for (int i = 0; i < elements.size(); i++) {
            PlanElementDTO<?> el = elements.get(i);

            if (i % 2 == 0) {
                if (!(el instanceof ActivityDTO)) {
                    outcome = false;
                    em.addErrorMessages("Plan element at position " + i + " must be an ActivityDTO.");
                }
            } else {
                if (!(el instanceof LegDTO)) {
                    outcome = false;
                    em.addErrorMessages("Plan element at position " + i + " must be a LegDTO.");
                }
            }
        }

        return outcome;
    }

    public static Function<Plan, PlanDTO> toDTOFromBaseObject() {
        return PlanDTO::new;
    }

    public static JsonObject getJsonSchema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        schema.addProperty("additionalProperties", false);
        schema.addProperty(
            "description",
            "MATSim plan represented as an ordered list of plan elements. "
          + "Elements must alternate as Activity, Leg, Activity, Leg, and so on. "
          + "The first and last elements must be Activity objects."
        );

        JsonObject props = new JsonObject();

        JsonObject elementsProp = new JsonObject();
        elementsProp.addProperty("type", "array");
        elementsProp.addProperty(
            "description",
            "Ordered list of plan elements forming the MATSim plan."
        );

        JsonObject items = new JsonObject();
        JsonArray oneOf = new JsonArray();
        oneOf.add(ActivityDTO.getJsonSchema());
        oneOf.add(LegDTO.getJsonSchema());
        items.add("oneOf", oneOf);

        elementsProp.add("items", items);
        props.add("elements", elementsProp);

        schema.add("properties", props);

        JsonArray required = new JsonArray();
        required.add("elements");
        schema.add("required", required);

        return schema;
    }
}