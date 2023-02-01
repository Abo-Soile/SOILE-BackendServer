package fi.abo.kogni.soile2.http_server.auth;

import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.PermissionType;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.ext.auth.User;
import io.vertx.ext.mongo.MongoClient;

public class SoileProjectInstanceAuthHandler extends SoileIDBasedAuthorizationHandler {

	public SoileProjectInstanceAuthHandler(String targetCollection, MongoClient client) {
		super(targetCollection, client);
		// TODO Auto-generated constructor stub
	}

	
	public Future<Void> authorize(User user, String id, boolean adminAllowed, PermissionType requiredAccess, boolean requiresParticipant) { 
		
		Promise<Void> authPromise = Promise.promise();
		super.authorize(user, id, adminAllowed, requiredAccess);
		return null;
	}
	
}
