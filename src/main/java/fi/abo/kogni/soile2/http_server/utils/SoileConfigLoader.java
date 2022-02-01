package fi.abo.kogni.soile2.http_server.utils;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class SoileConfigLoader {
	
	public static String SESSIONFIELDS = "session";
	public static String COMMUNICATION_FIELDS = "communication";
	public static String DB_FIELDS = "db_fields";
	public static String DB = "db";
	public static String USERMANAGEMENTFIELDS = "UManagement";
	public static String EXPERIMENTFIELDS = "experiments";
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
	
	
	public static String getCommand(JsonObject config, String command)
	{
		return config.getString(COMMAND_PREFIX_FIELD) + config.getJsonObject(COMMANDS).getString(command); 
	}
}
