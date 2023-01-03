package fi.abo.kogni.soile2.http_server.auth;

import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.PermissionType;
import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.Roles;
import fi.abo.kogni.soile2.projecthandling.participant.ParticipantHandler;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authorization.RoleBasedAuthorization;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.validation.RequestParameters;
import io.vertx.ext.web.validation.ValidationHandler;

public class TokenAuthProvider {
	
	ParticipantHandler partHandler;
	
	public TokenAuthProvider(ParticipantHandler partHandler)
	{
		this.partHandler = partHandler;
	}
	
	public Future<User> authenticate(RoutingContext context )
	{
		RequestParameters params = context.get(ValidationHandler.REQUEST_CONTEXT_KEY);		
		String requestedInstanceID = params.pathParameter("id").getString();
		Promise<User> userPromise = Promise.promise();
		String token = params.headerParameter("Authorization").getString();
		partHandler.getParticipantForToken(token, requestedInstanceID)
		.onSuccess(participant -> {
			User currentUser = User.fromToken(token);
			// we add 
			currentUser.authorizations().add("TokenProvider", RoleBasedAuthorization.create(Roles.Participant.toString()));
			currentUser.authorizations().add("TokenProvider", SoilePermissionProvider.buildPermission(requestedInstanceID, PermissionType.EXECUTE));
			userPromise.complete(currentUser);
		})
		.onFailure(err -> userPromise.fail(err));
		return userPromise.future();
		
	}
}
