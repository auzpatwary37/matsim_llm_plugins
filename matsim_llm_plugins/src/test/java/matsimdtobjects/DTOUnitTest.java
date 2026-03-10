package matsimdtobjects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.pt.routes.DefaultTransitPassengerRoute;
import org.matsim.pt.routes.TransitPassengerRoute;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

public class DTOUnitTest {

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private Map<String, Object> emptyContext() {
        return new HashMap<>();
    }

    @Test
    void testActivityDTO_baseToDtoToBase() {
        Activity original = PopulationUtils.createActivityFromFacilityId(
                "home",
                Id.create("fac_home", ActivityFacility.class)
        );
        original.setEndTime(28800.0);

        ActivityDTO dto = ActivityDTO.toDTOFromBaseObject().apply(original);

        assertNotNull(dto);
        assertEquals("activity", dto.getElementType());
        assertEquals("home", dto.type);
        assertEquals("fac_home", dto.facilityId);
        assertNotNull(dto.endTime);
        assertEquals(28800.0, dto.endTime, 1e-6);
        assertTrue(dto.isVerified());

        Activity rebuilt = dto.toBaseClass(emptyContext());

        assertNotNull(rebuilt);
        assertEquals("home", rebuilt.getType());
        assertEquals("fac_home", rebuilt.getFacilityId().toString());
        assertNotNull(rebuilt.getEndTime());
        assertTrue(rebuilt.getEndTime().isDefined());
        assertEquals(28800.0, rebuilt.getEndTime().seconds(), 1e-6);
    }

    @Test
    void testActivityDTO_jsonRoundTrip() {
        ActivityDTO dto = new ActivityDTO();
        dto.elementType = "activity";
        dto.type = "work";
        dto.facilityId = "fac_work";
        dto.endTime = 32400.0;

        assertTrue(dto.isVerified());

        String json = gson.toJson(dto);
        ActivityDTO parsed = gson.fromJson(json, ActivityDTO.class);

        assertNotNull(parsed);
        assertEquals("activity", parsed.elementType);
        assertEquals("work", parsed.type);
        assertEquals("fac_work", parsed.facilityId);
        assertEquals(32400.0, parsed.endTime, 1e-6);
        assertTrue(parsed.isVerified());
    }

    @Test
    void testActivityDTO_interactionWithoutEndTime() {
        ActivityDTO dto = new ActivityDTO();
        dto.elementType = "activity";
        dto.type = "pt interaction";
        dto.facilityId = "fac_pt_interaction";

        assertTrue(dto.isVerified());

        Activity rebuilt = dto.toBaseClass(emptyContext());
        assertNotNull(rebuilt);
        assertEquals("pt interaction", rebuilt.getType());
        assertEquals("fac_pt_interaction", rebuilt.getFacilityId().toString());
    }

    @Test
    void testNetworkRouteDTO_baseToDtoToBase() {
        NetworkRoute original = RouteUtils.createNetworkRoute(Arrays.asList(Id.createLinkId("l1"),Id.createLinkId("l2"), Id.createLinkId("l3"),Id.createLinkId("l4")));

        NetworkRouteDTO dto = NetworkRouteDTO.toDTOFromBaseObject().apply(original);

        assertNotNull(dto);
        assertEquals("network", dto.getRouteType());
        assertEquals("l1", dto.startLinkId);
        assertEquals("l4", dto.endLinkId);
        assertNotNull(dto.linkIds);
        assertEquals(List.of("l2", "l3"), dto.linkIds);
        assertTrue(dto.isVerified());

        NetworkRoute rebuilt = dto.toBaseClass(emptyContext());

        assertNotNull(rebuilt);
        assertEquals("l1", rebuilt.getStartLinkId().toString());
        assertEquals("l4", rebuilt.getEndLinkId().toString());
        assertEquals(
                List.of("l2", "l3"),
                rebuilt.getLinkIds().stream().map(Object::toString).toList()
        );
    }

    @Test
    void testNetworkRouteDTO_jsonRoundTrip() {
        NetworkRouteDTO dto = new NetworkRouteDTO();
        dto.startLinkId = "a";
        dto.endLinkId = "d";
        dto.linkIds = Arrays.asList("b", "c");

        assertTrue(dto.isVerified());

        String json = gson.toJson(dto);
        NetworkRouteDTO parsed = gson.fromJson(json, NetworkRouteDTO.class);

        assertNotNull(parsed);
        assertEquals("network", parsed.routeType);
        assertEquals("a", parsed.startLinkId);
        assertEquals("d", parsed.endLinkId);
        assertEquals(List.of("b", "c"), parsed.linkIds);
        assertTrue(parsed.isVerified());
    }

    @Test
    void testTransitPassengerRouteDTO_baseToDtoToBase() {
        TransitPassengerRoute original = new DefaultTransitPassengerRoute(
                Id.createLinkId("start_link"),
                Id.createLinkId("end_link"),
                Id.create("stop_a", TransitStopFacility.class),
                Id.create("stop_b", TransitStopFacility.class),
                Id.create("line_1", TransitLine.class),
                Id.create("route_1", TransitRoute.class)
        );

        TransitPassengerRouteDTO dto = TransitPassengerRouteDTO.toDTOFromBaseObject().apply(original);

        assertNotNull(dto);
        assertEquals("transit_passenger", dto.getRouteType());
        assertEquals("start_link", dto.accessLinkId);
        assertEquals("end_link", dto.egressLinkId);
        assertEquals("stop_a", dto.accessStopId);
        assertEquals("stop_b", dto.egressStopId);
        assertEquals("line_1", dto.lineId);
        assertEquals("route_1", dto.routeId);
        assertTrue(dto.isVerified());

        TransitPassengerRoute rebuilt = dto.toBaseClass(emptyContext());

        assertNotNull(rebuilt);
        assertEquals("start_link", rebuilt.getStartLinkId().toString());
        assertEquals("end_link", rebuilt.getEndLinkId().toString());
        assertEquals("stop_a", rebuilt.getAccessStopId().toString());
        assertEquals("stop_b", rebuilt.getEgressStopId().toString());
        assertEquals("line_1", rebuilt.getLineId().toString());
        assertEquals("route_1", rebuilt.getRouteId().toString());
    }

    @Test
    void testTransitPassengerRouteDTO_jsonRoundTrip() {
        TransitPassengerRouteDTO dto = new TransitPassengerRouteDTO();
        dto.routeType = "transit_passenger";
        dto.accessLinkId = "access_link";
        dto.egressLinkId = "egress_link";
        dto.accessStopId = "stop_in";
        dto.egressStopId = "stop_out";
        dto.lineId = "lineX";
        dto.routeId = "routeY";
        dto.departureId = "dep1";

        assertTrue(dto.isVerified());

        String json = gson.toJson(dto);
        TransitPassengerRouteDTO parsed = gson.fromJson(json, TransitPassengerRouteDTO.class);

        assertNotNull(parsed);
        assertEquals("transit_passenger", parsed.routeType);
        assertEquals("access_link", parsed.accessLinkId);
        assertEquals("egress_link", parsed.egressLinkId);
        assertEquals("stop_in", parsed.accessStopId);
        assertEquals("stop_out", parsed.egressStopId);
        assertEquals("lineX", parsed.lineId);
        assertEquals("routeY", parsed.routeId);
        assertEquals("dep1", parsed.departureId);
        assertTrue(parsed.isVerified());
    }

    @Test
    void testLegDTO_withNetworkRoute_baseToDtoToBase() {
        Leg original = PopulationUtils.createLeg("car");
        original.setRoute(RouteUtils.createNetworkRoute(Arrays.asList(Id.createLinkId("l1"),Id.createLinkId("l2"), Id.createLinkId("l3"),Id.createLinkId("l4"))));

        LegDTO dto = LegDTO.toDTOFromBaseObject().apply(original);

        assertNotNull(dto);
        assertEquals("leg", dto.getElementType());
        assertEquals("car", dto.mode);
        assertNotNull(dto.route);
        assertTrue(dto.route instanceof NetworkRouteDTO);
        assertTrue(dto.isVerified());

        Leg rebuilt = dto.toBaseClass(emptyContext());

        assertNotNull(rebuilt);
        assertEquals("car", rebuilt.getMode());
        assertNotNull(rebuilt.getRoute());
        assertTrue(rebuilt.getRoute() instanceof NetworkRoute);

        NetworkRoute rebuiltRoute = (NetworkRoute) rebuilt.getRoute();
        assertEquals("l1", rebuiltRoute.getStartLinkId().toString());
        assertEquals("l4", rebuiltRoute.getEndLinkId().toString());
        assertEquals(
                List.of("l2", "l3"),
                rebuiltRoute.getLinkIds().stream().map(Object::toString).toList()
        );
    }

    @Test
    void testLegDTO_withTransitRoute_baseToDtoToBase() {
        Leg original = PopulationUtils.createLeg("pt");
        original.setRoute(new DefaultTransitPassengerRoute(
                Id.createLinkId("start_link"),
                Id.createLinkId("end_link"),
                Id.create("stop_a", TransitStopFacility.class),
                Id.create("stop_b", TransitStopFacility.class),
                Id.create("line_1", TransitLine.class),
                Id.create("route_1", TransitRoute.class)
        ));

        LegDTO dto = LegDTO.toDTOFromBaseObject().apply(original);

        assertNotNull(dto);
        assertEquals("leg", dto.getElementType());
        assertEquals("pt", dto.mode);
        assertNotNull(dto.route);
        assertTrue(dto.route instanceof TransitPassengerRouteDTO);
        assertTrue(dto.isVerified());

        Leg rebuilt = dto.toBaseClass(emptyContext());

        assertNotNull(rebuilt);
        assertEquals("pt", rebuilt.getMode());
        assertNotNull(rebuilt.getRoute());
        assertTrue(rebuilt.getRoute() instanceof TransitPassengerRoute);

        TransitPassengerRoute rebuiltRoute = (TransitPassengerRoute) rebuilt.getRoute();
        assertEquals("start_link", rebuiltRoute.getStartLinkId().toString());
        assertEquals("end_link", rebuiltRoute.getEndLinkId().toString());
        assertEquals("stop_a", rebuiltRoute.getAccessStopId().toString());
        assertEquals("stop_b", rebuiltRoute.getEgressStopId().toString());
        assertEquals("line_1", rebuiltRoute.getLineId().toString());
        assertEquals("route_1", rebuiltRoute.getRouteId().toString());
    }

    @Test
    void testLegDTO_jsonRoundTrip_withConcreteRouteDTO() {
        LegDTO dto = new LegDTO();
        dto.elementType = "leg";
        dto.mode = "bike";
        dto.departureTimeSeconds = 100.0;
        dto.travelTimeSeconds = 500.0;

        NetworkRouteDTO routeDto = new NetworkRouteDTO();
        
        routeDto.startLinkId = "s";
        routeDto.endLinkId = "e";
        routeDto.linkIds = Arrays.asList("m1", "m2");

        dto.route = routeDto;
        
        

        assertTrue(dto.isVerified());

        String json = dto.toJsonObject(gson).toString();

        assertNotNull(json);
        assertTrue(json.contains("\"elementType\":\"leg\""));
        assertTrue(json.contains("\"routeType\":\"network\""));
    }

    @Test
    void testLegDTO_invalidModeShouldFail() {
        LegDTO dto = new LegDTO();
        dto.elementType = "leg";
        dto.mode = "spaceship";

        NetworkRouteDTO routeDto = new NetworkRouteDTO();
//        routeDto.routeType = "network";
        routeDto.startLinkId = "l1";
        routeDto.endLinkId = "l2";
        routeDto.linkIds = List.of();

        dto.route = routeDto;

        assertFalse(dto.isVerified());
        assertNull(dto.toBaseClass(emptyContext()));
    }

    @Test
    void testLegDTO_missingRouteShouldFail() {
        LegDTO dto = new LegDTO();
        dto.elementType = "leg";
        dto.mode = "walk";

        assertFalse(dto.isVerified());
        assertNull(dto.toBaseClass(emptyContext()));
    }

    @Test
    void testSchemasExist() {
        assertNotNull(ActivityDTO.getJsonSchema());
        assertNotNull(LegDTO.getJsonSchema());
        assertNotNull(NetworkRouteDTO.getJsonSchema());
        assertNotNull(TransitPassengerRouteDTO.getJsonSchema());
    }

    @Test
    void testActivitySchemaRequiresElementType() {
        String schema = ActivityDTO.getJsonSchema().toString();
        assertTrue(schema.contains("elementType"));
        assertTrue(schema.contains("facilityId"));
        assertTrue(schema.contains("type"));
    }

    @Test
    void testLegSchemaRequiresElementTypeAndRoute() {
        String schema = LegDTO.getJsonSchema().toString();
        assertTrue(schema.contains("elementType"));
        assertTrue(schema.contains("mode"));
        assertTrue(schema.contains("route"));
    }

    @Test
    void testRouteSchemasContainRouteType() {
        assertTrue(NetworkRouteDTO.getJsonSchema().toString().contains("routeType"));
        assertTrue(TransitPassengerRouteDTO.getJsonSchema().toString().contains("routeType"));
    }
}