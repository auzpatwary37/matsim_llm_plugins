package matsimdtobjects;

import org.matsim.api.core.v01.population.PlanElement;

import tools.ToolArgumentDTO;

public abstract class PlanElementDTO<P extends PlanElement> extends ToolArgumentDTO<P> {

    public abstract String getElementType();

}