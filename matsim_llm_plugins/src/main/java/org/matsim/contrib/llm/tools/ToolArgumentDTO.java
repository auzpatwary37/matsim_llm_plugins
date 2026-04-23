package org.matsim.contrib.llm.tools;

import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public abstract class ToolArgumentDTO <T>{
	
	/**
	 * This should return the original class. 
	 * @param context objects required to create the base class. 
	 * @return
	 */
	public abstract T toBaseClass(Map<String, Object> context, ErrorMessages em);
	
	/**
	 * Should throw VerificationFailedException with the list of error messages to be sent back to the LLM. 
	 * Provides additional verification option after creation is complete. Internal consistency verification.
	 * @return
	 */
	public abstract boolean isVerified(ErrorMessages em,Map<String,Object> context);
	
	/**
	 * Optional hook executed immediately after the DTO is populated from JSON using Gson.
	 *
	 * The default Gson deserialization works for most DTOs, but it cannot automatically
	 * reconstruct nested polymorphic structures (e.g., fields whose concrete type depends
	 * on a discriminator such as "elementType" or "routeType").
	 *
	 * DTO classes that contain such polymorphic or dynamically typed fields may override
	 * this method to perform additional parsing or reconstruction using the raw JSON
	 * representation. Typical use cases include:
	 *   - Resolving nested DTO subclasses based on discriminator fields.
	 *   - Reconstructing fields that Gson cannot infer automatically.
	 *   - Performing additional normalization before verification.
	 *
	 * The default implementation does nothing, which is sufficient for DTOs that only
	 * contain simple fields or non-polymorphic nested objects.
	 *
	 * @param json the original JSON string used to populate this DTO
	 * @param gson the Gson instance used for parsing
	 */
	@Deprecated
	public void afterJsonLoad(String json, Gson gson) {
	        // default: do nothing
	}
	
	/**
	 * Optional hook for DTOs that need custom JSON serialization.
	 * Useful when the DTO contains nested polymorphic fields that Gson
	 * cannot serialize correctly through abstract declared types.
	 *
	 * The default implementation uses standard Gson serialization.
	 *
	 * @param gson the Gson instance used for serialization
	 * @return a JsonObject representation of this DTO
	 */
	public JsonObject toJsonObject(Gson gson) {
	    return gson.toJsonTree(this).getAsJsonObject();
	}
}
