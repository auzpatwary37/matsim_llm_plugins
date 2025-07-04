package tools;

import java.util.Map;

public abstract class ToolArgumentDTO <T>{
	
	/**
	 * This should return the original class. 
	 * @param context objects required to create the base class. 
	 * @return
	 */
	public abstract T toBaseClass(Map<String, Object> context);
	
	/**
	 * Should throw VerificationFailedException with the list of error messages to be sent back to the LLM. 
	 * Provides additional verification option after creation is complete. Internal consistency verification.
	 * @return
	 */
	public abstract boolean isVerified();
}
