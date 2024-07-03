package gsonprocessor;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.population.PopulationUtils;
import org.matsim.facilities.ActivityFacility;

public class ActivityGson{
	public String id;
	public String activityType;
	public double endTime;
	public String carLocation;
	public String linkId;
	public String facilityId;
	public Coord coord;
	public double typicalDuration;
	public double maximumDuration;
	public double typicalSoc;
	
	public Activity getActivity() {
		Activity act = PopulationUtils.createActivityFromFacilityId(activityType, Id.create(facilityId, ActivityFacility.class));
		act.setCoord(coord);
		if(Double.isFinite(endTime))act.setEndTime(endTime);
		act.setLinkId(Id.createLinkId(linkId));
		act.getAttributes().putAttribute("actSOC", typicalSoc);
		return act;
	}
}
