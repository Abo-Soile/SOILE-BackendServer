package fi.abo.kogni.soile2.utils;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.impl.future.SucceededFuture;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.mongo.MongoAuthenticationOptions;
import io.vertx.ext.auth.mongo.MongoAuthorizationOptions;

public class SoileConfigLoader {
	
	public static final String SESSION_CFG = "session";
	public static final String COMMUNICATION_CFG = "communication";
	public static final String USER_DB_FIELDS = "db_user_fields";	
	public static final String DB_CFG = "db";
	public static final String USERMGR_CFG = "UManagement";
	public static final String EXPERIMENT_CFG = "experiments";
	public static final String COMMAND_PREFIX_FIELD = "commandPrefix";
	public static final String COMMANDS = "commands";
	public static final String HTTP_SERVER_CFG = "http_server";
	public static final String TASK_CFG = "tasks";
	public static final String VERTICLE_CFG = "verticles";

	public static final String Owner = "Owner";
	public static final String Participant = "Participant";
	public static final String Collaborator = "Collaborator";	
	
	public static final String EXPERIMENT = "experiment";
	public static final String PROJECT = "project";
	public static final String TASK = "task";
	public static final String INSTANCE = "instance";	
	
	private static JsonObject dbCfg;
	private static JsonObject userdbFields;
	private static JsonObject sessionCfg;
	private static JsonObject commCfg;
	private static JsonObject userCfg;
	private static JsonObject expCfg;	
	private static JsonObject serverCfg;
	private static JsonObject taskCfg;
	private static JsonObject verticleCfg;
	
	private static JsonObject fullConfig;
	
	private static boolean isSetup = false;
	
	public static Future<Void> setupConfig(Vertx vertx)
	{
		if(isSetup)
		{
			return Future.succeededFuture();
		}
		Promise<Void> configPromise = Promise.promise();
		
		ConfigStoreOptions soileOptions = new ConfigStoreOptions()
				.setType("file")
				.setFormat("json")
				.setConfig(new JsonObject().put("path","soile_config.json"));				
		
		ConfigRetrieverOptions opts = new ConfigRetrieverOptions()
										   .addStore(soileOptions);
										   //.addStore(userManagementOptions);
		
		ConfigRetriever retriever = ConfigRetriever.create(vertx,opts);
		retriever.getConfig()
		.onSuccess(configObject -> {
			SoileConfigLoader.setConfigs(configObject);
			isSetup = true;
			configPromise.complete();
		})
		.onFailure(err -> configPromise.fail(err));
		return configPromise.future();
	}
	
	
	public static void setConfigs(JsonObject config)
	{
		dbCfg = config.getJsonObject(DB_CFG);
		userdbFields = config.getJsonObject(USER_DB_FIELDS);
		sessionCfg = config.getJsonObject(SESSION_CFG);
		commCfg = config.getJsonObject(COMMUNICATION_CFG);
		userCfg = config.getJsonObject(USERMGR_CFG);
		expCfg = config.getJsonObject(EXPERIMENT_CFG);
		serverCfg = config.getJsonObject(HTTP_SERVER_CFG);
		taskCfg = config.getJsonObject(TASK_CFG);
		verticleCfg = config.getJsonObject(VERTICLE_CFG);
		fullConfig = config;
	}
	
	/**
	 * Get the complete configuration
	 * @return
	 */
	public static JsonObject config()
	{
		return fullConfig;				
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
	 * Get a property from the Server config.
	 * @param property - the property to obtain.
	 * @return the property
	 */
	public static int getServerIntProperty(String property)
	{
		return serverCfg.getInteger(property);
	}
	
	/**
	 * Get the database config
	 * @return the database config {@link JsonObject}
	 */
	public static JsonObject getDbCfg() {
		return dbCfg;
	}

	/**
	 * Get the configuration object for a specific target.
	 * @param target  The name of the target configuration
	 * @return the JsonObject for the target configuration
	 */
	public static JsonObject getConfig(String target)
	{
		switch(target)
		{
		case SESSION_CFG :
				return sessionCfg;
		case COMMUNICATION_CFG :
				return commCfg;
		case USER_DB_FIELDS :
				return userdbFields;
		case DB_CFG :
				return dbCfg;
		case USERMGR_CFG :
				return userCfg;
		case EXPERIMENT_CFG :
				return expCfg;				
		case HTTP_SERVER_CFG :
				return serverCfg;
		case VERTICLE_CFG :
			return verticleCfg;
		default:
				return null;
		}
	}
	
	/**
	 * Get a property from the given target 
	 * @param target The target from which to retrieve a property
	 * @param property the property to retrieve
	 * @return the retrieved property
	 */
	public static String getStringProperty(String target, String property)
	{
		JsonObject cfg = getConfig(target);
		if(cfg != null)
		{
			return cfg.getString(property);
		}
		return null;
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
	 * Get a property from the Task config.
	 * @param property - the property to obtain.
	 * @return the property
	 */
	public static String getTaskProperty(String property)
	{
		return taskCfg.getString(property);
	}
	
	/**
	 * Get a property from the Verticle config.
	 * @param property - the property to obtain.
	 * @return the property
	 */
	public static String getVerticleProperty(String property)
	{
		return verticleCfg.getString(property);
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
	public static String getUserdbField(String fieldType)
	{
		return userdbFields.getString(fieldType);
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
	 * Get the command representing the string for the given target config
	 * @param config the config that contains commands (and a command prefix)
	 * @param command the command from the config to use.
	 * @return The command string
	 */
	public static String getCommand(String target, String command)
	{
		JsonObject config = getConfig(target);
		return getCommand(config, command);
	}
	
	/**
	 * Get the Collection name from the config for a specific user type.
	 * @param config the config that contains the usercollections data
	 * @param collection the collection name to fetch
	 * @return The collection name
	 */
	public static String getCollectionName(String collection)
	{
		return dbCfg.getString(collection); 
	}	
	
	
	// TODO: Move these to SioleAuthorization!
	public static MongoAuthenticationOptions getMongoAuthNOptions()
	{
		MongoAuthenticationOptions res = new MongoAuthenticationOptions();
		res.setCollectionName(dbCfg.getString("userCollection"));
		res.setPasswordCredentialField(getUserdbField("passwordCredentialField"));
		res.setPasswordField(getUserdbField("passwordField"));
		res.setUsernameCredentialField(getUserdbField("usernameCredentialField"));
		res.setUsernameField(getUserdbField("usernameField"));				
		return res;
	}
	
	public static MongoAuthorizationOptions getMongoAuthZOptions()
	{
		MongoAuthorizationOptions res = new MongoAuthorizationOptions();
		res.setCollectionName(dbCfg.getString("userCollection"));
		res.setPermissionField(getUserdbField("userPermissionsField"));
		res.setRoleField(getUserdbField("userRolesField"));
		res.setUsernameField(getUserdbField("usernameField"));				
		return res;
	}
	
	public static MongoAuthorizationOptions getMongoTaskAuthorizationOptions()
	{
		MongoAuthorizationOptions res = new MongoAuthorizationOptions();
		res.setCollectionName(dbCfg.getString("userCollection"));
		res.setPermissionField(getUserdbField("taskPermissionsField"));
		res.setRoleField(getUserdbField("userRolesField"));
		res.setUsernameField(getUserdbField("usernameField"));				
		return res;
	}
	
	public static MongoAuthorizationOptions getMongoExperimentAuthorizationOptions()
	{
		MongoAuthorizationOptions res = new MongoAuthorizationOptions();
		res.setCollectionName(dbCfg.getString("userCollection"));
		res.setPermissionField(getUserdbField("experimentPermissionsField"));
		res.setRoleField(getUserdbField("userRolesField"));
		res.setUsernameField(getUserdbField("usernameField"));				
		return res;
	}
	
	public static MongoAuthorizationOptions getMongoProjectAuthorizationOptions()
	{
		MongoAuthorizationOptions res = new MongoAuthorizationOptions();
		res.setCollectionName(dbCfg.getString("userCollection"));
		res.setPermissionField(getUserdbField("projectPermissionsField"));
		res.setRoleField(getUserdbField("userRolesField"));
		res.setUsernameField(getUserdbField("usernameField"));				
		return res;
	}
	
	public static MongoAuthorizationOptions getMongoInstanceAuthorizationOptions()
	{
		MongoAuthorizationOptions res = new MongoAuthorizationOptions();
		res.setCollectionName(dbCfg.getString("userCollection"));
		res.setPermissionField(getUserdbField("instancePermissionsField"));
		res.setRoleField(getUserdbField("userRolesField"));
		res.setUsernameField(getUserdbField("usernameField"));				
		return res;
	}
}
