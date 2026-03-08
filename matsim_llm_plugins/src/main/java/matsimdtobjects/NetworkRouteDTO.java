package matsimdtobjects;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class NetworkRouteDTO extends RouteDTO<NetworkRoute> {

    // Keep it dead-simple for tool calling:
    // startLinkId, endLinkId, and intermediate link ids (can be empty).
    public String startLinkId;
    public String endLinkId;
    public List<String> linkIds = new ArrayList<>();

    // discriminator for polymorphic JSON parsing
    public final String routeType = "network";

    public NetworkRouteDTO() {}

    public NetworkRouteDTO(NetworkRoute route) {
        if (route == null) return;

        if (route.getStartLinkId() != null) this.startLinkId = route.getStartLinkId().toString();
        if (route.getEndLinkId() != null) this.endLinkId = route.getEndLinkId().toString();

        // intermediate link ids (may be empty)
        if (route.getLinkIds() != null) {
            route.getLinkIds().forEach(id -> this.linkIds.add(id.toString()));
        }
    }

    @Override
    public NetworkRoute toBaseClass(Map<String, Object> context) {
        if (!isVerified()) return null;
        
        Id<Link> start = Id.createLinkId(startLinkId);
        Id<Link> end = Id.createLinkId(endLinkId);

        // Create a concrete NetworkRoute implementation (no network needed for construction)
        NetworkRoute r = RouteUtils.createLinkNetworkRouteImpl(start, end);
        

        // Convert intermediate ids
        List<Id<Link>> mids = new ArrayList<>();
        if (linkIds != null) {
            for (String s : linkIds) {
                if (s != null && !s.trim().isEmpty()) {
                    mids.add(Id.createLinkId(s.trim()));
                }
            }
        }

        r.setLinkIds(start, mids, end);
        return r;
    }

    @Override
    public boolean isVerified() {
    	if (!"network".equals(routeType)) return false;
        if (startLinkId == null || startLinkId.trim().isEmpty()) return false;
        if (endLinkId == null || endLinkId.trim().isEmpty()) return false;

        // linkIds can be empty; if present, must not contain blanks
        if (linkIds != null) {
            for (String s : linkIds) {
                if (s == null || s.trim().isEmpty()) return false;
            }
        }
        return true;
    }

    public static Function<NetworkRoute, NetworkRouteDTO> toDTOFromBaseObject() {
        return NetworkRouteDTO::new;
    }

    public static JsonObject getJsonSchema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        schema.addProperty("additionalProperties", false);
        schema.addProperty(
            "description",
            "MATSim NetworkRoute definition. Provide routeType='network', startLinkId, endLinkId, and optional intermediate linkIds."
        );

        JsonObject props = new JsonObject();

        JsonObject routeTypeProp = new JsonObject();
        routeTypeProp.addProperty("type", "string");
        routeTypeProp.addProperty("description", "Discriminator for the MATSim route subtype.");
        JsonArray routeTypeEnum = new JsonArray();
        routeTypeEnum.add("network");
        routeTypeProp.add("enum", routeTypeEnum);
        props.add("routeType", routeTypeProp);

        JsonObject startProp = new JsonObject();
        startProp.addProperty("type", "string");
        startProp.addProperty("description", "Route start link id.");
        props.add("startLinkId", startProp);

        JsonObject endProp = new JsonObject();
        endProp.addProperty("type", "string");
        endProp.addProperty("description", "Route end link id.");
        props.add("endLinkId", endProp);

        JsonObject linksProp = new JsonObject();
        linksProp.addProperty("type", "array");
        linksProp.addProperty(
            "description",
            "Intermediate link ids in travel order. Can be empty. Do NOT include startLinkId or endLinkId here."
        );

        JsonObject items = new JsonObject();
        items.addProperty("type", "string");
        linksProp.add("items", items);

        props.add("linkIds", linksProp);

        schema.add("properties", props);

        JsonArray required = new JsonArray();
        required.add("routeType");
        required.add("startLinkId");
        required.add("endLinkId");
        schema.add("required", required);

        return schema;
    }

	@Override
	public String getRouteType() {
		return "network";
	}
}