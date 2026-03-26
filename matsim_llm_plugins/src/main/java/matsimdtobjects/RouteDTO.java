package matsimdtobjects;

import org.matsim.api.core.v01.population.Route;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import tools.ToolArgumentDTO;

public abstract class RouteDTO<R extends Route> extends ToolArgumentDTO<R> {

    public abstract String getRouteType();

    public static JsonObject getJsonSchema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("description", "Polymorphic MATSim route object.");

        JsonArray oneOf = new JsonArray();
        oneOf.add(NetworkRouteDTO.getJsonSchema());
        oneOf.add(TransitPassengerRouteDTO.getJsonSchema());
        oneOf.add(GenericRouteDTO.getJsonSchema());

        schema.add("oneOf", oneOf);
        return schema;
    }
}