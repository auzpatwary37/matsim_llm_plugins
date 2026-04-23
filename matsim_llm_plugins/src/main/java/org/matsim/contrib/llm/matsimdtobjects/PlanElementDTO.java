package org.matsim.contrib.llm.matsimdtobjects;

import org.matsim.api.core.v01.population.PlanElement;

import org.matsim.contrib.llm.tools.ToolArgumentDTO;

public abstract class PlanElementDTO<P extends PlanElement> extends ToolArgumentDTO<P> {

    public abstract String getElementType();

}