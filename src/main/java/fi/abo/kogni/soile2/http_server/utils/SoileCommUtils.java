package fi.abo.kogni.soile2.http_server.utils;

import io.vertx.core.json.JsonObject;

public class SoileCommUtils {

	public static String SUCCESS = "Success";
	public static String RESULTFIELD = "Result";
	public static String FAILED = "Error";
	public static String REASONFIELD = "Reason";
	
	public static JsonObject errorObject(String reason)
	{
		return new JsonObject().put(RESULTFIELD, FAILED).put(REASONFIELD, reason);	
	}
	
	public static JsonObject successObject()
	{
		return new JsonObject().put(RESULTFIELD, SUCCESS);	
	}
		
	public static boolean resultSuccessFull(JsonObject result)
	{
		return result.getString(RESULTFIELD) != null && result.getString(RESULTFIELD).equals(SUCCESS);
	}
}
