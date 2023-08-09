package fi.abo.kogni.soile2.utils;

import java.util.Set;

import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.TargetElementType;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.mongo.MongoAuthenticationOptions;
import io.vertx.ext.auth.mongo.MongoAuthorizationOptions;

/**
 * Heart of the SOILE2 Configuration loading. 
 * This provides static access to the configuration and needs to be set up BEFORE anything else. 
 * @author Thomas Pfau
 *
 */
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
	public static final String MONGO_CFG = "mongo";

	public static final String Owner = "Owner";
	public static final String Participant = "Participant";
	public static final String Collaborator = "Collaborator";	
	
	private static JsonObject dbCfg;
	private static JsonObject userdbFields;
	private static JsonObject sessionCfg;
	private static JsonObject commCfg;
	private static JsonObject userCfg;
	private static JsonObject expCfg;	
	private static JsonObject serverCfg;
	private static JsonObject taskCfg;
	private static JsonObject verticleCfg;
	private static JsonObject mongoCfg;
	
	private static JsonObject fullConfig;
	
	private static boolean isSetup = false;
	
	/**
	 * Set up the configuration from Vertx. 
	 * @param vertx the Vertx instance to use for loading the configuration
	 * @return A Successfull future if the Configuration was loaded.
	 */
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
	
	
	/**
	 * Set the individual configs from the given JsonObject
	 * @param config
	 */
	private static void setConfigs(JsonObject config)
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
		mongoCfg = config.getJsonObject(MONGO_CFG);
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
	 * Get a boolean property from the Server config.
	 * @param property - the property to obtain.
	 * @return the property
	 */
	public static Boolean getServerBooleanProperty(String property, Boolean defaultValue)
	{
		return serverCfg.getBoolean(property, defaultValue);
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
	 * Get the Mongo config (port etc)
	 * @return the database config {@link JsonObject}
	 */
	public static JsonObject getMongoCfg() {
		return mongoCfg;
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
	 * Get a property from the mongo config.
	 * @param property - the property to obtain.
	 * @return the property
	 */
	public static String getMongoProperty(String property)
	{
		return mongoCfg.getString(property);
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
	 * Check, whether the language and version are possible.
	 * @param language the language (e.g. qmarkup, elang or psychopy
	 * @param version the version (1.0 for elang or qmarkup, and e.g. 2022.2.5 for psychopy)
	 * @return
	 */

	public static boolean isValidTaskType(String language, String version)
	{
		return taskCfg.getJsonObject("availableVersions").getJsonObject(language).getJsonArray("versions",new JsonArray()).contains(version);
	}
	
	/**
	 * Get the mime type for a specific task language
	 * @param language the language (e.g. qmarkup, elang or psychopy or javascript)
	 * @param version the version (1.0 for elang or qmarkup, and e.g. 2022.2.5 for psychopy)
	 * @return
	 */

	public static String getMimeTypeForTaskLanugage(String language)
	{
		return taskCfg.getJsonObject("availableVersions").getJsonObject(language).getString("mimeType");
	}
	
	/**
	 * Get all available code types supported. 
	 * @return the available code types
	 */
	public static JsonArray getAvailableTaskOptions()
	{
		JsonArray result = new JsonArray();
		for(String option : taskCfg.getJsonObject("availableVersions").fieldNames())
		{
			result.add(new JsonObject().put("name", option)
									   .put("mimetype", getMimeTypeForTaskLanugage(option))
									   .put("versions", taskCfg.getJsonObject("availableVersions").getJsonObject(option).getJsonArray("versions")));
		}
		return result;
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
	
	public static MongoAuthorizationOptions getMongoStudyAuthorizationOptions()
	{
		MongoAuthorizationOptions res = new MongoAuthorizationOptions();
		res.setCollectionName(dbCfg.getString("userCollection"));
		res.setPermissionField(getUserdbField("studyPermissionsField"));
		res.setRoleField(getUserdbField("userRolesField"));
		res.setUsernameField(getUserdbField("usernameField"));				
		return res;
	}
	
	/**
	 * Get the database associated with a given element type
	 */
	public static String getDataBaseforElement(TargetElementType type)
	{
		switch(type)
		{
		case TASK: return dbCfg.getString("taskCollection");
		case EXPERIMENT: return dbCfg.getString("experimentCollection");
		case PROJECT: return dbCfg.getString("projectCollection");
		case STUDY: return dbCfg.getString("studyCollection");
		default: return null;
		}
	}
}
