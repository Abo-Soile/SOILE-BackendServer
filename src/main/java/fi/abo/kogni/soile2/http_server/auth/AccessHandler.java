package fi.abo.kogni.soile2.http_server.auth;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.PermissionType;
import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.Roles;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.mongo.MongoAuthorization;
import io.vertx.ext.web.handler.HttpException;

/**
 * A Handler for Access to the Server
 * @author Thomas Pfau
 *
 */
public class AccessHandler{
	
	MongoAuthorization mongoAuth;
	SoileIDBasedAuthorizationHandler idAccessHandler;
	SoileRoleBasedAuthorizationHandler roleHandler;
	private static final Logger LOGGER = LogManager.getLogger(AccessHandler.class);

	/**
	 * Default Cosntructor
	 * @param mongoAuth the mongo Authentication to use
	 * @param studyIDAccessHandler The {@link SoileIDBasedAuthorizationHandler} used by the AccessHandler
	 * @param roleHandler the {@link SoileRoleBasedAuthorizationHandler} used by the AccessHandler
	 */
	public AccessHandler(MongoAuthorization mongoAuth, SoileIDBasedAuthorizationHandler studyIDAccessHandler,
			SoileRoleBasedAuthorizationHandler roleHandler) {
		super();
		this.mongoAuth = mongoAuth;
		this.idAccessHandler = studyIDAccessHandler;
		this.roleHandler = roleHandler;
	}

	/**
	 * Check whether the given user has the access necessary for the given ID.
	 * @param user The user to check
	 * @param id the object ID 
	 * @param requiredRole the required role (can be <code>null</code>)
	 * @param requiredPermission the minimum required permission 
	 * @param adminAllowed whether admin access overrides any requirements
	 * @return A successful future if the user has the required permissions.
	 */
	public Future<Void> checkAccess(User user, String id, Roles requiredRole, PermissionType requiredPermission, boolean adminAllowed) {
		return checkAccess(user, id, requiredRole, requiredPermission, adminAllowed, mongoAuth, idAccessHandler);
	}
	/**
	 * Check whether the specified ID is linked to a private object
	 * @param id The ID to check
	 * @return A {@link Future} of whether the ID is private
	 */
	public Future<Boolean> checkRestricted(String id)
	{
		return idAccessHandler.checkIsPrivate(id);
	}
	
	/**
	 * Helper function for access checks
	 * @param user the user to check
	 * @param id the target id to check
	 * @param requiredRole the required role to test
	 * @param requiredPermission the required permission level
	 * @param adminAllowed whether admins are allowed to run this
	 * @param authProvider the {@link MongoAuthorization} to get authorization details
	 * @param IDAccessHandler the {@link SoileIDBasedAuthorizationHandler} for id based access
	 * @return A successful {@link Future} if access is ok with for the given user
	 */
	protected Future<Void> checkAccess(User user, String id, Roles requiredRole, PermissionType requiredPermission,
			boolean adminAllowed, MongoAuthorization authProvider, SoileIDBasedAuthorizationHandler IDAccessHandler)
	{
		if(user == null)
		{
			LOGGER.error("No user found for request");
			return Future.failedFuture(new HttpException(403,"Not authenticated"));
		}
		Promise<Void> accessPromise = Promise.<Void>promise();
		authProvider.getAuthorizations(user)
		.onSuccess(Void -> {
			roleHandler.authorize(user, requiredRole)
			.onSuccess(acceptRole -> {
				if( id != null)
				{
					IDAccessHandler.authorize(user, id, adminAllowed, requiredPermission)
					.onSuccess(acceptID -> {
						// both role and permission checks are successfull.
						accessPromise.complete();
					})
					.onFailure(err -> accessPromise.fail(err));
				}
				else
				{
					accessPromise.complete();
				}
			})
			.onFailure(err -> accessPromise.fail(err));			
		})
		.onFailure(err -> {
			accessPromise.fail(new HttpException(500,err.getMessage()));
		});

		return accessPromise.future();
	}
	
}
