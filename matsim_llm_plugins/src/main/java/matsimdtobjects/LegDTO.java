package matsimdtobjects;

import java.util.Map;
import java.util.function.Function;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.pt.routes.TransitPassengerRoute;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import tools.ErrorMessages;

public class LegDTO extends PlanElementDTO<Leg> {

	public String elementType = "leg";

	public String mode;

	public String routingMode;

	public Double departureTimeSeconds;
	public Double travelTimeSeconds;

	// Optional
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
		this.routingMode = leg.getRoutingMode();
		this.departureTimeSeconds = optionalTimeToDouble(leg.getDepartureTime());
		this.travelTimeSeconds = optionalTimeToDouble(leg.getTravelTime());

		Route baseRoute = leg.getRoute();

		if (baseRoute != null) {
			if (baseRoute instanceof NetworkRoute) {
				this.route = new NetworkRouteDTO((NetworkRoute) baseRoute);
			} else if (baseRoute instanceof TransitPassengerRoute) {
				this.route = new TransitPassengerRouteDTO((TransitPassengerRoute) baseRoute);
			} else {
				this.route = new GenericRouteDTO(baseRoute);
			}
		}
	}

	@Override
	public Leg toBaseClass(Map<String, Object> context, ErrorMessages em) {
		if (!isVerified(em)) {
			return null;
		}

		Leg leg = PopulationUtils.createLeg(mode.trim());



		if (routingMode == null) {
			if ("pt".equals(mode) || "transit_walk".equals(mode)) {
				routingMode = "pt";
			} else {
				routingMode = mode;
			}
		}

		if (routingMode != null) {
			TripStructureUtils.setRoutingMode(leg, routingMode);
		}

		if (departureTimeSeconds != null) {
			leg.setDepartureTime(departureTimeSeconds);
		}

		if (travelTimeSeconds != null) {
			leg.setTravelTime(travelTimeSeconds);
		}

		if (route != null) {
			Route matsimRoute = route.toBaseClass(context, em);
			if (matsimRoute == null) {
				return null;
			}
			leg.setRoute(matsimRoute);
		}

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
		
		if (routingMode != null && !isAllowedRoutingMode(routingMode.trim())) {
		    outcome = false;
		    em.addErrorMessages("routingMode is not an allowed routing mode: " + routingMode);
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

		if (route != null && !route.isVerified(em)) {
			outcome = false;
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
				"MATSim leg definition with constrained mode values and an optional route."
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

		JsonObject routingModeProp = new JsonObject();
		routingModeProp.addProperty("type", "string");
		routingModeProp.addProperty("description", "Routing mode of the trip (important for MATSim simulation)");


		JsonArray routingModeEnum = new JsonArray();
		routingModeEnum.add("car");
		routingModeEnum.add("pt");
		routingModeEnum.add("bike");
		routingModeEnum.add("walk");
		routingModeProp.add("enum", routingModeEnum);
		props.add("routingMode", routingModeProp);

		JsonObject depProp = new JsonObject();
		depProp.addProperty("type", "number");
		depProp.addProperty("description", "Optional departure time in seconds.");
		props.add("departureTimeSeconds", depProp);

		JsonObject ttProp = new JsonObject();
		ttProp.addProperty("type", "number");
		ttProp.addProperty("description", "Optional travel time in seconds.");
		props.add("travelTimeSeconds", ttProp);

		JsonObject routeProp = RouteDTO.getJsonSchema();
		routeProp.addProperty("description", "Optional route object.");
		props.add("route", routeProp);

		schema.add("properties", props);

		JsonArray required = new JsonArray();
		required.add("elementType");
		required.add("mode");
		schema.add("required", required);

		return schema;
	}

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

		if (obj.has("routingMode") && !obj.get("routingMode").isJsonNull()) {
			dto.routingMode = obj.get("routingMode").getAsString();
		}

		if (obj.has("departureTimeSeconds") && !obj.get("departureTimeSeconds").isJsonNull()) {
			dto.departureTimeSeconds = obj.get("departureTimeSeconds").getAsDouble();
		}

		if (obj.has("travelTimeSeconds") && !obj.get("travelTimeSeconds").isJsonNull()) {
			dto.travelTimeSeconds = obj.get("travelTimeSeconds").getAsDouble();
		}

		if (obj.has("route") && !obj.get("route").isJsonNull()) {
			if (!obj.get("route").isJsonObject()) {
				throw new RuntimeException("route exists in LegDTO JSON but is not a JSON object");
			}

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
			case "generic":
				dto.route = GenericRouteDTO.fromJsonObject(routeObj, gson);
				break;
			default:
				throw new RuntimeException("Unknown routeType: " + routeType);
			}
		}

		return dto;
	}

	private static Double optionalTimeToDouble(OptionalTime t) {
		return (t != null && t.isDefined()) ? t.seconds() : null;
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
	
	private static boolean isAllowedRoutingMode(String routingMode) {
	    return "car".equals(routingMode)
	            || "pt".equals(routingMode)
	            || "bike".equals(routingMode)
	            || "walk".equals(routingMode);
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
		obj.addProperty("routingMode", routingMode);


		if (departureTimeSeconds != null) {
			obj.addProperty("departureTimeSeconds", departureTimeSeconds);
		}

		if (travelTimeSeconds != null) {
			obj.addProperty("travelTimeSeconds", travelTimeSeconds);
		}

		if (route != null) {
			obj.add("route", route.toJsonObject(gson));
		}

		return obj;
	}
}