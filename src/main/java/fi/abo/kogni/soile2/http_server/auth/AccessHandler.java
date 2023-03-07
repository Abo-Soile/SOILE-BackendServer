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

public class AccessHandler{
	
	MongoAuthorization mongoAuth;
	SoileIDBasedAuthorizationHandler idAccessHandler;
	SoileRoleBasedAuthorizationHandler roleHandler;
	private static final Logger LOGGER = LogManager.getLogger(AccessHandler.class);

	public AccessHandler(MongoAuthorization mongoAuth, SoileIDBasedAuthorizationHandler instanceIDAccessHandler,
			SoileRoleBasedAuthorizationHandler roleHandler) {
		super();
		this.mongoAuth = mongoAuth;
		this.idAccessHandler = instanceIDAccessHandler;
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
