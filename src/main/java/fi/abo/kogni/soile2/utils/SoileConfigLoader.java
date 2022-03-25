package fi.abo.kogni.soile2.utils;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.impl.future.SucceededFuture;
import io.vertx.core.json.JsonObject;

public class SoileConfigLoader {
	
	public static String SESSION_CFG = "session";
	public static String COMMUNICATION_CFG = "communication";
	public static String DB_FIELDS = "db_fields";
	public static String DB_CFG = "db";
	public static String USERMGR_CFG = "UManagement";
	public static String EXPERIMENT_CFG = "experiments";
	public static String COMMAND_PREFIX_FIELD = "commandPrefix";
	public static String COMMANDS = "commands";
	public static String USERCOLLECTIONS = "userCollections";
	public static String COLLECTIONNAME_FIELD = "collectionName";
	public static String HTTP_SERVER_CFG = "http_server";
	private static JsonObject dbCfg;
	private static JsonObject dbFields;
	private static JsonObject sessionCfg;
	private static JsonObject commCfg;
	private static JsonObject userCfg;
	private static JsonObject expCfg;	
	private static JsonObject collectionsCfg;
	private static JsonObject serverCfg;
	
	
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
	
	
	public static Future<Void> setConfigs(JsonObject config)
	{
		dbCfg = config.getJsonObject(DB_CFG);
		dbFields = config.getJsonObject(DB_FIELDS);
		sessionCfg = config.getJsonObject(SESSION_CFG);
		commCfg = config.getJsonObject(COMMUNICATION_CFG);
		userCfg = config.getJsonObject(USERMGR_CFG);
		expCfg = config.getJsonObject(EXPERIMENT_CFG);
		collectionsCfg = config.getJsonObject(USERCOLLECTIONS);
		serverCfg = config.getJsonObject(HTTP_SERVER_CFG);
		return Future.succeededFuture();
	}
	
	/**
	 * Get a property from the Session config.
	 * @param property - the property to obtain.
	 * @return the property
	 */
	public static String getSessionProperty(String property)
	{
		return sessionCfg.getString(property);
	}
	
	/**
	 * Get a long property from the Session config.
	 * @param property - the property to obtain.
	 * @return the property
	 */
	public static long getSessionLongProperty(String property)
	{
		return sessionCfg.getLong(property);
	}
	
	/**
	 * Get a property from the Server config.
	 * @param property - the property to obtain.
	 * @return the property
	 */
	public static String getServerProperty(String property)
	{
		return serverCfg.getString(property);
	}
	
	/**
	 * Get a property from the database config.
	 * @param property - the property to obtain.
	 * @return the property
	 */
	public static String getdbProperty(String property)
	{
		return dbCfg.getString(property);
	}
	
	/**
	 * Get a property from the User config.
	 * @param property - the property to obtain.
	 * @return the property
	 */
	public static String getUserProperty(String property)
	{
		return userCfg.getString(property);
	}
	
	/**
	 * Get a property from the Communication config.
	 * @param property - the property to obtain.
	 * @return the property
	 */
	public static String getCommunicationField(String property)
	{
		return commCfg.getJsonObject(SoileCommUtils.FIELDID).getString(property);
	}
	
	/**
	 * Get a Result from the Communication config.
	 * @param property - the property to obtain.
	 * @return the property
	 */
	public static String getCommunicationResult(String result)
	{
		return commCfg.getJsonObject(SoileCommUtils.RESULTID).getString(result);
	}
	
	/**
	 * Get a property from the Experiment config.
	 * @param property - the property to obtain.
	 * @return the property
	 */
	public static String getExperimentProperty(String property)
	{
		return expCfg.getString(property);
	}
	
	/**
	 * Get the corresponding database field
	 * @param fieldType the field to retrieve
	 * @return the field name
	 */
	public static String getdbField(String fieldType)
	{
		return dbFields.getString(fieldType);
	}
	

	/**
	 * Get the command representing the sring for the given config
	 * @param config the config that contains commands (and a command prefix)
	 * @param command the command from the config to use.
	 * @return The command string
	 */
	public static String getCommand(JsonObject config, String command)
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
	 * Get the Collection name from the config for a specific user type.
	 * @param config the config that contains the usercollections data
	 * @param collection the collection name to fetch
	 * @return The collection name
	 */
	public static String getCollectionName(String collection)
	{
		return collectionsCfg.getString(collection); 
	}	
	
	/**
	 * Get the command to be issued via the eventbus.
	 * @param command The command entry in the config for which to extract the command.
	 * @return The command string to be handled via the eventbus, or null if the command does not exist
	 */
	public static String getUserCommand(String command)
	{		
		return userCfg.getJsonObject(COMMANDS).getString(command);
	}

	/**
	 * Get the command to be issued via the eventbus.
	 * @param command The command entry in the config for which to extract the command.
	 * @return The command string to be handled via the eventbus, or null if the command does not exist
	 */
	public static String getExperimentCommand(String command)
	{		
		return expCfg.getJsonObject(COMMANDS).getString(command);
	}	
	
}
