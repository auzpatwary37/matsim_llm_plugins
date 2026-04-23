package org.matsim.contrib.llm.matsimBinding;

import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.selectors.ExpBetaPlanSelector;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class LLMReplanningStrategyProvider implements Provider<PlanStrategy>{
	
	
	@Inject
	private LLMReplanningStrategyModule module;
	
	
	@Inject
	public LLMReplanningStrategyProvider() {
		
	}

	@Override
	public PlanStrategy get() {
		double logitScaleFactor = 1.0;
        PlanStrategyImpl.Builder builder = new PlanStrategyImpl.Builder(new ExpBetaPlanSelector<>(logitScaleFactor));
        
        builder.addStrategyModule(module);
        
        return builder.build();
		
	}

}
