package fi.abo.kogni.soile2.http_server.auth;

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

public class SoileAuthorization{

	public enum TargetElementType
	{
		TASK,
		EXPERIMENT,
		PROJECT,
		INSTANCE
	}
	
	public enum PermissionType
	{
		READ
		{
			public String toString()
			{
				return "READ";
			}
		},
		READ_WRITE
		{
			public String toString()
			{
				return "READ_WRITE";
			}
		},
		FULL {
			public String toString()
			{
				return "FULL";
			}
		}
	}
	
	public enum Roles
	{
		Admin,		
		Researcher,
		Participant 
	}
	
	MongoAuthorization taskAuthorization;
	MongoAuthorization projectAuthorization;
	MongoAuthorization experimentAuthorization;
	MongoAuthorization instanceAuthorization;	
	MongoAuthorizationOptions taskOptions;
	MongoAuthorizationOptions projectOptions;
	MongoAuthorizationOptions experimentOptions;
	MongoAuthorizationOptions instanceOptions;
	MongoClient client;
	
	public SoileAuthorization(MongoClient client)
	{
		this.client = client;
		
		taskOptions = SoileConfigLoader.getMongoTaskAuthorizationOptions();
		projectOptions = SoileConfigLoader.getMongoProjectAuthorizationOptions();
		experimentOptions = SoileConfigLoader.getMongoExperimentAuthorizationOptions();
		instanceOptions = SoileConfigLoader.getMongoInstanceAuthorizationOptions();
		
		taskAuthorization = MongoAuthorization.create("taskProvider", client, taskOptions);
		experimentAuthorization = MongoAuthorization.create("experimentProvider", client, experimentOptions);
		projectAuthorization = MongoAuthorization.create("projectProvider", client, projectOptions);
		instanceAuthorization = MongoAuthorization.create("instanceProvider", client, instanceOptions);		
	}

	public MongoAuthorization getTaskAuthorization() {
		return taskAuthorization;
	}

	public MongoAuthorization getProjectAuthorization() {
		return projectAuthorization;
	}

	public MongoAuthorization getExperimentAuthorization() {
		return experimentAuthorization;
	}

	public MongoAuthorization getInstanceAuthorization() {
		return instanceAuthorization;
	}
	
	/**
	 * Get the permissions for the given user and the specified type of permission
	 * @param user - The user for which to obtain the permissions
	 * @param permissionType The type of permissions one of 
	 * @return
	 */
	public Future<JsonArray> getPermissions(User user, TargetElementType permissionType)
	{
		Promise<JsonArray> permissionPromise = Promise.promise();
		MongoAuthorizationOptions options = getAuthorizationOptionsForOption(permissionType);
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
		return permissionPromise.future();
	}
	
	
	/**
	 * Get the Element IDs of the given type for which the user has any permissions (only return the actual ids not the permission strings)
	 * @param user - The user for which to obtain the permissions
	 * @param permissionType The type of permissions one of 
	 * @return
	 */
	public Future<JsonArray> getGeneralPermissions(User user, TargetElementType permissionType)
	{
		Promise<JsonArray> permissionPromise = Promise.promise();		
		getPermissions(user, permissionType)
		.onSuccess(permissions -> {
			JsonArray result = new JsonArray();
			for(int i = 0; i < permissions.size(); ++i)
			{				
				result.add(permissions.getString(i).substring(permissions.getString(i).indexOf("$")));				
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
	 * @return
	 */
	public Future<JsonArray> getFullPermissions(User user, TargetElementType permissionType)
	{
		Promise<JsonArray> permissionPromise = Promise.promise();		
		getPermissions(user, permissionType)
		.onSuccess(permissions -> {
			JsonArray result = new JsonArray();
			for(int i = 0; i < permissions.size(); ++i)
			{				
				if(permissions.getString(i).substring(0,permissions.getString(i).indexOf("$")).equals(PermissionType.FULL.toString()))
				{
					result.add(permissions.getString(i).substring(permissions.getString(i).indexOf("$")));
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
	 * @return
	 */
	public Future<JsonArray> getWritePermissions(User user, TargetElementType permissionType)
	{
		Promise<JsonArray> permissionPromise = Promise.promise();		
		getPermissions(user, permissionType)
		.onSuccess(permissions -> {
			JsonArray result = new JsonArray();
			for(int i = 0; i < permissions.size(); ++i)
			{				
				if(permissions.getString(i).substring(0,permissions.getString(i).indexOf("$")).equals(PermissionType.FULL.toString()) 
				   || permissions.getString(i).substring(0,permissions.getString(i).indexOf("$")).equals(PermissionType.READ_WRITE.toString()) )
				{
					result.add(permissions.getString(i).substring(permissions.getString(i).indexOf("$")));
				}
			}
			permissionPromise.complete(result);
		})
		.onFailure(err -> permissionPromise.fail(err));
		return permissionPromise.future();
	}
	
	public MongoAuthorization getAuthorizationForOption(TargetElementType option)
	{
		switch(option)
		{
		case TASK: return taskAuthorization;
		case EXPERIMENT: return experimentAuthorization;
		case PROJECT: return projectAuthorization;
		case INSTANCE: return instanceAuthorization;
		default: return null;
		}
	}	
	
	private MongoAuthorizationOptions getAuthorizationOptionsForOption(TargetElementType option)
	{
		switch(option)
		{
		case TASK: return taskOptions;
		case EXPERIMENT: return experimentOptions;
		case PROJECT: return projectOptions;
		case INSTANCE: return instanceOptions;
		default: return null;
		}
	}			
	
}
