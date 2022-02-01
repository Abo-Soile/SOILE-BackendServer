package fi.abo.kogni.soile2.http_server;

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
public class SoileEventVerticle extends AbstractVerticle {
	
//	private String commandPrefix;
	private JsonObject typeSpecificConfig; 	
	private JsonObject communicationConfig;
	private JsonObject dbConfig;
	public final static String COMM_FIELD = "communication_fields";
	public final static String DB_FIELD = "db_fields";
	
	/**
	 * Set up the individual config for this verticle. The field refers to the field in the general config.  
	 * @param field The field that contains all config options for this verticle.
	 */
	public void setupConfig(String field)
	{
		this.typeSpecificConfig = config().getJsonObject(field);
		//commandPrefix = typeSpecificConfig.getString("commandPrefix");
		communicationConfig = config().getJsonObject(COMM_FIELD);
		dbConfig = config().getJsonObject(DB_FIELD);
	}
	
	/**
	 * Get the command to be issued via the eventbus.
	 * @param Command The command entry in the config for which to extract the command.
	 * @return The command string to be handled via the eventbus, or null if the command does not exist
	 */
	public String getEventbusCommandString(String Command)
	{		
		JsonObject commands = typeSpecificConfig.getJsonObject("commands");
		if (commands instanceof JsonObject)
		{
			return SoileConfigLoader.getCommand(typeSpecificConfig, Command);
		}
		else
		{
			return null;
		}
			
	}
	
	/**
	 * Get the string representing a specific result.
	 * @param result
	 * @return the requested result string
	 */
	public String getCommunicationResult(String result)
	{
		return communicationConfig.getJsonObject("Results").getString(result);		
	}
	
	/**
	 * Request the communication command.
	 * @param field The requested field name
	 * @return the field name
	 */
	public String getCommunicationField(String field)
	{
		return communicationConfig.getJsonObject("Fields").getString(field);	
	}
	
	/**
	 * Get the command to be issued via the eventbus.
	 * @param Command The command entry in the config for which to extract the command.
	 * @return The command string to be handled via the eventbus, or null if the command does not exist
	 */
	public String getCommandString(String Command)
	{		
		JsonObject commands = typeSpecificConfig.getJsonObject("commands");
		if (commands instanceof JsonObject)
		{
			return typeSpecificConfig.getJsonObject("commands").getString(Command);
		}
		else
		{
			return null;
		}
			
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
}
