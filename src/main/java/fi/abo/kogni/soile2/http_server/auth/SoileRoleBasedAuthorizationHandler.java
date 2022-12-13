
package fi.abo.kogni.soile2.http_server.auth;

import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.Roles;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authorization.Authorization;
import io.vertx.ext.auth.authorization.OrAuthorization;
import io.vertx.ext.auth.authorization.RoleBasedAuthorization;
import io.vertx.ext.web.handler.HttpException;


public class SoileRoleBasedAuthorizationHandler{

	public SoileRoleBasedAuthorizationHandler()
	{
	}

	/**
	 * This requires the authorizations to be loaded for the USER PRE testing!
	 * @param user The user with the authorizations loaded.
	 * @param requiredRole the Role required for the user to proceed
	 * @return
	 */
	public Future<Void> authorize(User user, Roles requiredRole) {
		Promise<Void> authorizationPromise = Promise.<Void>promise(); 
		if(requiredRole == null)
		{
			authorizationPromise.complete();
			return authorizationPromise.future();
		}
		if(buildRoleAuthorization(requiredRole).match(user))
			{
				authorizationPromise.complete();
			}
			else
			{
				authorizationPromise.fail(new HttpException(403));
			}
		return authorizationPromise.future();
	}



	private Authorization buildRoleAuthorization(Roles requiredRole)
	{

		if(requiredRole == Roles.Admin)
		{
			return RoleBasedAuthorization.create(Roles.Admin.toString());
		}
		else
		{
			if(requiredRole == Roles.Researcher)
			{
				return OrAuthorization.create().addAuthorization(RoleBasedAuthorization.create(Roles.Researcher.toString()))
											   .addAuthorization(RoleBasedAuthorization.create(Roles.Admin.toString()));				
			}
			else
			{
				return OrAuthorization.create().addAuthorization(RoleBasedAuthorization.create(Roles.Participant.toString()))
											   .addAuthorization(RoleBasedAuthorization.create(Roles.Researcher.toString()))
											   .addAuthorization(RoleBasedAuthorization.create(Roles.Admin.toString()));
			}
		}
	}		

  

}
