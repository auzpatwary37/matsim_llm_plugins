package matsimdtobjects;

import java.util.Map;
import java.util.function.Function;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.pt.routes.TransitPassengerRoute;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import tools.ErrorMessages;

public class LegDTO extends PlanElementDTO<Leg> {

    // Discriminator for nested polymorphic DTO parsing
    public String elementType = "leg";

    public String mode;

    // Optional
    public Double departureTimeSeconds;
    public Double travelTimeSeconds;

    // Required in your current schema/validation design
    public RouteDTO<?> route;

    public LegDTO() {
        this.elementType = "leg";
    }

    public LegDTO(Leg leg) {
        this.elementType = "leg";

        if (leg == null) {
            return;
        }

        this.mode = leg.getMode();
        this.departureTimeSeconds = safeOptionalTimeToDouble(leg.getDepartureTime());
        this.travelTimeSeconds = safeOptionalTimeToDouble(leg.getTravelTime());

        Route baseRoute = leg.getRoute();

        if (baseRoute != null) {
            if (baseRoute instanceof NetworkRoute) {
                this.route = new NetworkRouteDTO((NetworkRoute) baseRoute);
            } else if (baseRoute instanceof TransitPassengerRoute) {
                this.route = new TransitPassengerRouteDTO((TransitPassengerRoute) baseRoute);
            } else {
                throw new RuntimeException(
                        "Unsupported MATSim route type: " + baseRoute.getClass().getName()
                );
            }
        }
    }

    @Override
    public Leg toBaseClass(Map<String, Object> context, ErrorMessages em) {
        if (!isVerified(em)) {
            return null;
        }

        Leg leg = PopulationUtils.createLeg(mode.trim());

        if (departureTimeSeconds != null) {
            safeSetDepartureTime(leg, departureTimeSeconds);
        }

        if (travelTimeSeconds != null) {
            safeSetTravelTime(leg, travelTimeSeconds);
        }

        Route matsimRoute = route.toBaseClass(context,em);
        if (matsimRoute == null) {
            return null;
        }
        leg.setRoute(matsimRoute);

        return leg;
    }

    @Override
    public boolean isVerified(ErrorMessages em) {
        boolean outcome = true;

        if (!"leg".equals(elementType)) {
            outcome = false;
            em.addErrorMessages("elementType is not leg.");
        }

        if (!isNonBlank(mode)) {
            outcome = false;
            em.addErrorMessages("mode is not defined for leg.");
        }

        if (mode != null && !isAllowedMode(mode.trim())) {
            outcome = false;
            em.addErrorMessages("mode is not an allowed leg mode: " + mode);
        }

        if (departureTimeSeconds != null && departureTimeSeconds < 0) {
            outcome = false;
            em.addErrorMessages("departureTimeSeconds is negative.");
        }

        if (travelTimeSeconds != null && travelTimeSeconds < 0) {
            outcome = false;
            em.addErrorMessages("travelTimeSeconds is negative.");
        }

        if (route == null) {
            outcome = false;
            em.addErrorMessages("route is not defined for leg.");
        } else {
            if (!route.isVerified(em)) {
                outcome = false;
            }
        }

        return outcome;
    }

    public static Function<Leg, LegDTO> toDTOFromBaseObject() {
        return LegDTO::new;
    }

    public static JsonObject getJsonSchema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        schema.addProperty("additionalProperties", false);
        schema.addProperty(
                "description",
                "MATSim leg definition with constrained mode values and a required route."
        );

        JsonObject props = new JsonObject();

        JsonObject elementTypeProp = new JsonObject();
        elementTypeProp.addProperty("type", "string");
        elementTypeProp.addProperty("description", "Discriminator for the MATSim plan element subtype.");
        JsonArray elementTypeEnum = new JsonArray();
        elementTypeEnum.add("leg");
        elementTypeProp.add("enum", elementTypeEnum);
        props.add("elementType", elementTypeProp);

        JsonObject modeProp = new JsonObject();
        modeProp.addProperty("type", "string");
        modeProp.addProperty("description", "Mode of the leg.");
        JsonArray modeEnum = new JsonArray();
        modeEnum.add("car");
        modeEnum.add("pt");
        modeEnum.add("car_passenger");
        modeEnum.add("bike");
        modeEnum.add("walk");
        modeEnum.add("transit_walk");
        modeProp.add("enum", modeEnum);
        props.add("mode", modeProp);

        JsonObject depProp = new JsonObject();
        depProp.addProperty("type", "number");
        depProp.addProperty("description", "Optional departure time in seconds.");
        props.add("departureTimeSeconds", depProp);

        JsonObject ttProp = new JsonObject();
        ttProp.addProperty("type", "number");
        ttProp.addProperty("description", "Optional travel time in seconds.");
        props.add("travelTimeSeconds", ttProp);

        JsonObject routeProp = RouteDTO.getJsonSchema();
        routeProp.addProperty("description", "Required route object.");
        props.add("route", routeProp);

        schema.add("properties", props);

        JsonArray required = new JsonArray();
        required.add("elementType");
        required.add("mode");
        required.add("route");
        schema.add("required", required);

        return schema;
    }
    
//    @Override
//    public void afterJsonLoad(String json, Gson gson) {
//        JsonObject obj = gson.fromJson(json, JsonObject.class);
//
//        if (obj.has("route") && obj.get("route").isJsonObject()) {
//            JsonObject routeObj = obj.getAsJsonObject("route");
//
//            if (!routeObj.has("routeType")) {
//                throw new RuntimeException("Missing routeType in leg route");
//            }
//
//            String routeType = routeObj.get("routeType").getAsString();
//
//            switch (routeType) {
//                case "network":
//                    this.route = gson.fromJson(routeObj, NetworkRouteDTO.class);
//                    break;
//                case "transit_passenger":
//                    this.route = gson.fromJson(routeObj, TransitPassengerRouteDTO.class);
//                    break;
//                default:
//                    throw new RuntimeException("Unknown routeType: " + routeType);
//            }
//        }
//    }
    
    public static LegDTO fromJsonObject(JsonObject obj, Gson gson) {
        LegDTO dto = new LegDTO();

        if (obj == null) {
            throw new RuntimeException("LegDTO JSON object is null");
        }

        if (obj.has("elementType") && !obj.get("elementType").isJsonNull()) {
            dto.elementType = obj.get("elementType").getAsString();
        } else {
            dto.elementType = "leg";
        }

        if (obj.has("mode") && !obj.get("mode").isJsonNull()) {
            dto.mode = obj.get("mode").getAsString();
        }

        if (obj.has("departureTimeSeconds") && !obj.get("departureTimeSeconds").isJsonNull()) {
            dto.departureTimeSeconds = obj.get("departureTimeSeconds").getAsDouble();
        }

        if (obj.has("travelTimeSeconds") && !obj.get("travelTimeSeconds").isJsonNull()) {
            dto.travelTimeSeconds = obj.get("travelTimeSeconds").getAsDouble();
        }

        if (obj.has("route") && obj.get("route").isJsonObject()) {
            JsonObject routeObj = obj.getAsJsonObject("route");

            if (!routeObj.has("routeType") || routeObj.get("routeType").isJsonNull()) {
                throw new RuntimeException("Missing routeType in leg route");
            }

            String routeType = routeObj.get("routeType").getAsString();

            switch (routeType) {
                case "network":
                    dto.route = NetworkRouteDTO.fromJsonObject(routeObj, gson);
                    break;

                case "transit_passenger":
                    dto.route = TransitPassengerRouteDTO.fromJsonObject(routeObj, gson);
                    break;

                default:
                    throw new RuntimeException("Unknown routeType: " + routeType);
            }
        }

        return dto;
    }

    private static boolean isNonBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private static boolean isAllowedMode(String mode) {
        return "car".equals(mode)
                || "pt".equals(mode)
                || "car_passenger".equals(mode)
                || "bike".equals(mode)
                || "walk".equals(mode)
                || "transit_walk".equals(mode);
    }

    private static Double safeOptionalTimeToDouble(Object optionalTime) {
        if (optionalTime == null) {
            return null;
        }

        try {
            java.lang.reflect.Method isDefined = optionalTime.getClass().getMethod("isDefined");
            Object defined = isDefined.invoke(optionalTime);
            if (defined instanceof Boolean && !((Boolean) defined)) {
                return null;
            }
        } catch (Throwable ignored) {
        }

        try {
            java.lang.reflect.Method seconds = optionalTime.getClass().getMethod("seconds");
            Object value = seconds.invoke(optionalTime);
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
        } catch (Throwable ignored) {
        }

        try {
            java.lang.reflect.Method orElse = optionalTime.getClass().getMethod("orElse", double.class);
            Object value = orElse.invoke(optionalTime, Double.NaN);
            if (value instanceof Number) {
                double v = ((Number) value).doubleValue();
                return Double.isNaN(v) ? null : v;
            }
        } catch (Throwable ignored) {
        }

        return null;
    }

    private static void safeSetDepartureTime(Leg leg, double seconds) {
        try {
            java.lang.reflect.Method method = leg.getClass().getMethod("setDepartureTime", double.class);
            method.invoke(leg, seconds);
            return;
        } catch (Throwable ignored) {
        }

        try {
            Class<?> optionalTimeClass = Class.forName("org.matsim.core.utils.misc.OptionalTime");
            java.lang.reflect.Method definedMethod = optionalTimeClass.getMethod("defined", double.class);
            Object optionalTime = definedMethod.invoke(null, seconds);

            java.lang.reflect.Method method = leg.getClass().getMethod("setDepartureTime", optionalTimeClass);
            method.invoke(leg, optionalTime);
        } catch (Throwable ignored) {
        }
    }

    private static void safeSetTravelTime(Leg leg, double seconds) {
        try {
            java.lang.reflect.Method method = leg.getClass().getMethod("setTravelTime", double.class);
            method.invoke(leg, seconds);
            return;
        } catch (Throwable ignored) {
        }

        try {
            Class<?> optionalTimeClass = Class.forName("org.matsim.core.utils.misc.OptionalTime");
            java.lang.reflect.Method definedMethod = optionalTimeClass.getMethod("defined", double.class);
            Object optionalTime = definedMethod.invoke(null, seconds);

            java.lang.reflect.Method method = leg.getClass().getMethod("setTravelTime", optionalTimeClass);
            method.invoke(leg, optionalTime);
        } catch (Throwable ignored) {
        }
    }

    @Override
    public String getElementType() {
        return elementType;
    }
    
    @Override
    public JsonObject toJsonObject(Gson gson) {
        JsonObject obj = new JsonObject();

        obj.addProperty("elementType", elementType);
        obj.addProperty("mode", mode);

        if (departureTimeSeconds != null) {
            obj.addProperty("departureTimeSeconds", departureTimeSeconds);
        }

        if (travelTimeSeconds != null) {
            obj.addProperty("travelTimeSeconds", travelTimeSeconds);
        }

        if (route != null) {
            if (route instanceof NetworkRouteDTO) {
                obj.add("route", ((NetworkRouteDTO) route).toJsonObject(gson));
            } else if (route instanceof TransitPassengerRouteDTO) {
                obj.add("route", ((TransitPassengerRouteDTO) route).toJsonObject(gson));
            } else {
                throw new RuntimeException("Unsupported route DTO subtype: " + route.getClass().getName());
            }
        }

        return obj;
    }
}