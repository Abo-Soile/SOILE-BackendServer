package fi.abo.kogni.soile2.utils;

import io.vertx.core.json.JsonObject;

/**
 * Utils for Event Bus Communication.
 * @author Thomas Pfau
 *
 */
public class SoileCommUtils {

	public static String SUCCESS = "Success";
	public static String RESULTFIELD = "Result";
	public static String DATAFIELD = "DATA";
	public static String FAILED = "Error";
	public static String REASONFIELD = "Reason";
	public static String FIELDID = "Fields";
	public static String RESULTID = "Results";

	public static JsonObject errorObject(String reason)
	{
		return new JsonObject().put(RESULTFIELD, FAILED).put(REASONFIELD, reason);	
	}
	
	public static JsonObject successObject()
	{
		return new JsonObject().put(RESULTFIELD, SUCCESS);	
	}
		
	public static boolean isResultSuccessFull(JsonObject result)
	{
		return result.getString(RESULTFIELD) != null && result.getString(RESULTFIELD).equals(SUCCESS);
	}
	
	/**
	 * Helper function to retrieve Communication Fields.
	 * @param fieldName the name of the field in the communication config to retrieve
	 * @return
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
