package fi.abo.kogni.soile2.http_server.auth;

import java.lang.annotation.Target;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.abo.kogni.soile2.http_server.routes.SoileRouter;
import fi.abo.kogni.soile2.http_server.userManagement.exceptions.UserDoesNotExistException;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.mongo.MongoAuthorization;
import io.vertx.ext.auth.mongo.MongoAuthorizationOptions;
import io.vertx.ext.mongo.MongoClient;

/**
 * Authorization class for SOILE
 * @author Thomas Pfau
 *
 */
public class SoileAuthorization{

	
	static final Logger LOGGER = LogManager.getLogger(SoileAuthorization.class);	

	/**
	 * Different possible target element types that could be targets of authorization 
	 * @author Thomas Pfau
	 *
	 */
	public enum TargetElementType
	{
		/**
		 * A Task
		 */
		TASK,
		/**
		 * An Experiment
		 */
		EXPERIMENT,
		/**
		 * A Project
		 */
		PROJECT,
		/**
		 * A Study
		 */
		STUDY
	}
	/**
	 * Different permission types
	 * @author Thomas Pfau
	 *
	 */
	public enum PermissionType
	{
		/**
		 * Execute permissions
		 */
		EXECUTE,
		/**
		 * read permissions
		 */
		READ,
		/**
		 *  read + write permissions
		 */
		READ_WRITE,
		/**
		 * full permissions
		 */
		FULL,
		/**
		 * All permissions
		 */
		ALL
	}
	/**
	 * Different roles on the server
	 * @author Thomas Pfau
	 *
	 */
	public enum Roles
	{
		/**
		 * Admin Role
		 */
		Admin,		
		/**
		 * Researcher Role
		 */
		Researcher,
		/**
		 * Participant Role
		 */
		Participant, 
	}
	
	MongoAuthorization taskAuthorization;
	MongoAuthorization projectAuthorization;
	MongoAuthorization experimentAuthorization;
	MongoAuthorization studyAuthorization;	
	MongoAuthorizationOptions taskOptions;
	MongoAuthorizationOptions projectOptions;
	MongoAuthorizationOptions experimentOptions;
	MongoAuthorizationOptions studyOptions;
	MongoClient client;

	/**
	 * Default constructor
	 * @param client the {@link MongoClient} to use for DB communication
	 */
	public SoileAuthorization(MongoClient client)
	{
		this.client = client;
		
		taskOptions = SoileConfigLoader.getMongoTaskAuthorizationOptions();
		projectOptions = SoileConfigLoader.getMongoProjectAuthorizationOptions();
		experimentOptions = SoileConfigLoader.getMongoExperimentAuthorizationOptions();
		studyOptions = SoileConfigLoader.getMongoStudyAuthorizationOptions();
		
		taskAuthorization = MongoAuthorization.create("taskProvider", client, taskOptions);
		experimentAuthorization = MongoAuthorization.create("experimentProvider", client, experimentOptions);
		projectAuthorization = MongoAuthorization.create("projectProvider", client, projectOptions);
		studyAuthorization = MongoAuthorization.create("studyProvider", client, studyOptions);		
	}

	/**
	 * Get a Authorization with a config for the Task Database
	 * @return the requested {@link MongoAuthorization}
	 */
	public MongoAuthorization getTaskAuthorization() {
		return taskAuthorization;
	}

	/**
	 * Get a Authorization with a config for the Project Database
	 * @return the requested {@link MongoAuthorization}
	 */
	public MongoAuthorization getProjectAuthorization() {
		return projectAuthorization;
	}

	/**
	 * Get a Authorization with a config for the Experiment Database
	 * @return the requested {@link MongoAuthorization}
	 */
	public MongoAuthorization getExperimentAuthorization() {
		return experimentAuthorization;
	}

	/**
	 * Get a Authorization with a config for the Study Database
	 * @return the requested {@link MongoAuthorization}
	 */
	public MongoAuthorization getInstanceAuthorization() {
		return studyAuthorization;
	}
	
	/**
	 * Get the permissions for the given user and the specified type of permission
	 * @param user - The user for which to obtain the permissions
	 * @param permissionType The type of permissions one of 
	 * @return A {@link JsonArray} containing permission strings
	 */
	public Future<JsonArray> getPermissions(User user, TargetElementType permissionType)
	{
		Promise<JsonArray> permissionPromise = Promise.promise();
		MongoAuthorizationOptions options = getAuthorizationOptionsForOption(permissionType);
		if(SoileRouter.isTokenUser(user) &&  permissionType == TargetElementType.STUDY) // we only allow this to be used for studies. no other token users allowed.
		{
			permissionPromise.complete(new JsonArray().add(user.principal().getValue("tokenPermission")));
		}
		else
		{
			LOGGER.debug(options.getCollectionName() + " // " + new JsonObject().put(options.getUsernameField(), user.principal().getString("username")).encodePrettily());
			LOGGER.debug(options.getPermissionField());
		
			client.findOne(options.getCollectionName(), 
						   new JsonObject().put(options.getUsernameField(), user.principal().getString("username")),
						   new JsonObject().put(options.getPermissionField(), 1))
			.onSuccess(result -> {
				if(result == null)
				{
					permissionPromise.fail(new UserDoesNotExistException(user.principal().getString("username")));
				}
				else
				{	
					permissionPromise.complete(result.getJsonArray(options.getPermissionField(), new JsonArray()));										
				}
		})
		.onFailure(err -> permissionPromise.fail(err));
		}
		return permissionPromise.future();
	}
	
	
	/**
	 * Get the Element IDs of the given type for which the user has any permissions (only return the actual ids not the permission strings)
	 * @param user - The user for which to obtain the permissions
	 * @param permissionType The type of permissions one of 
	 * @return A {@link JsonArray} containing permission strings
	 */
	public Future<JsonArray> getGeneralPermissions(User user, TargetElementType permissionType)
	{
		Promise<JsonArray> permissionPromise = Promise.promise();		
		getPermissions(user, permissionType)
		.onSuccess(permissions -> {
			JsonArray result = new JsonArray();
			for(int i = 0; i < permissions.size(); ++i)
			{				
				
				result.add(SoilePermissionProvider.getTargetFromPermission(permissions.getString(i)));				
			}
			LOGGER.debug("Finishing Permission retrieval");
			permissionPromise.complete(result);
		})
		.onFailure(err -> permissionPromise.fail(err));
		return permissionPromise.future();
	}

	/**
	 * Get the Element IDs of the given type for which the user has read permissions (only return the actual ids not the permission strings)
	 * @param user - The user for which to obtain the permissions
	 * @param permissionType The type of permissions one of 
	 * @return A {@link JsonArray} containing permission strings
	 */
	public Future<JsonArray> getReadPermissions(User user, TargetElementType permissionType)
	{
		Promise<JsonArray> permissionPromise = Promise.promise();		
		getPermissions(user, permissionType)
		.onSuccess(permissions -> {
			JsonArray result = new JsonArray();
			for(int i = 0; i < permissions.size(); ++i)
			{		
				String type = SoilePermissionProvider.getTypeFromPermission(permissions.getString(i));
				if(type.equals(PermissionType.FULL.toString()) 
				|| type.equals(PermissionType.READ_WRITE.toString()) 
				|| type.equals(PermissionType.READ.toString()) )
						{
							result.add(SoilePermissionProvider.getTargetFromPermission(permissions.getString(i)));
						}
			}
			permissionPromise.complete(result);
		})
		.onFailure(err -> permissionPromise.fail(err));
		return permissionPromise.future();
	}
	
	/**
	 * Get the Element IDs of the given type for which the user has full permissions (only return the actual ids not the permission strings)
	 * @param user - The user for which to obtain the permissions
	 * @param permissionType The type of permissions one of 
	 * @return A {@link JsonArray} containing permission strings
	 */
	public Future<JsonArray> getFullPermissions(User user, TargetElementType permissionType)
	{
		Promise<JsonArray> permissionPromise = Promise.promise();		
		getPermissions(user, permissionType)
		.onSuccess(permissions -> {
			JsonArray result = new JsonArray();
			for(int i = 0; i < permissions.size(); ++i)
			{				
				if(SoilePermissionProvider.getTypeFromPermission(permissions.getString(i)).equals(PermissionType.FULL.toString()))
				{
					result.add(SoilePermissionProvider.getTargetFromPermission(permissions.getString(i)));
				}
			}
			permissionPromise.complete(result);
		})
		.onFailure(err -> permissionPromise.fail(err));
		return permissionPromise.future();
	}
	
	/**
	 * Get the Element IDs of the given type for which the user has write or full permissions (only return the actual ids not the permission strings)
	 * @param user - The user for which to obtain the permissions
	 * @param permissionType The type of permissions one of 
	 * @return A {@link JsonArray} containing permission strings
	 */
	public Future<JsonArray> getWritePermissions(User user, TargetElementType permissionType)
	{
		Promise<JsonArray> permissionPromise = Promise.promise();		
		getPermissions(user, permissionType)
		.onSuccess(permissions -> {
			JsonArray result = new JsonArray();
			for(int i = 0; i < permissions.size(); ++i)
			{				
				String type = SoilePermissionProvider.getTypeFromPermission(permissions.getString(i));
				if(type.equals(PermissionType.FULL.toString()) 
				|| type.equals(PermissionType.READ_WRITE.toString())  )
				{
					result.add(SoilePermissionProvider.getTargetFromPermission(permissions.getString(i)));
				}
			}
			permissionPromise.complete(result);
		})
		.onFailure(err -> permissionPromise.fail(err));
		return permissionPromise.future();
	}
	
	/**
	 * Get a Authorization with a config for the requested {@link TargetElementType}
	 * @param option the {@link TargetElementType} 
	 * @return the requested {@link MongoAuthorization}
	 */
	public MongoAuthorization getAuthorizationForOption(TargetElementType option)
	{
		switch(option)
		{
		case TASK: return taskAuthorization;
		case EXPERIMENT: return experimentAuthorization;
		case PROJECT: return projectAuthorization;
		case STUDY: return studyAuthorization;
		default: return null;
		}
	}	
	
	/**
	 * Get a {@link MongoAuthorizationOptions} with a config for the requested {@link TargetElementType}
	 * @param option the {@link TargetElementType} 
	 * @return the requested {@link MongoAuthorizationOptions}
	 */
	private MongoAuthorizationOptions getAuthorizationOptionsForOption(TargetElementType option)
	{
		switch(option)
		{
		case TASK: return taskOptions;
		case EXPERIMENT: return experimentOptions;
		case PROJECT: return projectOptions;
		case STUDY: return studyOptions;
		default: return null;
		}
	}
	
}
