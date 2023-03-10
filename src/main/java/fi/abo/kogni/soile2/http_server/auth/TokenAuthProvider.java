package fi.abo.kogni.soile2.http_server.auth;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.PermissionType;
import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.Roles;
import fi.abo.kogni.soile2.projecthandling.exceptions.ObjectDoesNotExist;
import fi.abo.kogni.soile2.projecthandling.participant.ParticipantHandler;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authorization.RoleBasedAuthorization;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.RoutingContext;
/**
 * Class that provides Authentication for token carrying requests. 
 * @author Thomas Pfau
 *
 */
public class TokenAuthProvider {
	static final Logger LOGGER = LogManager.getLogger(TokenAuthProvider.class);

	ParticipantHandler partHandler;
	MongoClient client;
	public TokenAuthProvider(ParticipantHandler partHandler, MongoClient client)
	{
		this.partHandler = partHandler;
		this.client = client;
	}
	
	/**
	 * Authenticate the given token. The token must fit the instanceID requested in the RoutingContext.
	 * If both match (i.e. the token is associated with a valid participant for the projectInstance, then the 
	 * Provider creates a user which does NOT have a username but only a token.
	 * TODO: Need to ensure that this is compatible with all relevant endpoints (i.e. the endpoints for project execution)  
	 * @param context
	 * @return A Future of the user to be added to the context.
	 */
	public Future<User> authenticate(RoutingContext context )
	{			
		String pathID = context.pathParam("id");
		String token = context.request().getHeader("Authorization");
		LOGGER.debug("Trying to retrieve TokenParticipant for Token: " + token);
		Promise<User> userPromise = Promise.promise();
		getID(pathID)
		.onSuccess(requestedInstanceID -> {
			LOGGER.debug("Trying to retrieve participant for project: " + requestedInstanceID);
			partHandler.getParticipantForToken(token, requestedInstanceID)
			.onSuccess(participant -> {
				LOGGER.debug("Got participant");
				User currentUser = User.fromToken(token);
				// we add 
				currentUser.authorizations().add("TokenProvider", RoleBasedAuthorization.create(Roles.Participant.toString()));
				currentUser.authorizations().add("TokenProvider", SoilePermissionProvider.buildPermission(requestedInstanceID, PermissionType.EXECUTE));
				userPromise.complete(currentUser);
			})
			.onFailure(err -> userPromise.fail(err));
		})
		.onFailure(err -> userPromise.fail(err));
		
		return userPromise.future();
		
	}
	
	private Future<String> getID(String pathID)
	{
		Promise<String> idPromise = Promise.promise();		 				
		JsonObject Query = new JsonObject().put("$or", new JsonArray().add(new JsonObject().put("shortcut",pathID))
																	  .add(new JsonObject().put("_id", pathID))
																	  );		
		client.findOne(SoileConfigLoader.getCollectionName("projectInstanceCollection"),Query,new JsonObject().put("_id",1))
		.onSuccess(project -> 
		{
			if(project == null)
			{
				idPromise.fail(new ObjectDoesNotExist(pathID));
			}
			else
			{
				idPromise.complete(project.getString("_id"));
			}
		})
		.onFailure(err -> idPromise.fail(err));		
		return idPromise.future();
	}
}
