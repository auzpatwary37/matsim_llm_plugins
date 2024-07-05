package gsonprocessor;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.population.PopulationUtils;
import org.matsim.facilities.ActivityFacility;

public class ActivityGson extends PlanElementGson{
	public String id;
	public String activityType;
	public Double endTime;
	public String carLocation;
	public String linkId;
	public String facilityId;
	public Coord coord;
	public Double typicalDuration;
	public Double maximumDuration;
	public Double typicalSoc;
	
	public Activity getActivity() {
		Activity act = null;
		if(this.facilityId!=null)act = PopulationUtils.createActivityFromFacilityId(activityType, Id.create(facilityId, ActivityFacility.class));
		else if(this.linkId!=null) act = PopulationUtils.createActivityFromLinkId(activityType,Id.createLinkId(linkId));
		else{
			throw new IllegalArgumentException("Both linkId and Facility id cannot be null!!!");
		}
		act.setCoord(coord);
		if(Double.isFinite(endTime))act.setEndTime(endTime);
		act.setLinkId(Id.createLinkId(linkId));
		act.getAttributes().putAttribute("actSOC", typicalSoc);
		return act;
	}
}
