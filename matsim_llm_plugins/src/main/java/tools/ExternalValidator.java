package tools;


import java.util.Map;

import tools.IToolResponse;

public interface ExternalValidator<T> {
	
	Class<T> getTargetType();

    /**
     * Name of the tool whose output should be validated
     * e.g. "extract_plan"
     */
    String getTargetToolName();



    /**
     * Validate the candidate against external context
     */
    boolean validate(T candidate, Map<String, Object> context, ErrorMessages em);


}