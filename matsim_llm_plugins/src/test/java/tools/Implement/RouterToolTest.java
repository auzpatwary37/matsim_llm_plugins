package tools.Implement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.pt.routes.DefaultTransitPassengerRoute;
import org.matsim.pt.routes.TransitPassengerRoute;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import matsimdtobjects.LegDTO;
import matsimdtobjects.PlanDTO;
import tools.ErrorMessages;
import tools.IToolResponse;
import tools.VerificationFailedException;

public class RouterToolTest {

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private RouterTool tool;
    private ActivityFacilities facilities;
    private ActivityFacility facA;
    private ActivityFacility facB;

    @BeforeEach
    void setUp() {
        tool = new RouterTool();

        facilities = FacilitiesUtils.createActivityFacilities();
        ActivityFacilitiesFactory factory = facilities.getFactory();

        facA = factory.createActivityFacility(Id.create("fac_a", ActivityFacility.class), new Coord(0.0, 0.0));
        facB = factory.createActivityFacility(Id.create("fac_b", ActivityFacility.class), new Coord(1000.0, 0.0));

        facilities.addActivityFacility(facA);
        facilities.addActivityFacility(facB);
    }

    @Test
    void testGetJsonSchema_departureTimeIsOptional() {
        JsonObject schema = tool.getJsonSchema();

        assertNotNull(schema);
        assertEquals(tool.getName(), schema.get("name").getAsString());

        JsonObject parameters = schema.getAsJsonObject("parameters");
        assertNotNull(parameters);

        JsonObject properties = parameters.getAsJsonObject("properties");
        assertTrue(properties.has("fromFacilityId"));
        assertTrue(properties.has("toFacilityId"));
        assertTrue(properties.has("mode"));
        assertTrue(properties.has("departureTimeSeconds"));

        JsonArray required = parameters.getAsJsonArray("required");
        List<String> requiredFields = new ArrayList<>();
        required.forEach(e -> requiredFields.add(e.getAsString()));

        assertTrue(requiredFields.contains("fromFacilityId"));
        assertTrue(requiredFields.contains("toFacilityId"));
        assertTrue(requiredFields.contains("mode"));
        assertFalse(requiredFields.contains("departureTimeSeconds"));
    }

    @Test
    void testVerifyArguments_acceptsValidNetworkRequest() throws Exception {
        Map<String, Object> args = new HashMap<>();
        args.put("fromFacilityId", "fac_a");
        args.put("toFacilityId", "fac_b");
        args.put("mode", "car");
        args.put("departureTimeSeconds", 28800.0);

        Map<String, Object> context = new HashMap<>();
        context.put("activityFacilities", facilities);
        context.put("tripRouter", new FakeNetworkTripRouter());

        ErrorMessages em = new ErrorMessages();

        tool.verifyArguments(args, context, em);

        assertTrue(em.getErrorMessages().isEmpty());
    }

    @Test
    void testVerifyArguments_rejectsMissingFacility() {
        Map<String, Object> args = new HashMap<>();
        args.put("fromFacilityId", "fac_missing");
        args.put("toFacilityId", "fac_b");
        args.put("mode", "car");

        Map<String, Object> context = new HashMap<>();
        context.put("activityFacilities", facilities);
        context.put("tripRouter", new FakeNetworkTripRouter());

        ErrorMessages em = new ErrorMessages();

        VerificationFailedException ex = assertThrows(
                VerificationFailedException.class,
                () -> tool.verifyArguments(args, context, em)
        );

        assertNotNull(ex);
        assertFalse(em.getErrorMessages().isEmpty());
        assertTrue(em.getCombinedErrorMessages().contains("Origin facilityId not found"));
    }

    @Test
    void testCallTool_networkRoute_returnsPlanAndSerializableJson() {
        Map<String, Object> context = new HashMap<>();
        context.put("activityFacilities", facilities);
        context.put("tripRouter", new FakeNetworkTripRouter());


        Map<String, Object> args = new HashMap<>();
        args.put("fromFacilityId", "fac_a");
        args.put("toFacilityId", "fac_b");
        args.put("mode", "car");
        args.put("departureTimeSeconds", 28800.0);

        IToolResponse<Plan> response = tool.callTool("call_1", args, null, context);

        assertNotNull(response);
        assertEquals("call_1", response.getToolCallId());
        assertEquals(tool.getName(), response.getName());
        assertTrue(response.isForLLM());

        Plan outputPlan = response.getToolCallOutputContainer();
        assertNotNull(outputPlan);
        assertEquals(1, outputPlan.getPlanElements().size());
        assertTrue(outputPlan.getPlanElements().get(0) instanceof Leg);

        Leg leg = (Leg) outputPlan.getPlanElements().get(0);
        assertEquals("car", leg.getMode());
        assertNotNull(leg.getRoute());
        assertTrue(leg.getRoute() instanceof NetworkRoute);

        JsonObject json = gson.fromJson(response.getResponseJson(), JsonObject.class);
        PlanDTO parsedDto = PlanDTO.fromJsonObject(json, gson);

        assertNotNull(parsedDto);
        assertEquals(1, parsedDto.elements.size());
        assertTrue(parsedDto.elements.get(0) instanceof LegDTO);

        LegDTO legDto = (LegDTO) parsedDto.elements.get(0);
        assertEquals("car", legDto.mode);
        assertNotNull(legDto.route);
        assertEquals("network", legDto.route.getRouteType());
    }

    @Test
    void testCallTool_ptRoute_preservesFullElementStack() {
        Map<String, Object> context = new HashMap<>();
        context.put("activityFacilities", facilities);
        context.put("tripRouter", new FakePtTripRouter());
       

        Map<String, Object> args = new HashMap<>();
        args.put("fromFacilityId", "fac_a");
        args.put("toFacilityId", "fac_b");
        args.put("mode", "pt");
        args.put("departureTimeSeconds", 30000.0);

        IToolResponse<Plan> response = tool.callTool("call_pt", args, null, context);

        assertNotNull(response);
        assertTrue(response.isForLLM());

        Plan outputPlan = response.getToolCallOutputContainer();
        assertNotNull(outputPlan);
        assertEquals(5, outputPlan.getPlanElements().size());

        assertTrue(outputPlan.getPlanElements().get(0) instanceof Activity);
        assertTrue(outputPlan.getPlanElements().get(1) instanceof Leg);
        assertTrue(outputPlan.getPlanElements().get(2) instanceof Activity);
        assertTrue(outputPlan.getPlanElements().get(3) instanceof Leg);
        assertTrue(outputPlan.getPlanElements().get(4) instanceof Activity);

        Leg accessLeg = (Leg) outputPlan.getPlanElements().get(1);
        Leg ptLeg = (Leg) outputPlan.getPlanElements().get(3);

        assertEquals("transit_walk", accessLeg.getMode());
        assertEquals("pt", ptLeg.getMode());

        assertNotNull(accessLeg.getRoute());
        assertNotNull(ptLeg.getRoute());
        assertTrue(ptLeg.getRoute() instanceof TransitPassengerRoute);

        JsonObject json = gson.fromJson(response.getResponseJson(), JsonObject.class);
        PlanDTO parsedDto = PlanDTO.fromJsonObject(json, gson);

        assertNotNull(parsedDto);
        assertEquals(5, parsedDto.elements.size());

        LegDTO parsedAccessLeg = (LegDTO) parsedDto.elements.get(1);
        LegDTO parsedPtLeg = (LegDTO) parsedDto.elements.get(3);

        assertEquals("transit_walk", parsedAccessLeg.mode);
        assertEquals("generic", parsedAccessLeg.route.getRouteType());

        assertEquals("pt", parsedPtLeg.mode);
        assertEquals("transit_passenger", parsedPtLeg.route.getRouteType());
    }

    @Test
    void testCall_parsesWrappedJsonArguments() {
        Map<String, Object> context = new HashMap<>();
        context.put("activityFacilities", facilities);
        context.put("tripRouter", new FakeNetworkTripRouter());


        String toolJson = """
            {
              "fromFacilityId": { "value": "fac_a" },
              "toFacilityId": { "value": "fac_b" },
              "mode": { "value": "car" },
              "departureTimeSeconds": { "value": 28800.0 }
            }
            """;

        IToolResponse<Plan> response = tool.call(toolJson, "call_json", null, context);

        assertNotNull(response);
        assertEquals("call_json", response.getToolCallId());
        assertNotNull(response.getToolCallOutputContainer());
        assertTrue(response.isForLLM());

        Plan plan = response.getToolCallOutputContainer();
        assertEquals(1, plan.getPlanElements().size());
        assertTrue(plan.getPlanElements().get(0) instanceof Leg);
    }

    @Test
    void testCall_returnsStructuredErrorResponseForBadInput() {
        Map<String, Object> context = new HashMap<>();
        context.put("activityFacilities", facilities);
        context.put("tripRouter", new FakeNetworkTripRouter());


        String badJson = """
            {
              "fromFacilityId": { "value": "fac_missing" },
              "toFacilityId": { "value": "fac_b" },
              "mode": { "value": "car" }
            }
            """;

        IToolResponse<Plan> response = tool.call(badJson, "call_bad", null, context);

        assertNotNull(response);
        assertEquals("call_bad", response.getToolCallId());
        assertNull(response.getToolCallOutputContainer());

        JsonObject error = gson.fromJson(response.getResponseJson(), JsonObject.class);
        assertEquals("ERROR", error.get("status").getAsString());
        assertTrue(error.get("message").getAsString().contains("Origin facilityId not found"));
    }

    // ---------------------------------------------------------------------
    // Fake routers used by reflection inside RouterTool
    // ---------------------------------------------------------------------

    public static class FakeNetworkTripRouter {
        public List<? extends PlanElement> calcRoute(
                String mode,
                ActivityFacility fromFacility,
                ActivityFacility toFacility,
                double departureTimeSeconds,
                Person person) {

            Leg leg = PopulationUtils.createLeg(mode);

            NetworkRoute route = RouteUtils.createNetworkRoute(List.of(
                    Id.createLinkId("l_start"),
                    Id.createLinkId("l_mid"),
                    Id.createLinkId("l_end")
            ));
            leg.setRoute(route);
            leg.setDepartureTime(departureTimeSeconds);
            leg.setTravelTime(600.0);

            return List.of(leg);
        }
    }

    public static class FakePtTripRouter {
        public List<? extends PlanElement> calcRoute(
                String mode,
                ActivityFacility fromFacility,
                ActivityFacility toFacility,
                double departureTimeSeconds,
                Person person) {

            Activity access = PopulationUtils.createActivityFromFacilityId(
                    "pt interaction",
                    Id.create("fac_access", ActivityFacility.class)
            );

            Leg walkLeg = PopulationUtils.createLeg("transit_walk");
            GenericRouteImpl walkRoute = new GenericRouteImpl(
                    Id.createLinkId("walk_start"),
                    Id.createLinkId("walk_end")
            );
            walkRoute.setDistance(250.0);
            walkRoute.setTravelTime(180.0);
            walkRoute.setRouteDescription("walk to stop");
            walkLeg.setRoute(walkRoute);
            walkLeg.setDepartureTime(departureTimeSeconds);
            walkLeg.setTravelTime(180.0);

            Activity wait = PopulationUtils.createActivityFromFacilityId(
                    "pt interaction",
                    Id.create("fac_wait", ActivityFacility.class)
            );

            Leg ptLeg = PopulationUtils.createLeg("pt");
            TransitPassengerRoute ptRoute = new DefaultTransitPassengerRoute(
                    Id.createLinkId("pt_start"),
                    Id.createLinkId("pt_end"),
                    Id.create("stop_a", TransitStopFacility.class),
                    Id.create("stop_b", TransitStopFacility.class),
                    Id.create("line_1", TransitLine.class),
                    Id.create("route_1", TransitRoute.class)
            );
            ptLeg.setRoute(ptRoute);
            ptLeg.setDepartureTime(departureTimeSeconds + 180.0);
            ptLeg.setTravelTime(900.0);

            Activity egress = PopulationUtils.createActivityFromFacilityId(
                    "work",
                    Id.create("fac_b", ActivityFacility.class)
            );

            return List.of(access, walkLeg, wait, ptLeg, egress);
        }
    }
}