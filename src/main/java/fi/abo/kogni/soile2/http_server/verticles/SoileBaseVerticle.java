package fi.abo.kogni.soile2.http_server.verticles;

import fi.abo.kogni.soile2.utils.SoileCommUtils;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.AbstractVerticle;

/**
 * This class is a base verticle class for all verticles that have to handle database IO and need to access 
 * a particular field of the configuration. It further provides access to all eventbus commands that can be issued
 * for this database. 
 * @author thomas
 *
 */
public class SoileBaseVerticle extends AbstractVerticle {
	
	private String mainField;
	
	/**
	 * Set up the individual config for this verticle. The field refers to the field in the general config.  
	 * @param field The field that contains all config options for this verticle.
	 */
	public void setupConfig(String field)
	{
		mainField = field;		
	}
	
	/**
	 * Get the command to be issued via the eventbus.
	 * @param command The command entry in the config for which to extract the command.
	 * @return The command string to be handled via the eventbus, or null if the command does not exist
	 */
	public String getEventbusCommandString(String command)
	{		
		return SoileCommUtils.getEventBusCommand(mainField, command);
	}
	
	
	/**
	 * Get the DB Field for this string
	 * @param entry The element to extract
	 * @return The extracted element
	 */
	public String getDBField(String entry)
	{
		return SoileConfigLoader.getUserdbField(entry);
	}
	

}
