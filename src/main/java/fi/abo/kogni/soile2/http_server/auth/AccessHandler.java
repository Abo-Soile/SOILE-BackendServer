package fi.abo.kogni.soile2.http_server.auth;

import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.PermissionType;
import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.Roles;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.mongo.MongoAuthorization;
import io.vertx.ext.web.handler.HttpException;

public class AccessHandler{
	
	MongoAuthorization mongoAuth;
	SoileIDBasedAuthorizationHandler instanceIDAccessHandler;
	SoileRoleBasedAuthorizationHandler roleHandler;

	public AccessHandler(MongoAuthorization mongoAuth, SoileIDBasedAuthorizationHandler instanceIDAccessHandler,
			SoileRoleBasedAuthorizationHandler roleHandler) {
		super();
		this.mongoAuth = mongoAuth;
		this.instanceIDAccessHandler = instanceIDAccessHandler;
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
		// TODO Auto-generated method stub
		Promise<Void> accessPromise = Promise.<Void>promise();
		mongoAuth.getAuthorizations(user)
		.onSuccess(Void -> {
			instanceIDAccessHandler.authorize(user, id, adminAllowed, requiredPermission)
			.onSuccess(acceptID -> {
				roleHandler.authorize(user, requiredRole)
				.onSuccess(acceptRole -> {
					// both role and permission checks are successfull.
					accessPromise.complete();
				})
				.onFailure(err -> accessPromise.fail(err));
			})
			.onFailure(err -> accessPromise.fail(err));
		})
		.onFailure(err -> {
			accessPromise.fail(new HttpException(500,err.getMessage()));
		});
		return accessPromise.future();
	}

}
