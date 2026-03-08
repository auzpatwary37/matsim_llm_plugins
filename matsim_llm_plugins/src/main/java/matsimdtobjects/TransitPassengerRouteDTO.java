package matsimdtobjects;

import java.util.Map;
import java.util.function.Function;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.pt.routes.DefaultTransitPassengerRoute;
import org.matsim.pt.routes.TransitPassengerRoute;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class TransitPassengerRouteDTO extends RouteDTO<TransitPassengerRoute> {

    // Discriminator for nested polymorphic DTO parsing
    public String routeType = "transit_passenger";

    // MATSim PT route identifiers
    public String accessLinkId;
    public String egressLinkId;
    public String accessStopId;
    public String egressStopId;
    public String lineId;
    public String routeId;

    // Optional; handled only if supported by the MATSim version
    public String departureId;

    public TransitPassengerRouteDTO() {
        this.routeType = "transit_passenger";
    }

    public TransitPassengerRouteDTO(TransitPassengerRoute route) {
        this.routeType = "transit_passenger";

        if (route == null) {
            return;
        }

        if (route.getStartLinkId() != null) {
            this.accessLinkId = route.getStartLinkId().toString();
        }
        if (route.getEndLinkId() != null) {
            this.egressLinkId = route.getEndLinkId().toString();
        }
        if (route.getAccessStopId() != null) {
            this.accessStopId = route.getAccessStopId().toString();
        }
        if (route.getEgressStopId() != null) {
            this.egressStopId = route.getEgressStopId().toString();
        }
        if (route.getLineId() != null) {
            this.lineId = route.getLineId().toString();
        }
        if (route.getRouteId() != null) {
            this.routeId = route.getRouteId().toString();
        }

        this.departureId = safeGetDepartureId(route);
    }

    @Override
    public TransitPassengerRoute toBaseClass(Map<String, Object> context) {
        if (!isVerified()) {
            return null;
        }

        Id<Link> startLink = Id.createLinkId(accessLinkId.trim());
        Id<Link> endLink = Id.createLinkId(egressLinkId.trim());

        Id<TransitStopFacility> accessStop = Id.create(accessStopId.trim(), TransitStopFacility.class);
        Id<TransitStopFacility> egressStop = Id.create(egressStopId.trim(), TransitStopFacility.class);

        Id<TransitLine> transitLine = Id.create(lineId.trim(), TransitLine.class);
        Id<TransitRoute> transitRoute = Id.create(routeId.trim(), TransitRoute.class);

        TransitPassengerRoute ptRoute = new DefaultTransitPassengerRoute(
                startLink,
                endLink,
                accessStop,
                egressStop,
                transitLine,
                transitRoute
        );

        if (departureId != null && !departureId.trim().isEmpty()) {
            safeSetDepartureId(ptRoute, departureId.trim());
        }

        return ptRoute;
    }

    @Override
    public boolean isVerified() {
        return "transit_passenger".equals(routeType)
                && isNonBlank(accessLinkId)
                && isNonBlank(egressLinkId)
                && isNonBlank(accessStopId)
                && isNonBlank(egressStopId)
                && isNonBlank(lineId)
                && isNonBlank(routeId)
                && (departureId == null || isNonBlank(departureId));
    }

    public static Function<TransitPassengerRoute, TransitPassengerRouteDTO> toDTOFromBaseObject() {
        return TransitPassengerRouteDTO::new;
    }

    public static JsonObject getJsonSchema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        schema.addProperty("additionalProperties", false);
        schema.addProperty(
                "description",
                "MATSim TransitPassengerRoute definition for a public transport leg. "
                        + "Includes start/end network link ids, boarding/alighting stop ids, transit line id, transit route id, "
                        + "and optionally a departure id if supported by the MATSim version."
        );

        JsonObject properties = new JsonObject();

        JsonObject routeType = new JsonObject();
        routeType.addProperty("type", "string");
        routeType.addProperty("description", "Discriminator for the MATSim route subtype.");
        JsonArray routeTypeEnum = new JsonArray();
        routeTypeEnum.add("transit_passenger");
        routeType.add("enum", routeTypeEnum);
        properties.add("routeType", routeType);

        JsonObject accessLink = new JsonObject();
        accessLink.addProperty("type", "string");
        accessLink.addProperty("description", "Network link id where the PT route starts.");
        properties.add("accessLinkId", accessLink);

        JsonObject egressLink = new JsonObject();
        egressLink.addProperty("type", "string");
        egressLink.addProperty("description", "Network link id where the PT route ends.");
        properties.add("egressLinkId", egressLink);

        JsonObject accessStop = new JsonObject();
        accessStop.addProperty("type", "string");
        accessStop.addProperty("description", "Boarding stop facility id.");
        properties.add("accessStopId", accessStop);

        JsonObject egressStop = new JsonObject();
        egressStop.addProperty("type", "string");
        egressStop.addProperty("description", "Alighting stop facility id.");
        properties.add("egressStopId", egressStop);

        JsonObject line = new JsonObject();
        line.addProperty("type", "string");
        line.addProperty("description", "Transit line id.");
        properties.add("lineId", line);

        JsonObject route = new JsonObject();
        route.addProperty("type", "string");
        route.addProperty("description", "Transit route id within the transit line.");
        properties.add("routeId", route);

        JsonObject departure = new JsonObject();
        departure.addProperty("type", "string");
        departure.addProperty("description", "Optional departure id. Omit if unknown or unsupported.");
        properties.add("departureId", departure);

        schema.add("properties", properties);

        JsonArray required = new JsonArray();
        required.add("routeType");
        required.add("accessLinkId");
        required.add("egressLinkId");
        required.add("accessStopId");
        required.add("egressStopId");
        required.add("lineId");
        required.add("routeId");
        schema.add("required", required);

        return schema;
    }

    private static boolean isNonBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private static String safeGetDepartureId(TransitPassengerRoute route) {
        try {
            java.lang.reflect.Method method = route.getClass().getMethod("getDepartureId");
            Object value = method.invoke(route);
            return value == null ? null : value.toString();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void safeSetDepartureId(TransitPassengerRoute route, String departureIdStr) {
        try {
            java.lang.reflect.Method method = route.getClass().getMethod("setDepartureId", Id.class);
            @SuppressWarnings("unchecked")
            Id<Departure> departureId = (Id<Departure>) (Id<?>) Id.create(departureIdStr, Departure.class);
            method.invoke(route, departureId);
        } catch (NoSuchMethodException e) {
            try {
                java.lang.reflect.Method method = route.getClass().getMethod("setDepartureId", String.class);
                method.invoke(route, departureIdStr);
            } catch (Throwable ignored) {
                // unsupported in this MATSim version
            }
        } catch (Throwable ignored) {
            // unsupported in this MATSim version
        }
    }

    @Override
    public String getRouteType() {
        return routeType;
    }
}