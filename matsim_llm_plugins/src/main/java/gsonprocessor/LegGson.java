package gsonprocessor;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.population.PopulationUtils;

public class LegGson {
	public String mode;
	public double distance;
	public Leg getLeg() {
		Leg leg = PopulationUtils.createLeg(mode);
		return leg;
	}
}
