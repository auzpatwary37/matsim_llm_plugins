package tools.Implement;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripRouter;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Provider;

import matsimdtobjects.PlanDTO;
import rag.IVectorDB;
import tools.DefaultToolResponse;
import tools.ErrorMessages;
import tools.ITool;
import tools.IToolResponse;
import tools.SimpleDoubleDTO;
import tools.SimpleStringDTO;
import tools.ToolArgument;
import tools.ToolArgumentDTO;
import tools.VerificationFailedException;

public class RouterTool implements ITool<Plan> {

    private final Map<String, ToolArgument<?, ? extends ToolArgumentDTO<?>>> arguments = new HashMap<>();


    public RouterTool() {
        registerArgument(SimpleStringDTO.forArgument("fromFacilityId"));
        registerArgument(SimpleStringDTO.forArgument("toFacilityId"));
        registerArgument(SimpleStringDTO.forArgument("mode"));
        registerArgument(SimpleDoubleDTO.forArgument("departureTimeSeconds"));
    }

    @Override
    public String getName() {
        return "router_tool";
    }

    @Override
    public Class<Plan> getOutputClass() {
        return Plan.class;
    }

    @Override
    public String getDescription() {
        return "Routes one trip between two MATSim facilities using the MATSim TripRouter. "
             + "Returns only the routed trip sub-chain between the two surrounding real activities, not a full plan. "
             + "The returned elements usually start with a leg and end with a leg. "
             + "For simple modes, the result may contain a single leg. "
             + "For public transport, the result may contain multiple legs and pt interaction activities. "
             + "You must insert the returned sub-chain exactly between the original before-activity and after-activity to reconstruct the full plan. "
             + "Do not add, remove, reorder, or simplify returned elements.";
    }

    @Override
    public boolean isDummy() {
        return false;
    }

    @Override
    public Map<String, ToolArgument<?, ? extends ToolArgumentDTO<?>>> getRegisteredArguments() {
        return arguments;
    }

    @Override
    public JsonObject getJsonSchema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("name", getName());
        schema.addProperty("description", getDescription());

        JsonObject parameters = new JsonObject();
        parameters.addProperty("type", "object");

        JsonObject properties = new JsonObject();
        properties.add("fromFacilityId", SimpleStringDTO.STATIC_SCHEMA);
        properties.add("toFacilityId", SimpleStringDTO.STATIC_SCHEMA);
        properties.add("mode", SimpleStringDTO.STATIC_SCHEMA);
        properties.add("departureTimeSeconds", SimpleDoubleDTO.STATIC_SCHEMA);

        JsonArray required = new JsonArray();
        required.add("fromFacilityId");
        required.add("toFacilityId");
        required.add("mode");
        required.add("departureTimeSeconds");

        parameters.add("properties", properties);
        parameters.add("required", required);

        schema.add("parameters", parameters);
        return schema;
    }

    @Override
    public IToolResponse<Plan> callTool(String id, Map<String, Object> arguments, IVectorDB vectorDB, Map<String,Object> contextObject) {
        String fromFacilityId = (String) arguments.get("fromFacilityId");
        String toFacilityId = (String) arguments.get("toFacilityId");
        String mode = (String) arguments.get("mode");
        Double departureTimeSeconds = (Double) arguments.get("departureTimeSeconds");

        if (departureTimeSeconds == null) {
            departureTimeSeconds = 0.0;
        }

        ActivityFacilities facilities = getFacilitiesFromContext(contextObject);
        Provider<TripRouter> tripRouter = getTripRouterProviderFromContext(contextObject);
        Person person = getOptionalPersonFromContext(contextObject);

        ActivityFacility fromFacility = facilities.getFacilities().get(
            Id.create(fromFacilityId.trim(), ActivityFacility.class)
        );
        ActivityFacility toFacility = facilities.getFacilities().get(
            Id.create(toFacilityId.trim(), ActivityFacility.class)
        );

        if (fromFacility == null) {
            throw new RuntimeException("Origin facility not found: " + fromFacilityId);
        }
        if (toFacility == null) {
            throw new RuntimeException("Destination facility not found: " + toFacilityId);
        }

        List<? extends PlanElement> routedElements = tripRouter.get().calcRoute(mode, fromFacility, toFacility, 
        		departureTimeSeconds, person, null);
        
//        List<? extends PlanElement> routedElements = invokeTripRouterCalcRoute(
//            tripRouter,
//            mode.trim(),
//            fromFacility,
//            toFacility,
//            departureTimeSeconds,
//            person
//        );

        if (routedElements == null || routedElements.isEmpty()) {
            throw new RuntimeException("TripRouter returned no plan elements.");
        }

        Plan routedPlan = PopulationUtils.createPlan();

        for (PlanElement pe : routedElements) {
            if (pe instanceof Activity) {
                routedPlan.addActivity((Activity) pe);
            } else if (pe instanceof Leg) {
                routedPlan.addLeg((Leg) pe);
            } else {
                throw new RuntimeException("Unsupported routed plan element type: " + pe.getClass().getName());
            }
        }

        Gson gson = new Gson();
        JsonObject response = new PlanDTO(routedPlan).toJsonObject(gson);

        return new DefaultToolResponse<>(
            id,
            getName(),
            response.toString(),
            routedPlan,
            false
        );
    }

    @Override
    public void verifyArguments(Map<String, Object> arguments, Map<String, Object> context, ErrorMessages em)
            throws VerificationFailedException {

        List<String> errors = new ArrayList<>();

        if (arguments == null) {
            errors.add("Arguments map is null.");
        } else {
            String fromFacilityId = (String) arguments.get("fromFacilityId");
            String toFacilityId = (String) arguments.get("toFacilityId");
            String mode = (String) arguments.get("mode");
            Double departureTimeSeconds = (Double) arguments.get("departureTimeSeconds");

            if (fromFacilityId == null || fromFacilityId.trim().isEmpty()) {
                errors.add("Missing or empty fromFacilityId.");
            }

            if (toFacilityId == null || toFacilityId.trim().isEmpty()) {
                errors.add("Missing or empty toFacilityId.");
            }

            if (mode == null || mode.trim().isEmpty()) {
                errors.add("Missing or empty mode.");
            } else if (!isAllowedMode(mode.trim())) {
                errors.add("Unsupported routing mode: " + mode
                        + ". Supported modes are car, pt, car_passenger, bike, walk, transit_walk.");
            }

            if (departureTimeSeconds != null && departureTimeSeconds < 0) {
                errors.add("departureTimeSeconds cannot be negative.");
            }

            if (context == null) {
                errors.add("Tool context is null.");
            } else {
                Object tripRouterObj = getContextValue(context, "tripRoutersProvider");
                if (tripRouterObj == null) {
                    errors.add("Context does not contain TripRouterProvider under key 'tripRouterProvider' or 'router'.");
                }

                Object facilitiesObj = getContextValue(context, "activityFacilities", "facilities");
                if (!(facilitiesObj instanceof ActivityFacilities)) {
                    errors.add("Context does not contain ActivityFacilities under key 'activityFacilities' or 'facilities'.");
                } else {
                    ActivityFacilities facilities = (ActivityFacilities) facilitiesObj;

                    if (fromFacilityId != null && !fromFacilityId.trim().isEmpty()) {
                        if (!facilities.getFacilities().containsKey(
                                Id.create(fromFacilityId.trim(), ActivityFacility.class))) {
                            errors.add("Origin facilityId not found: " + fromFacilityId);
                        }
                    }

                    if (toFacilityId != null && !toFacilityId.trim().isEmpty()) {
                        if (!facilities.getFacilities().containsKey(
                                Id.create(toFacilityId.trim(), ActivityFacility.class))) {
                            errors.add("Destination facilityId not found: " + toFacilityId);
                        }
                    }
                }
            }
        }

        em.getErrorMessages().addAll(errors);

        if (!errors.isEmpty()) {
            throw new VerificationFailedException(errors);
        }
    }

 

    private ActivityFacilities getFacilitiesFromContext(Map<String,Object> contextObject) {
        Object obj = getContextValue(contextObject, "activityFacilities", "facilities");
        if (!(obj instanceof ActivityFacilities)) {
            throw new RuntimeException(
                "RouterTool requires ActivityFacilities in context under key 'activityFacilities' or 'facilities'."
            );
        }
        return (ActivityFacilities) obj;
    }

    private Provider<TripRouter> getTripRouterProviderFromContext(Map<String,Object> contextObject) {
    	Provider<TripRouter> obj = (Provider<TripRouter>)getContextValue(contextObject, "tripRoutersProvider");
        if (obj == null) {
            throw new RuntimeException(
                "RouterTool requires TripRouter in context under key 'tripRouter' or 'router'."
            );
        }
        return obj;
    }

    private Person getOptionalPersonFromContext(Map<String, Object> context) {
        Object obj = getContextValue(context, "person");
        if (obj instanceof Person) {
            return (Person) obj;
        }
        return null;
    }

    private Object getContextValue(Map<String, Object> context, String... keys) {
        if (context == null) {
            return null;
        }
        for (String key : keys) {
            if (context.containsKey(key)) {
                return context.get(key);
            }
        }
        return null;
    }

    private boolean isAllowedMode(String mode) {
        return "car".equals(mode)
                || "pt".equals(mode)
                || "car_passenger".equals(mode)
                || "bike".equals(mode)
                || "walk".equals(mode)
                || "transit_walk".equals(mode);
    }
}
   