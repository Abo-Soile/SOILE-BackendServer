package fi.abo.kogni.soile2.http_server.authentication;

import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authorization.AuthorizationProvider;
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
		
		JsonArray owned = user.principal().getJsonArray(SoileConfigLoader.getSessionProperty("userOwnes"));
		JsonArray collaborates = user.principal().getJsonArray(SoileConfigLoader.getSessionProperty("userCollaborates"));
		JsonArray participates = user.principal().getJsonArray(SoileConfigLoader.getSessionProperty("userParticipates"));
		JsonArray roles = user.principal().getJsonArray(SoileConfigLoader.getSessionProperty("userRoles"));
		for(Object Exp : owned)
		{			
			user.authorizations().add(SoileAuth, RoleBasedAuthorization.create(PermissionIDStrategy.getPermissionID(Exp.toString(), PermissionIDStrategy.Type.Owner)));
		}
		for(Object Exp : collaborates)
		{			
			user.authorizations().add(SoileAuth, RoleBasedAuthorization.create(PermissionIDStrategy.getPermissionID(Exp.toString(), PermissionIDStrategy.Type.Collaborator)));
		}
		for(Object Exp : participates)
		{			
			user.authorizations().add(SoileAuth, RoleBasedAuthorization.create(PermissionIDStrategy.getPermissionID(Exp.toString(), PermissionIDStrategy.Type.Participant)));
		}
		for(Object role : roles)
		{
			user.authorizations().add(SoileAuth, RoleBasedAuthorization.create(role.toString()));			
		}
		
	}
		
		
	@Override
	public String getId() {
		return SoileAuth;
	}

}
