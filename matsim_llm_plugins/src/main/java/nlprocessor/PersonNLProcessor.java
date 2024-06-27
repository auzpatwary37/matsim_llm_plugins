package nlprocessor;

import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.ev.fleet.ElectricFleetSpecification;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;



public class PersonNLProcessor {
	
	public static String buildPersonDescription(Person person, ElectricFleetSpecification fleetSpecs, ActivityFacilities facs) {
        // Extracting person attributes
        Integer ageDummy = (Integer) person.getAttributes().getAttribute("age");
        String bikeAvailability = (String) person.getAttributes().getAttribute("bikeAvailability");
        String carAvail = (String) person.getAttributes().getAttribute("carAvail");
        Integer economicSector = (Integer) person.getAttributes().getAttribute("economic_sector");
        String hasLicense = (String) person.getAttributes().getAttribute("hasLicense");
        Long householdId = (Long) person.getAttributes().getAttribute("household_id");
        Integer householdIncome = (Integer) person.getAttributes().getAttribute("household_income");
        Boolean isCarPassenger = (Boolean) person.getAttributes().getAttribute("isCarPassenger");
        Id<Person> personId = person.getId();
        Integer sex = (Integer) person.getAttributes().getAttribute("sex");

        // Converting dummy values to actual descriptions
        String ageDescription = getAgeDescription(ageDummy);
        String sexDescription = (sex == 1) ? "male" : "female";
        String pronoun = (sex == 1) ? "He" : "She";
        String incomeDescription = getIncomeDescription(householdIncome);
        String carPassengerDescription = isCarPassenger ? pronoun + " is a car passenger." : pronoun + " is not a car passenger.";

        boolean ifHasEv = fleetSpecs.getVehicleSpecifications().containsKey(Id.create(personId.toString(), ElectricVehicle.class));
        
        //boolean ifHasEv = fleetSpecs.getVehicleSpecifications().containsKey(Id.create(personId.toString(), ElectricVehicle.class));
        String vehicleOwnership;
        if (ifHasEv) {
            vehicleOwnership = pronoun + " owns an electric vehicle (EV).";
        } else if (!"never".equalsIgnoreCase(carAvail)) {
            vehicleOwnership = pronoun + " owns a gasoline car.";
        } else {
            vehicleOwnership = pronoun + " does not own a car.";
        }

        // Building the description string
        String description = String.format(
            "Person ID %s is a %s aged between %s years old. %s belongs to household ID %d and has a household income of %s per year. " +
            "%s holds a driver's license (%s) and %s car availability is '%s'. " +
            "%s works in the economic sector %d. %s Additionally, %s has bike availability for '%s'. %s",
            personId.toString(), sexDescription, ageDescription, pronoun, householdId, incomeDescription, pronoun, hasLicense, pronoun.toLowerCase(), carAvail, pronoun, economicSector, carPassengerDescription, pronoun.toLowerCase(), bikeAvailability, vehicleOwnership
        );
        
        description = description+buildPlanDescription(person.getSelectedPlan(),facs);

        return description;
    }
	
	public static String buildPersonDescription(Person person, ActivityFacilities facs) {
        // Extracting person attributes
        Integer ageDummy = (Integer) person.getAttributes().getAttribute("age");
        String bikeAvailability = (String) person.getAttributes().getAttribute("bikeAvailability");
        String carAvail = (String) person.getAttributes().getAttribute("carAvail");
        Integer economicSector = (Integer) person.getAttributes().getAttribute("economic_sector");
        String hasLicense = (String) person.getAttributes().getAttribute("hasLicense");
        Long householdId = (Long) person.getAttributes().getAttribute("household_id");
        Integer householdIncome = (Integer) person.getAttributes().getAttribute("household_income");
        Boolean isCarPassenger = (Boolean) person.getAttributes().getAttribute("isCarPassenger");
        Id<Person> personId = person.getId();
        Integer sex = (Integer) person.getAttributes().getAttribute("sex");

        // Converting dummy values to actual descriptions
        String ageDescription = getAgeDescription(ageDummy);
        String sexDescription = (sex == 1) ? "male" : "female";
        String pronoun = (sex == 1) ? "He" : "She";
        String incomeDescription = (householdIncome ==null)?"not available":getIncomeDescription(householdIncome);
        String carPassengerDescription = isCarPassenger ? pronoun + " is a car passenger." : pronoun + " is not a car passenger.";

//        boolean ifHasEv = fleetSpecs.getVehicleSpecifications().containsKey(Id.create(personId.toString(), ElectricVehicle.class));
//        
//        //boolean ifHasEv = fleetSpecs.getVehicleSpecifications().containsKey(Id.create(personId.toString(), ElectricVehicle.class));
        String vehicleOwnership;
//        if (ifHasEv) {
//            vehicleOwnership = pronoun + " owns an electric vehicle (EV).";
//        } else 
        if (!"never".equalsIgnoreCase(carAvail)) {
            vehicleOwnership = pronoun + " owns a gasoline or electric car.";
        } else {
            vehicleOwnership = pronoun + " does not own a car.";
        }
        
        // Building the description string
        String description = String.format(
            "Person ID %s is a %s aged between %s years old. %s belongs to household ID %d and has a household income of %s per year. " +
            "%s holds a driver's license (%s) and %s car availability is '%s'. " +
            "%s works in the economic sector %d. %s Additionally, %s has bike availability for '%s'. %s",
            personId.toString(), sexDescription, ageDescription, pronoun, householdId, incomeDescription, pronoun, hasLicense, pronoun.toLowerCase(), carAvail, pronoun, economicSector, carPassengerDescription, pronoun.toLowerCase(), bikeAvailability, vehicleOwnership
        );
        
        description = description+buildPlanDescription(person.getSelectedPlan(),facs);

        return description;
    }

	public static String interpretPersonPlan(Person person, ActivityFacilities facilities) {
        String personID = person.getId().toString();
        String age = Double.toString((Double)person.getAttributes().getAttribute("age")*5);
        String gender = person.getAttributes().getAttribute("gender").toString();
        String employmentStatus = person.getAttributes().getAttribute("employmentStatus").toString();
        String incomeGroup = person.getAttributes().getAttribute("income").toString();
        
        Plan selectedPlan = person.getSelectedPlan();
        double planScore = selectedPlan.getScore();
        
        StringBuilder interpretation = new StringBuilder();
        interpretation.append(String.format(
            "Person %s is a %s-year-old %s who is %s. "
            + "Their selected daily plan, which has a score of %.1f, ",
            personID, age, gender, employmentStatus, planScore
        ));
        
        Activity previousActivity = null;
        for (PlanElement element : selectedPlan.getPlanElements()) {
            if (element instanceof Activity) {
                Activity activity = (Activity) element;
                String activityType = activity.getType();
                Id<ActivityFacility> facilityId = activity.getFacilityId();
                ActivityFacility facility = facilities.getFacilities().get(facilityId);
                double xCoord = facility.getCoord().getX();
                double yCoord = facility.getCoord().getY();
                String startTime = activity.getStartTime().isUndefined() ? "start time undefined" : activity.getStartTime().toString();
                String endTime = activity.getEndTime().isUndefined() ? "end time undefined" : activity.getEndTime().toString();
                String linkId = facility.getLinkId().toString();
                String openingHours = facility.getActivityOptions().get(activityType).getOpeningTimes().toString();

                if (previousActivity == null) {
                    interpretation.append(String.format(
                        "involves starting the day at %s, located at facility %s with coordinates (%.2f, %.2f), " +
                        "on link %s, which opens during %s. ",
                        activityType, facilityId, xCoord, yCoord, linkId, openingHours
                    ));
                } else {
                    interpretation.append(String.format(
                        "to reach their next activity, %s, at facility %s with coordinates (%.2f, %.2f), " +
                        "on link %s, which opens during %s. This activity lasts from %s to %s. ",
                        activityType, facilityId, xCoord, yCoord, linkId, openingHours, startTime, endTime
                    ));
                }
                previousActivity = activity;
            } else if (element instanceof Leg) {
                Leg leg = (Leg) element;
                String mode = leg.getMode();
                Double travelTime = leg.getTravelTime().seconds();
                double routeDistance = leg.getRoute().getDistance();
                String routeLinks = leg.getRoute().getRouteDescription();
                
                interpretation.append(String.format(
                    "They travel by %s for %.1f minutes, covering a distance of %.2f km via route links %s, ",
                    mode, travelTime / 60, routeDistance / 1000, routeLinks
                ));
            }
        }
        
        // Ensure the last activity is appropriately ended
        if (previousActivity != null) {
            Id<ActivityFacility> facilityId = previousActivity.getFacilityId();
            ActivityFacility facility = facilities.getFacilities().get(facilityId);
            String linkId = facility.getLinkId().toString();
            String openingHours = facility.getActivityOptions().get(previousActivity.getType()).getOpeningTimes().toString();
            interpretation.append(String.format(
                "arriving at facility %s with coordinates (%.2f, %.2f), on link %s, which opens during %s, " +
                "for their final activity of the day, %s, from %s to %s.",
                facilityId, facility.getCoord().getX(), facility.getCoord().getY(), linkId, openingHours,
                previousActivity.getType(), previousActivity.getStartTime(), previousActivity.getEndTime()
            ));
        }
        interpretation.append("Assume empty string for any missing data. Some of the times maybe undefined.");
        return interpretation.toString();
        
        
        
    }

	 private static String getAgeDescription(int ageDummy) {
	        switch (ageDummy) {
	            case 1:
	                return "0-15";
	            case 2:
	                return "16-20";
	            case 3:
	                return "21-25";
	            case 4:
	                return "26-30";
	            case 5:
	                return "31-35";
	            case 6:
	                return "36-40";
	            case 7:
	                return "41-45";
	            case 8:
	                return "46-50";
	            case 9:
	                return "51-55";
	            case 10:
	                return "56-60";
	            case 11:
	                return "61-65";
	            case 12:
	                return "66-70";
	            case 13:
	                return "71-75";
	            case 14:
	                return "75 and above";
	            default:
	                return "unknown";
	        }
	    }

	    private static String getIncomeDescription(int incomeDummy) {
	        switch (incomeDummy) {
	            case 1:
	                return "less than $50,000";
	            case 2:
	                return "$50,000-$90,000";
	            case 3:
	                return "$90,000-$120,000";
	            case 4:
	                return "$120,000-$150,000";
	            case 5:
	                return "$150,000 and above";
	            default:
	                return "unknown";
	        }
	    }
	    
	    public static String buildPlanDescription(Plan plan, ActivityFacilities facilities) {
	        List<PlanElement> planElements = plan.getPlanElements();
	        StringBuilder description = new StringBuilder();
	        description.append("The individual starts their day at ");

	        Activity previousActivity = null;
	        boolean inPtTrip = false;

	        for (PlanElement element : planElements) {
	            if (element instanceof Activity) {
	                Activity activity = (Activity) element;
	                Id<ActivityFacility> facilityId = activity.getFacilityId();
	                ActivityFacility facility = facilities.getFacilities().get(facilityId);

	                if ("pt interaction".equals(activity.getType())) {
	                    inPtTrip = true;
	                    description.append(" They interact with public transport. ");
	                    previousActivity = activity;
	                    continue;
	                } else {
	                    inPtTrip = false;
	                }

	                // Calculate duration if start and end times are defined
	                String durationStr = "";
	                if (activity.getStartTime().isDefined() && activity.getEndTime().isDefined()) {
	                    double duration = activity.getEndTime().seconds() - activity.getStartTime().seconds();
	                    durationStr = String.format("The activity lasts %s.", Time.writeTime(duration));
	                }

	                // Get facility details
	                String facilityDetails = getFacilityDetails(facility);

	                if (previousActivity != null) {
	                    // Calculate Euclidean distance from previous activity
	                    double distance = CoordUtils.calcEuclideanDistance(previousActivity.getCoord(), activity.getCoord());
	                    description.append(String.format(" They travel approximately %.2f meters to ", distance));
	                }

	                description.append(String.format("%s at link %s, facility %s. %s %s", 
	                        activity.getType(), activity.getLinkId(), facilityId, facilityDetails, durationStr));

	                if (activity.getEndTime().isDefined()) {
	                    description.append(String.format(" They leave at %s.", Time.writeTime(activity.getEndTime().seconds())));
	                }
	                previousActivity = activity;
	            } else if (element instanceof Leg) {
	                Leg leg = (Leg) element;
	                if (inPtTrip) {
	                    description.append(" They continue their public transport trip.");
	                } else {
	                    description.append(String.format(" They leave at %s and take %s.", 
	                            Time.writeTime(leg.getDepartureTime().seconds()), leg.getMode()));
	                }
	                if (leg.getTravelTime() != null && leg.getTravelTime().isDefined()) {
	                    description.append(String.format(" The journey lasts %s.", Time.writeTime(leg.getTravelTime().seconds())));
	                }
	            }
	        }

	        return description.toString();
	    }

	        private static String getFacilityDetails(ActivityFacility facility) {
	            if (facility == null) {
	                return "No facility details available.";
	            }

	            StringBuilder facilityDetails = new StringBuilder();
	            facilityDetails.append("The facility allows the following activities: ");

	            Map<String, ActivityOption> activityOptions = facility.getActivityOptions();
	            if (activityOptions.isEmpty()) {
	                facilityDetails.append("None.");
	            } else {
	                for (Map.Entry<String, ActivityOption> entry : activityOptions.entrySet()) {
	                    facilityDetails.append(entry.getKey()).append(" with capacity ");
	                    if(Double.isFinite(entry.getValue().getCapacity()) && entry.getValue().getCapacity()>100) {
	                    	facilityDetails.append(entry.getValue().getCapacity()).append(" ");
	                    }else {
	                    	facilityDetails.append("not available").append(" ");
	                    }
	                    if (entry.getValue().getOpeningTimes() != null && entry.getValue().getOpeningTimes().size()!=0) {
	                        facilityDetails.append("and opening time at ").append(Time.writeTime(entry.getValue().getOpeningTimes().first().getStartTime())).append(" ");
	                        facilityDetails.append("and closing time at ").append(Time.writeTime(entry.getValue().getOpeningTimes().first().getEndTime())).append(". ");
		                    
	                    }
	                   
	                       
	                }
	            }

	            return facilityDetails.toString();
	        }

	        
    public static void main(String[] args) {
        // Example usage with a mock Person object and ActivityFacilities
        // This should be replaced with actual data from a MATSim scenario
        //Person person = ...; // Obtain a Person object from the MATSim population
       // ActivityFacilities facilities = ...; // Obtain ActivityFacilities object from the MATSim scenario
        
        //String result = interpretPersonPlan(person, facilities);
        //System.out.println(result);
    }

}
