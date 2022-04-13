package fi.abo.kogni.soile2.http_server.authentication;

import java.util.function.BiConsumer;

import io.vertx.core.Vertx;
import io.vertx.ext.auth.authorization.Authorization;
import io.vertx.ext.auth.authorization.AuthorizationContext;
import io.vertx.ext.auth.authorization.AuthorizationProvider;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.AuthorizationHandler;

public class SoileExperimentAuthorizationHandler implements AuthorizationHandler {

	
	final AuthorizationHandler authHandler;
	final MongoClient client;
	final Vertx vertx;
	
	public SoileExperimentAuthorizationHandler(Authorization auth, MongoClient client, Vertx vertx)
	{
		authHandler = AuthorizationHandler.create(auth);
		this.client = client;
		this.vertx = vertx;
	}
	
	@Override
	public void handle(RoutingContext ctx) {
		// We will first get the configuration of the experiment from the RoutingContext, 
		// and then pass over to the auth-handler.
		String ExperimentID = ctx.pathParams().get("id");
		vertx.eventBus().request(ExperimentID, ExperimentID)
		
		
		
		authHandler.handle(ctx);

	}

	@Override
	public AuthorizationHandler addAuthorizationProvider(AuthorizationProvider authorizationProvider) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AuthorizationHandler variableConsumer(BiConsumer<RoutingContext, AuthorizationContext> handler) {
		// TODO Auto-generated method stub
		return null;
	}

}
