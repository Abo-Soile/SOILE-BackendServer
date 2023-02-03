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
			
	
	/**
	 * Get the eventbus command (including the command prefix) for the given config and the given command string.
	 * @param config the config that contains commands (and a command prefix)
	 * @param command the command from the config to use.
	 * @return the combination of prefix and command string
	 */
	public static String getEventBusCommand(JsonObject config, String command)
	{
		if(config.getString(SoileConfigLoader.COMMAND_PREFIX_FIELD) != null)
		{
			return config.getString(SoileConfigLoader.COMMAND_PREFIX_FIELD) + config.getJsonObject(SoileConfigLoader.COMMANDS).getString(command); 
		}
		else
		{
			return null;
		}
	}

	/**
	 * Get an event bus command for a the user manager
	 * @param command the command for which to get an eventbus command string
	 * @return the command string.
	 */
	public static String getEventBusCommand(String target, String command)
	{
		
		if(SoileConfigLoader.getStringProperty(target,SoileConfigLoader.COMMAND_PREFIX_FIELD) != null)
		{
			return SoileConfigLoader.getStringProperty(target,SoileConfigLoader.COMMAND_PREFIX_FIELD) + SoileConfigLoader.getCommand(target, command); 
		}
		else
		{
			return null;
		}
	}	
	
	/**
	 * Get an event bus command for a the user manager
	 * @param command the command for which to get an eventbus command string
	 * @return the command string.
	 */
	public static String getUserEventBusCommand(String command)
	{
		if(SoileConfigLoader.getUserProperty(SoileConfigLoader.COMMAND_PREFIX_FIELD) != null)
		{
			return SoileConfigLoader.getUserProperty(SoileConfigLoader.COMMAND_PREFIX_FIELD) + SoileConfigLoader.getCommand(SoileConfigLoader.USERMGR_CFG, command); 
		}
		else
		{
			return null;
		}
	}	
	
}
