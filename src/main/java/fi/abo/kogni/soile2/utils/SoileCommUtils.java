package fi.abo.kogni.soile2.utils;

import io.vertx.core.json.JsonObject;

/**
 * Utils for Event Bus Communication.
 * @author Thomas Pfau
 *
 */
public class SoileCommUtils {

	/**
	 * Success value
	 */
	public static String SUCCESS = "Success";
	/**
	 * Result field
	 */
	public static String RESULTFIELD = "Result";
	/**
	 * Data Field
	 */
	public static String DATAFIELD = "DATA";
	/**
	 * Error Value
	 */
	public static String FAILED = "Error";
	/**
	 * Reason field (for errors)
	 */
	public static String REASONFIELD = "Reason";
	/**
	 * Field ID
	 */
	public static String FIELDID = "Fields";
	/**
	 * Result ID
	 */
	public static String RESULTID = "Results";

	/**
	 * Create an Error Object
	 * @param reason the reason for the error
	 * @return the JsonObjectrepresenting the error  
	 */
	public static JsonObject errorObject(String reason)
	{
		return new JsonObject().put(RESULTFIELD, FAILED).put(REASONFIELD, reason);	
	}
	
	/**
	 * Create a success Object
	 * @return the JsonObject representing success  
	 */
	public static JsonObject successObject()
	{
		return new JsonObject().put(RESULTFIELD, SUCCESS);	
	}
		
	/**
	 * Check a result object for success
	 * @param result the object to check
	 * @return whether the object is a success object  
	 */
	public static boolean isResultSuccessFull(JsonObject result)
	{
		return result.getString(RESULTFIELD) != null && result.getString(RESULTFIELD).equals(SUCCESS);
	}
	
	/**
	 * Helper function to retrieve Communication Fields.
	 * @param fieldName the name of the field in the communication config to retrieve
	 * @return The field name
	 */
	public static String getCommunicationField(String fieldName)
	{
		return SoileConfigLoader.getCommunicationField(fieldName);		
	}
	
	/**
	 * Get the results object for a specific result field.
	 * @param result the result field to obtain
	 * @return the String indicating the result field
	 */
	public static String getCommunicationResult(String result)
	{
		return SoileConfigLoader.getCommunicationResult(result);	
	}

	
}
