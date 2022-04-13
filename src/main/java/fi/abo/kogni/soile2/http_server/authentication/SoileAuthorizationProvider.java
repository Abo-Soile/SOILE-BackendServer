package fi.abo.kogni.soile2.http_server.authentication;

import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authorization.AuthorizationProvider;
import io.vertx.ext.auth.authorization.PermissionBasedAuthorization;
import io.vertx.ext.auth.authorization.RoleBasedAuthorization;
import io.vertx.ext.mongo.MongoClient;

/**
 * This class represents an authorization to experiment data based on types of users. e.g. participants will be matched on whether they 
 * have access permissions to the experiment, while for researchers access will be checked on whether they are part of a group for a restricted experiment,
 * or whether they are researchers for unrestricted experiments. 
 */
public class SoileAuthorizationProvider implements AuthorizationProvider{

	
	public static final String SoileAuth = "SoileAuth";		
	private final MongoClient client;
	public SoileAuthorizationProvider(MongoClient client)	
	{		
		this.client = client;
	}
	

	
	@Override
	public void getAuthorizations(User user, Handler<AsyncResult<Void>> handler) {
		
		JsonObject query = new JsonObject().put(SoileConfigLoader.getdbField("usernameField"), user.principal().getString("username"));
		client.find(SoileConfigLoader.getdbProperty("userCollection"), query, res -> {
			if (res.failed()) {
				handler.handle(Future.failedFuture(res.cause()));
				return;
			}

			for (JsonObject jsonObject : res.result()) {
				JsonArray roles = jsonObject.getJsonArray(SoileConfigLoader.getdbField("userRolesField"));
				if (roles!=null) {
					for (int i=0; i<roles.size(); i++) {
						String role = roles.getString(i);
						user.authorizations().add(SoileAuth, RoleBasedAuthorization.create(role));
					}
				}
				JsonArray permissions = jsonObject.getJsonArray(SoileConfigLoader.getdbField("userPermissionsField"));
				if (permissions!=null) {
					for (int i=0; i<permissions.size(); i++) {
						String permission = permissions.getString(i);
						user.authorizations().add(SoileAuth, PermissionBasedAuthorization.create(permission));
					}
				}
			}
			handler.handle(Future.succeededFuture());
		});
	}
		
		
	@Override
	public String getId() {
		return SoileAuth;
	}

}
