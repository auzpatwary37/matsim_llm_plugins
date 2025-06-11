package tools;

import java.util.List;

public abstract class ToolArgumentDTO <T>{
	
	/**
	 * This should return the original class. 
	 * @return
	 */
	public abstract T toBaseClass();
	
	/**
	 * Should throw VerificationFailedException with the list of error messages to be sent back to the LLM. 
	 * Provides additional verification option after creation is complete. Internal consistency verification.
	 * @return
	 */
	public abstract boolean isVerified();
}
