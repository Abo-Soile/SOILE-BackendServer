package fi.abo.kogni.soile2.http_server;

import fi.abo.kogni.soile2.http_server.utils.SoileCommUtils;
import fi.abo.kogni.soile2.http_server.utils.SoileConfigLoader;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;

/**
 * This class is a base verticle class for all verticles that have to handle database IO and need to access 
 * a particular field of the configuration. It further provides access to all eventbus commands that can be issued
 * for this database. 
 * @author thomas
 *
 */
public class SoileBaseVerticle extends AbstractVerticle {
	
//	private String commandPrefix;
	private JsonObject typeSpecificConfig; 	
	private JsonObject communicationConfig;
	private JsonObject dbConfig;
	public final static String COMM_FIELD = "communication_fields";
	
	/**
	 * Set up the individual config for this verticle. The field refers to the field in the general config.  
	 * @param field The field that contains all config options for this verticle.
	 */
	public void setupConfig(String field)
	{
		this.typeSpecificConfig = config().getJsonObject(field);
		//commandPrefix = typeSpecificConfig.getString("commandPrefix");
		communicationConfig = config().getJsonObject(COMM_FIELD);
		dbConfig = config().getJsonObject(SoileConfigLoader.DB_FIELDS);
	}
	
	/**
	 * Get the command to be issued via the eventbus.
	 * @param command The command entry in the config for which to extract the command.
	 * @return The command string to be handled via the eventbus, or null if the command does not exist
	 */
	public String getEventbusCommandString(String command)
	{		
		return SoileConfigLoader.getEventBusCommand(typeSpecificConfig, command);
	}
	
	/**
	 * Get the command string for the given command.
	 * @param command The command entry in the config for which to extract the command.
	 * @return The command string 
	 */
	public String getCommandString(String command)
	{
		return SoileConfigLoader.getCommand(typeSpecificConfig, command);
	}
	
	/**
	 * Get the string representing a specific result.
	 * @param result
	 * @return the requested result string
	 */
	public String getCommunicationResult(String result)
	{
		return SoileCommUtils.getCommunicationResult(communicationConfig,result);		
	}
	
	/**
	 * Request the communication command.
	 * @param field The requested field name
	 * @return the field name
	 */
	public String getCommunicationField(String field)
	{
		return SoileCommUtils.getCommunicationField(communicationConfig, field);	
	}
	
	/**
	 * Get the DB Field for this string
	 * @param entry The element to extract
	 * @return The extracted element
	 */
	public String getDBField(String entry)
	{
		return dbConfig.getString(entry);
	}
	
	
	/**
	 * Get the configuration element for this configuration entry.
	 * @param entry The element to extract
	 * @return The extracted element
	 */
	public String getConfig(String entry)
	{
		return typeSpecificConfig.getString(entry);
	}
	
	/**
	 * Convenience method to get the session Config in a Soile verticle
	 */
	public JsonObject getSessionConfig()
	{
		return SoileConfigLoader.getSessionConfig(config());
	}
}
