package fi.abo.kogni.soile2.http_server.utils;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class SoileConfigLoader {
	
	public static String SESSION_CFG = "session";
	public static String COMMUNICATION_FIELDS = "communication";
	public static String DB_FIELDS = "db_fields";
	public static String DB_CFG = "db";
	public static String USERMAGR_CFG = "UManagement";
	public static String EXPERIMENT_CFG = "experiments";
	public static String COMMAND_PREFIX_FIELD = "commandPrefix";
	public static String COMMANDS = "commands";

	
	public static ConfigRetriever getRetriever(Vertx vertx)
	{
		ConfigStoreOptions soileOptions = new ConfigStoreOptions()
				.setType("file")
				.setFormat("json")
				.setConfig(new JsonObject().put("path","soile_config.json"));				
		
		ConfigRetrieverOptions opts = new ConfigRetrieverOptions()
										   .addStore(soileOptions);
										   //.addStore(userManagementOptions);
		
		ConfigRetriever cfgRetriever = ConfigRetriever.create(vertx,opts);
		return cfgRetriever;
	}
	
	/**
	 * Get the eventbus command (including the command prefix) for the given config and the given command string.
	 * @param config the config that contains commands (and a command prefix)
	 * @param command the command from the config to use.
	 * @return the combination of prefix and command string
	 */
	public static String getEventBusCommand(JsonObject config, String command)
	{
		if(config.getJsonObject(COMMANDS) != null)
		{
			return config.getJsonObject(COMMANDS).getString(command); 
		}
		else
		{
			return null;
		}
	}

	/**
	 * Get the command representing the sring for the given config
	 * @param config the config that contains commands (and a command prefix)
	 * @param command the command from the config to use.
	 * @return The command string
	 */
	public static String getCommand(JsonObject config, String command)
	{
		if(config.getString(COMMAND_PREFIX_FIELD) != null)
		{
			return config.getString(COMMAND_PREFIX_FIELD) + config.getJsonObject(COMMANDS).getString(command); 
		}
		else
		{
			return null;
		}
	}
}
