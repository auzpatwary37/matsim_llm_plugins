package org.matsim.contrib.llm.matsimdtobjects;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.matsim.contrib.llm.tools.ErrorMessages;

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
    
    public static NetworkRouteDTO fromJsonObject(JsonObject obj, Gson gson) {
        return gson.fromJson(obj, NetworkRouteDTO.class);
    }

    @Override
    public NetworkRoute toBaseClass(Map<String, Object> context, ErrorMessages em) {
        if (!isVerified(em, context)) return null;
        
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
    public boolean isVerified(ErrorMessages em, Map<String, Object> context) {
        boolean outcome = true;

        if (!"network".equals(routeType)) {
            outcome = false;
            em.addErrorMessages("routeType is not network.");
        }

        if (startLinkId == null || startLinkId.trim().isEmpty()) {
            outcome = false;
            em.addErrorMessages("startLinkId is not defined for network route.");
        }

        if (endLinkId == null || endLinkId.trim().isEmpty()) {
            outcome = false;
            em.addErrorMessages("endLinkId is not defined for network route.");
        }

        if (linkIds != null) {
            for (int i = 0; i < linkIds.size(); i++) {
                String s = linkIds.get(i);
                if (s == null || s.trim().isEmpty()) {
                    outcome = false;
                    em.addErrorMessages("linkIds contains a null or blank entry at position " + i + ".");
                }
            }
        }

        Object scenarioObj = context == null ? null : context.get("scenario");
        if (scenarioObj == null) {
            outcome = false;
            em.addErrorMessages("Scenario is missing from context.");
            return outcome;
        }

        if (!(scenarioObj instanceof org.matsim.api.core.v01.Scenario scenario)) {
            outcome = false;
            em.addErrorMessages("Context entry 'scenario' is not a MATSim Scenario.");
            return outcome;
        }

        if (startLinkId != null && !startLinkId.trim().isEmpty()
                && !scenario.getNetwork().getLinks().containsKey(org.matsim.api.core.v01.Id.createLinkId(startLinkId))) {
            outcome = false;
            em.addErrorMessages("startLinkId '" + startLinkId + "' does not exist in the scenario network.");
        }

        if (endLinkId != null && !endLinkId.trim().isEmpty()
                && !scenario.getNetwork().getLinks().containsKey(org.matsim.api.core.v01.Id.createLinkId(endLinkId))) {
            outcome = false;
            em.addErrorMessages("endLinkId '" + endLinkId + "' does not exist in the scenario network.");
        }

        if (linkIds != null) {
            for (int i = 0; i < linkIds.size(); i++) {
                String s = linkIds.get(i);
                if (s != null && !s.trim().isEmpty()
                        && !scenario.getNetwork().getLinks().containsKey(org.matsim.api.core.v01.Id.createLinkId(s))) {
                    outcome = false;
                    em.addErrorMessages("linkIds entry '" + s + "' at position " + i + " does not exist in the scenario network.");
                }
            }
        }

        return outcome;
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