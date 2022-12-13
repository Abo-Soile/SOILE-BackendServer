package fi.abo.kogni.soile2.http_server.auth;

import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.PermissionType;
import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.Roles;
import fi.abo.kogni.soile2.http_server.authentication.utils.AccessElement;
import fi.abo.kogni.soile2.projecthandling.exceptions.ObjectDoesNotExist;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authorization.Authorization;
import io.vertx.ext.auth.authorization.OrAuthorization;
import io.vertx.ext.auth.authorization.PermissionBasedAuthorization;
import io.vertx.ext.auth.authorization.RoleBasedAuthorization;
import io.vertx.ext.auth.authorization.WildcardPermissionBasedAuthorization;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.handler.HttpException;


public class SoileIDBasedAuthorizationHandler<T extends AccessElement>{
			
	static final Logger LOGGER = LogManager.getLogger(SoileIDBasedAuthorizationHandler.class);
	MongoClient client;
	AccessElement baseElement;
	public SoileIDBasedAuthorizationHandler(Supplier<T> supplier, MongoClient client )
	{
		baseElement = supplier.get();
		this.client = client;
	}
		
	public Future<Void> authorize(User user, String id, boolean adminAllowed, PermissionType requiredAccess) {	
		Promise<Void> authorizationPromise = Promise.<Void>promise();
		if(requiredAccess == null)
		{
			authorizationPromise.complete();
			return authorizationPromise.future();
		}
		// TODO Auto-generated method stub	
			client.findOne(baseElement.getTargetCollection(),new JsonObject().put("_id", id),new JsonObject().put("private", 1))
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
			return PermissionBasedAuthorization.create(requiredAccess.toString() + "$" + id);
		}
		else
		{
			if(requiredAccess == PermissionType.READ_WRITE)
			{
				return OrAuthorization.create().addAuthorization(PermissionBasedAuthorization.create(PermissionType.FULL.toString() + "$" + id))
											   .addAuthorization(PermissionBasedAuthorization.create(PermissionType.READ_WRITE.toString() + "$" + id));	
			}
			else
			{
				return WildcardPermissionBasedAuthorization.create("*$" + id);
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
