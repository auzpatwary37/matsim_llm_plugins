package org.matsim.contrib.llm.matsimdtobjects;

import java.util.Map;
import java.util.function.Function;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.routes.GenericRouteImpl;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.matsim.contrib.llm.tools.ErrorMessages;

public class GenericRouteDTO extends RouteDTO<Route> {

    public String startLinkId;
    public String endLinkId;

    public Double distance;
    public Double travelTime;

    public String routeDescription;

    // discriminator for polymorphic JSON parsing
    public final String routeType = "generic";

    public GenericRouteDTO() {}

    public GenericRouteDTO(Route route) {
        if (route == null) return;

        if (route.getStartLinkId() != null) {
            this.startLinkId = route.getStartLinkId().toString();
        }

        if (route.getEndLinkId() != null) {
            this.endLinkId = route.getEndLinkId().toString();
        }

        this.distance = route.getDistance();

        if (route.getTravelTime() != null && route.getTravelTime().isDefined()) {
            this.travelTime = route.getTravelTime().seconds();
        }

        this.routeDescription = route.getRouteDescription();
    }

    public static GenericRouteDTO fromJsonObject(JsonObject obj, Gson gson) {
        return gson.fromJson(obj, GenericRouteDTO.class);
    }

    @Override
    public Route toBaseClass(Map<String, Object> context, ErrorMessages em) {
        if (!isVerified(em, context)) return null;

        GenericRouteImpl r = new GenericRouteImpl(
            startLinkId != null && !startLinkId.trim().isEmpty() ? Id.createLinkId(startLinkId.trim()) : null,
            endLinkId != null && !endLinkId.trim().isEmpty() ? Id.createLinkId(endLinkId.trim()) : null
        );

        if (distance != null) {
            r.setDistance(distance);
        }

        if (travelTime != null) {
            r.setTravelTime(travelTime);
        }

        if (routeDescription != null && !routeDescription.trim().isEmpty()) {
            r.setRouteDescription(routeDescription);
        }

        return r;
    }

    @Override
    public boolean isVerified(ErrorMessages em, Map<String,Object> context) {
        boolean outcome = true;

        if (!"generic".equals(routeType)) {
            outcome = false;
            em.addErrorMessages("routeType is not generic.");
        }

        if (distance != null && distance < 0) {
            outcome = false;
            em.addErrorMessages("distance cannot be negative for generic route.");
        }

        if (travelTime != null && travelTime < 0) {
            outcome = false;
            em.addErrorMessages("travelTime cannot be negative for generic route.");
        }

        if (startLinkId != null && startLinkId.trim().isEmpty()) {
            outcome = false;
            em.addErrorMessages("startLinkId is blank for generic route.");
        }

        if (endLinkId != null && endLinkId.trim().isEmpty()) {
            outcome = false;
            em.addErrorMessages("endLinkId is blank for generic route.");
        }

        return outcome;
    }

    public static Function<Route, GenericRouteDTO> toDTOFromBaseObject() {
        return GenericRouteDTO::new;
    }

    public static JsonObject getJsonSchema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        schema.addProperty("additionalProperties", false);
        schema.addProperty(
            "description",
            "MATSim generic route definition. Use for non-network routes such as walk or bike routes when only summary route information is available."
        );

        JsonObject props = new JsonObject();

        JsonObject routeTypeProp = new JsonObject();
        routeTypeProp.addProperty("type", "string");
        routeTypeProp.addProperty("description", "Discriminator for the MATSim route subtype.");
        JsonArray routeTypeEnum = new JsonArray();
        routeTypeEnum.add("generic");
        routeTypeProp.add("enum", routeTypeEnum);
        props.add("routeType", routeTypeProp);

        JsonObject startProp = new JsonObject();
        startProp.addProperty("type", "string");
        startProp.addProperty("description", "Optional route start link id.");
        props.add("startLinkId", startProp);

        JsonObject endProp = new JsonObject();
        endProp.addProperty("type", "string");
        endProp.addProperty("description", "Optional route end link id.");
        props.add("endLinkId", endProp);

        JsonObject distanceProp = new JsonObject();
        distanceProp.addProperty("type", "number");
        distanceProp.addProperty("description", "Optional route distance in meters.");
        props.add("distance", distanceProp);

        JsonObject travelTimeProp = new JsonObject();
        travelTimeProp.addProperty("type", "number");
        travelTimeProp.addProperty("description", "Optional route travel time in seconds.");
        props.add("travelTime", travelTimeProp);

        JsonObject descProp = new JsonObject();
        descProp.addProperty("type", "string");
        descProp.addProperty("description", "Optional MATSim route description string.");
        props.add("routeDescription", descProp);

        schema.add("properties", props);

        JsonArray required = new JsonArray();
        required.add("routeType");
        schema.add("required", required);

        return schema;
    }

    @Override
    public String getRouteType() {
        return "generic";
    }
}