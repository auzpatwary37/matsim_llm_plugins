package gsonprocessor;
import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.StageActivityHandling;
import org.matsim.core.router.TripStructureUtils.Trip;


public class PlanGson{
	List<Object> activitiesAndLegs = new ArrayList<>();

	public Id<Person> personId; 
	
	
	public static PlanGson createPlanGson(Plan pl) {
		Plan plan = PopulationUtils.createPlan();
		PopulationUtils.copyFromTo(pl, plan);
		List<Trip> trips = TripStructureUtils.getTrips(plan);
		List<Activity> activities = TripStructureUtils.getActivities(plan, StageActivityHandling.ExcludeStageActivities);
		PlanGson p = new PlanGson();
		p.personId = pl.getPerson().getId();
		double previousActEndTime = 0;
		boolean ifCar = false;
		String carLocation = "unknown";
		if(TripStructureUtils.getRoutingModeIdentifier().identifyMainMode(trips.get(0).getTripElements()).equals("car")) {
			ifCar = true;
		} 
		
		
		for(int i=0;i<activities.size();i++) {
			Activity a=activities.get(i);
			ActivityGson aa = new ActivityGson();
			aa.activityType = a.getType();
			aa.coord = a.getCoord();
			if(a.getEndTime().isDefined()) {
				aa.endTime = a.getEndTime().seconds();
			}else {
				aa.endTime = 27*3600;
			}
			aa.facilityId = a.getFacilityId().toString();
			aa.linkId = a.getLinkId().toString();
			aa.maximumDuration = aa.endTime-previousActEndTime;
			if(ifCar)carLocation =aa.activityType+"_"+aa.facilityId;
			aa.carLocation = carLocation;
			aa.typicalSoc = (double) a.getAttributes().getAttribute("actSOC");
					
			p.activitiesAndLegs.add(aa);
			
			previousActEndTime = aa.endTime;
			
			if(i<trips.size()) {
				LegGson l = new LegGson();
				String mode = TripStructureUtils.getRoutingModeIdentifier().identifyMainMode(trips.get(i).getTripElements());
				l.mode = mode;
				l.distance = NetworkUtils.getEuclideanDistance(activities.get(i+1).getCoord(),a.getCoord());
				p.activitiesAndLegs.add(l);
			}
			
		}
		return p;
			
	}
	
	
	public Plan getPlan() {
		Plan plan = PopulationUtils.createPlan();
		for(Object pe:this.activitiesAndLegs) {
			if(pe instanceof ActivityGson) {
				plan.addActivity(((ActivityGson)pe).getActivity());
			}else if(pe instanceof LegGson) {
				plan.addLeg(((LegGson)pe).getLeg());
			}
		}
		return plan;
	}
}