package fi.abo.kogni.soile2.http_server.auth;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.PermissionType;
import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.Roles;
import fi.abo.kogni.soile2.projecthandling.exceptions.ObjectDoesNotExist;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authorization.AndAuthorization;
import io.vertx.ext.auth.authorization.Authorization;
import io.vertx.ext.auth.authorization.OrAuthorization;
import io.vertx.ext.auth.authorization.RoleBasedAuthorization;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.handler.HttpException;

/**
 * Authorization handling for ID based auth. 
 * This is for any end-points with IDs (like tasks/experiments/projects/projectInstances) and allows handling those based on the appropriate 
 * Target collection to check authorizations against.
 * @author Thomas Pfau
 *
 */
public class SoileIDBasedAuthorizationHandler{
			
	static final Logger LOGGER = LogManager.getLogger(SoileIDBasedAuthorizationHandler.class);
	MongoClient client;
	String targetCollection;
	public SoileIDBasedAuthorizationHandler(String targetCollection, MongoClient client)
	{
		this.targetCollection = targetCollection;
		this.client = client;
	}

	/**
	 * Check if the given user has full authorization for the given list of IDs
	 * @param user The user to check
	 * @param IDs the ids to check
	 * @param AdminAllowed whether it would be sufficient if the user is an admin.
	 * @return
	 */
	public boolean checkMultipleFullAuthorizations(User user, List<String> IDs, boolean AdminAllowed)
	{
		AndAuthorization auth = AndAuthorization.create();
		for(String id : IDs)
		{
			auth.addAuthorization(getPermissionBasedAuth(id, PermissionType.FULL));
		}
		Authorization finalAuth;
		if(AdminAllowed)
		{
			finalAuth = OrAuthorization.create().addAuthorization(auth).addAuthorization(RoleBasedAuthorization.create(Roles.Admin.toString()));
		}
		else
		{
			finalAuth = auth;
		}
		if(finalAuth.match(user))
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	/**
	 * Check, whether the given user has authorization to access the given resource based on the required {@link PermissionType}
	 * @param user The user to check the authorization for
	 * @param id the ID of the object to check
	 * @param adminAllowed whether admin access overrides the access restrictions
	 * @param requiredAccess what the required level of access is (higher access overrides lower access).
	 * @return A successful future if the user has the given authorization.
	 */
	public Future<Void> authorize(User user, String id, boolean adminAllowed, PermissionType requiredAccess) {	
		Promise<Void> authorizationPromise = Promise.<Void>promise();
		if(requiredAccess == null)
		{
			authorizationPromise.complete();
			return authorizationPromise.future();
		}
		// Fetch the project with the id, but only return the private field (everything else is not relevant)
		client.findOne(targetCollection,new JsonObject().put("_id", id),new JsonObject().put("private", 1))
		.onSuccess( res -> {
			if( res == null)
			{
				authorizationPromise.fail(new ObjectDoesNotExist(id));
				return;
			}
			else
			{
				// if it is private we need explicit access to it.
				// and for any access point that needs full permissions, 
				if(res.getBoolean("private") || requiredAccess == PermissionType.FULL)
				{

					Authorization auth;
					if(adminAllowed)
					{
						auth = OrAuthorization.create().addAuthorization(getPermissionBasedAuth(id, requiredAccess))
								.addAuthorization(RoleBasedAuthorization.create(Roles.Admin.toString()));
					}
					else
					{
						auth = getPermissionBasedAuth(id, requiredAccess);
					}
					if(auth.match(user))
					{
						authorizationPromise.complete();
					}
					else
					{
						authorizationPromise.fail(new HttpException(403));
					}						

				}
				else
				{					
					// this is not private and no full access is required, so access can be granted without explicit permissions.
					authorizationPromise.complete();					
				}
			}
		})
		.onFailure(err -> failPromise(err,authorizationPromise,500));		
		return authorizationPromise.future();
	}
		
	private Authorization getPermissionBasedAuth(String id, PermissionType requiredAccess)
	{		
		
		if(requiredAccess == PermissionType.FULL)
		{
			return SoilePermissionProvider.buildPermission(id,requiredAccess);
		}
		else
		{
			if(requiredAccess == PermissionType.READ_WRITE)
			{
				return OrAuthorization.create().addAuthorization(SoilePermissionProvider.buildPermission(id, PermissionType.FULL))
											   .addAuthorization(SoilePermissionProvider.buildPermission(id, PermissionType.READ_WRITE));	
			}
			else
			{
				return SoilePermissionProvider.getWildCardPermission(id);
			}
			
		}
	}
	
	private void failPromise(Throwable err, Promise p, int StatusCode)
	{
		if(StatusCode >= 500 || StatusCode == 400)
		{
			LOGGER.error(err);
		}
		p.fail(new HttpException(StatusCode,err.getMessage()));
	}
	
}
