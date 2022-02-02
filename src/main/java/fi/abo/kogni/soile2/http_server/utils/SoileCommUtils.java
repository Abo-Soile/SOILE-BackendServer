package fi.abo.kogni.soile2.http_server.utils;

import io.vertx.core.json.JsonObject;

public class SoileCommUtils {

	public static String SUCCESS = "Success";
	public static String RESULTFIELD = "Result";
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
	 * @param communicationConfig JsonObject representing the communication config
	 * @param fieldName the name of the field in the communication config to retrieve
	 * @return
	 */
	public static String getCommunicationField(JsonObject communicationConfig, String fieldName)
	{
		return communicationConfig.getJsonObject(FIELDID).getString(fieldName);		
	}
	
	public static String getCommunicationResult(JsonObject communicationConfig, String result)
	{
		return communicationConfig.getJsonObject("Results").getString(result);	
	}
		
	
}
