package matsimBinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

import com.google.inject.Inject;

import rag.IVectorDB;

public class AgentExperienceEventHandler implements
        PersonDepartureEventHandler,
        PersonArrivalEventHandler,
        PersonEntersVehicleEventHandler,
        TransitDriverStartsEventHandler,
        VehicleEntersTrafficEventHandler,
        VehicleLeavesTrafficEventHandler,
        LinkEnterEventHandler,
        LinkLeaveEventHandler {

    private static final String AI_ATTRIBUTE = "isAI";

    private final Scenario scenario;
    private final IVectorDB vectorDb;

    private int currentIteration = 0;

    /**
     * transient state
     */
    private final Map<Id<Person>, TripState> activeTrips = new HashMap<>();
    private final Map<Id<Person>, Double> activePtWaitStart = new HashMap<>();
    private final Map<Id<org.matsim.vehicles.Vehicle>, Id<Person>> vehicleToDriver = new HashMap<>();
    private final Map<Id<org.matsim.vehicles.Vehicle>, Map<Id<Link>, Double>> vehicleLinkEnterTimes = new HashMap<>();
    private final Set<Id<Person>> transitDrivers = new HashSet<>();

    /**
     * persistent run state
     */
    private final Set<Id<Person>> insertedPersonProfiles = new HashSet<>();
    private final Map<Id<Person>, Set<String>> insertedMemoryIdsByPerson = new HashMap<>();

    @Inject
    public AgentExperienceEventHandler(Scenario scenario, IVectorDB vectorDb) {
        this.scenario = scenario;
        this.vectorDb = vectorDb;
    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        Id<Person> personId = event.getPersonId();
        if (!isAIAgent(personId)) {
            return;
        }

        ensurePersonContextInserted(personId);

        TripState trip = new TripState();
        trip.personId = personId;
        trip.mode = event.getLegMode();
        trip.departureTime = event.getTime();
        trip.originLinkId = event.getLinkId();
        trip.linkSequence.add(event.getLinkId());

        activeTrips.put(personId, trip);

        if ("pt".equals(event.getLegMode())) {
            activePtWaitStart.put(personId, event.getTime());
        }
    }

    @Override
    public void handleEvent(PersonArrivalEvent event) {
        Id<Person> personId = event.getPersonId();
        if (!isAIAgent(personId)) {
            return;
        }

        TripState trip = activeTrips.get(personId);
        if (trip == null) {
            return;
        }

        trip.arrivalTime = event.getTime();
        trip.destinationLinkId = event.getLinkId();

        double travelTime = trip.arrivalTime - trip.departureTime;

        String routeSignature = buildRouteSignature(trip);
        String memoryId = "experience_route_" + sanitize(personId.toString()) + "_"
                + sanitize(trip.mode) + "_"
                + sanitize(routeSignature) + "_day_" + currentIteration;

        String text = buildRouteExperienceText(trip, travelTime);

        Map<String, String> metadata = new HashMap<>();
        metadata.put("personId", personId.toString());
        metadata.put("experienceType", "route_travel_time");
        metadata.put("mode", trip.mode);
        metadata.put("originLinkId", safeId(trip.originLinkId));
        metadata.put("destinationLinkId", safeId(trip.destinationLinkId));
        metadata.put("routeSignature", routeSignature);
        metadata.put("travelTimeSec", formatSeconds(travelTime));
        metadata.put("timeBand", timeBand(trip.departureTime));
        metadata.put("day", String.valueOf(currentIteration));
        metadata.put("source", "experience_handler");

        insertAndTrack(personId, text, metadata);

        activeTrips.remove(personId);
    }

    @Override
    public void handleEvent(PersonEntersVehicleEvent event) {
        Id<Person> personId = event.getPersonId();

        if (transitDrivers.contains(personId)) {
            return;
        }
        if (!isAIAgent(personId)) {
            return;
        }

        TripState trip = activeTrips.get(personId);
        if (trip == null) {
            return;
        }

        if ("pt".equals(trip.mode) && activePtWaitStart.containsKey(personId)) {
            double waitStart = activePtWaitStart.remove(personId);
            double waitTime = event.getTime() - waitStart;

            String waitContext = safeId(trip.originLinkId) + "_" + timeBand(trip.departureTime);
            String memoryId = "experience_pt_wait_" + sanitize(personId.toString()) + "_"
                    + sanitize(waitContext) + "_day_" + currentIteration + "_" + shortUuid();

            String text = buildPtWaitExperienceText(trip, waitTime);

            Map<String, String> metadata = new HashMap<>();
            metadata.put("personId", personId.toString());
            metadata.put("experienceType", "pt_wait_time");
            metadata.put("mode", trip.mode);
            metadata.put("originLinkId", safeId(trip.originLinkId));
            metadata.put("waitTimeSec", formatSeconds(waitTime));
            metadata.put("timeBand", timeBand(trip.departureTime));
            metadata.put("day", String.valueOf(currentIteration));
            metadata.put("source", "experience_handler");

            insertAndTrack(personId, text, metadata);
        }
    }

    @Override
    public void handleEvent(TransitDriverStartsEvent event) {
        transitDrivers.add(event.getDriverId());
    }

    @Override
    public void handleEvent(VehicleEntersTrafficEvent event) {
        Id<Person> personId = event.getPersonId();
        if (!isAIAgent(personId)) {
            return;
        }

        TripState trip = activeTrips.get(personId);
        if (trip == null) {
            return;
        }

        vehicleToDriver.put(event.getVehicleId(), personId);
        vehicleLinkEnterTimes.computeIfAbsent(event.getVehicleId(), v -> new HashMap<>());
    }

    @Override
    public void handleEvent(VehicleLeavesTrafficEvent event) {
        vehicleToDriver.remove(event.getVehicleId());
        vehicleLinkEnterTimes.remove(event.getVehicleId());
    }

    @Override
    public void handleEvent(LinkEnterEvent event) {
        Id<Person> personId = vehicleToDriver.get(event.getVehicleId());
        if (personId == null || !isAIAgent(personId)) {
            return;
        }

        TripState trip = activeTrips.get(personId);
        if (trip == null) {
            return;
        }

        trip.linkSequence.add(event.getLinkId());

        vehicleLinkEnterTimes
                .computeIfAbsent(event.getVehicleId(), v -> new HashMap<>())
                .put(event.getLinkId(), event.getTime());
    }

    @Override
    public void handleEvent(LinkLeaveEvent event) {
        Id<Person> personId = vehicleToDriver.get(event.getVehicleId());
        if (personId == null || !isAIAgent(personId)) {
            return;
        }

        TripState trip = activeTrips.get(personId);
        if (trip == null) {
            return;
        }

        Map<Id<org.matsim.vehicles.Vehicle>, Map<Id<Link>, Double>> dummy = vehicleLinkEnterTimes;
        Map<Id<Link>, Double> enterTimes = dummy.get(event.getVehicleId());
        if (enterTimes == null) {
            return;
        }

        Double enterTime = enterTimes.remove(event.getLinkId());
        if (enterTime == null) {
            return;
        }

        Link link = scenario.getNetwork().getLinks().get(event.getLinkId());
        if (link == null) {
            return;
        }

        double actualTravelTime = event.getTime() - enterTime;
        double freeFlowTravelTime = Math.max(1.0, link.getLength() / Math.max(0.1, link.getFreespeed()));
        double congestionRatio = actualTravelTime / freeFlowTravelTime;

        boolean extraordinary = congestionRatio >= 3.0 || actualTravelTime - freeFlowTravelTime >= 120.0;
        if (!extraordinary) {
            return;
        }

        String memoryId = "experience_link_congestion_" + sanitize(personId.toString()) + "_"
                + sanitize(link.getId().toString()) + "_day_" + currentIteration + "_" + shortUuid();

        String text = buildLinkCongestionExperienceText(trip, link, actualTravelTime, freeFlowTravelTime, congestionRatio);

        Map<String, String> metadata = new HashMap<>();
        metadata.put("personId", personId.toString());
        metadata.put("experienceType", "link_congestion");
        metadata.put("mode", trip.mode);
        metadata.put("linkId", link.getId().toString());
        metadata.put("actualTravelTimeSec", formatSeconds(actualTravelTime));
        metadata.put("freeFlowTravelTimeSec", formatSeconds(freeFlowTravelTime));
        metadata.put("congestionRatio", String.format(Locale.ROOT, "%.2f", congestionRatio));
        metadata.put("timeBand", timeBand(event.getTime()));
        metadata.put("day", String.valueOf(currentIteration));
        metadata.put("source", "experience_handler");

        insertAndTrack(personId, text, metadata);
    }

    @Override
    public void reset(int iteration) {
        this.currentIteration = iteration;

        activeTrips.clear();
        activePtWaitStart.clear();
        vehicleToDriver.clear();
        vehicleLinkEnterTimes.clear();
        transitDrivers.clear();
    }

    public Set<String> getInsertedMemoryIdsForPerson(Id<Person> personId) {
        return insertedMemoryIdsByPerson.getOrDefault(personId, Set.of());
    }

    public Map<Id<Person>, Set<String>> getAllInsertedMemoryIdsByPerson() {
        return insertedMemoryIdsByPerson;
    }

    private boolean isAIAgent(Id<Person> personId) {
        Person person = scenario.getPopulation().getPersons().get(personId);
        if (person == null) {
            return false;
        }

        Object raw = person.getAttributes().getAttribute(AI_ATTRIBUTE);
        if (raw == null) {
            return false;
        }

        if (raw instanceof Boolean b) {
            return b;
        }

        return Boolean.parseBoolean(raw.toString());
    }

    private void ensurePersonContextInserted(Id<Person> personId) {
        if (insertedPersonProfiles.contains(personId)) {
            return;
        }

        Person person = scenario.getPopulation().getPersons().get(personId);
        if (person == null) {
            return;
        }

        Map<String, Object> attrMap = person.getAttributes().getAsMap();

        StringBuilder text = new StringBuilder();
        text.append("Traveler profile context for AI agent ").append(personId).append(". ");

        Map<String, String> metadata = new HashMap<>();
        metadata.put("personId", personId.toString());
        metadata.put("experienceType", "person_profile");
        metadata.put("day", String.valueOf(currentIteration));
        metadata.put("source", "person_attributes");

        List<String> pieces = new ArrayList<>();
        for (Map.Entry<String, Object> e : attrMap.entrySet()) {
            if (e.getValue() == null) {
                continue;
            }
            if (AI_ATTRIBUTE.equals(e.getKey())||e.getKey().equals("vehicles")) {
                continue;
            }
            String key = e.getKey();
            String value = String.valueOf(e.getValue());
            if(key.equals("gender")) {
            	if(value.equals("1")) {
            		value = "male";
            		
            	}else {
            		value = "female";
            	}
            }
            pieces.add(key + "=" + value);
            metadata.put("attr_" + key, value);
        }

        if (pieces.isEmpty()) {
            text.append("No additional attributes were available.");
        } else {
            text.append("Known attributes: ").append(String.join(", ", pieces)).append(".");
        }

        String memoryId = "person_profile_" + sanitize(personId.toString());
        insertAndTrack(personId, text.toString(), metadata);

        insertedPersonProfiles.add(personId);
    }

    private void insertAndTrack(Id<Person> personId, String content, Map<String, String> metadata) {
        String id = vectorDb.insert(content, metadata);
        insertedMemoryIdsByPerson.computeIfAbsent(personId, k -> new HashSet<>()).add(id);
    }

    private String buildRouteSignature(TripState trip) {
        if (trip.linkSequence == null || trip.linkSequence.isEmpty()) {
            return "from_" + safeId(trip.originLinkId) + "_to_" + safeId(trip.destinationLinkId);
        }

        String raw = trip.linkSequence.stream()
                .map(Id::toString)
                .collect(Collectors.joining(">"));

        return Integer.toHexString(raw.hashCode());
    }

    private String buildRouteExperienceText(TripState trip, double travelTime) {
        return "On day " + currentIteration
                + ", the " + trip.mode
                + " trip from link " + safeId(trip.originLinkId)
                + " to link " + safeId(trip.destinationLinkId)
                + " took about " + humanDuration(travelTime)
                + " during the " + timeBand(trip.departureTime)
                + ". This was the travel experience for that route on that day.";
    }

    private String buildPtWaitExperienceText(TripState trip, double waitTime) {
        return "On day " + currentIteration
                + ", the public transit wait from link " + safeId(trip.originLinkId)
                + " lasted about " + humanDuration(waitTime)
                + " during the " + timeBand(trip.departureTime)
                + ". This includes waiting for boarding or transfer at that stage of the trip.";
    }

    private String buildLinkCongestionExperienceText(
            TripState trip,
            Link link,
            double actualTravelTime,
            double freeFlowTravelTime,
            double congestionRatio) {

        return "On day " + currentIteration
                + ", link " + link.getId()
                + " was heavily congested during a " + trip.mode
                + " trip. It took about " + humanDuration(actualTravelTime)
                + " to traverse, compared with a rough free-flow time of "
                + humanDuration(freeFlowTravelTime)
                + ". The observed congestion ratio was about "
                + String.format(Locale.ROOT, "%.1f", congestionRatio)
                + ".";
    }

    private static String safeId(Id<?> id) {
        return id == null ? "unknown" : id.toString();
    }

    private static String sanitize(String s) {
        return s.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private static String shortUuid() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private static String formatSeconds(double seconds) {
        return String.format(Locale.ROOT, "%.1f", seconds);
    }

    private static String humanDuration(double seconds) {
        long rounded = Math.round(seconds);
        long min = rounded / 60;
        long sec = rounded % 60;

        if (min <= 0) {
            return sec + " seconds";
        }
        if (sec == 0) {
            return min + " minutes";
        }
        return min + " minutes " + sec + " seconds";
    }

    private static String timeBand(double timeSec) {
        double hour = (timeSec % 86400.0) / 3600.0;

        if (hour >= 6 && hour < 10) return "morning peak";
        if (hour >= 10 && hour < 15) return "midday";
        if (hour >= 15 && hour < 19) return "afternoon peak";
        if (hour >= 19 && hour < 24) return "evening";
        return "early day";
    }

    private static class TripState {
        Id<Person> personId;
        String mode;
        double departureTime;
        double arrivalTime;
        Id<Link> originLinkId;
        Id<Link> destinationLinkId;
        List<Id<Link>> linkSequence = new ArrayList<>();
    }
}