package fi.abo.kogni.soile2.http_server.authentication;

import java.util.function.BiConsumer;

import fi.abo.kogni.soile2.utils.DataRetriever;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import fi.abo.kogni.soile2.utils.TimeStampedPropertyMap;
import io.vertx.ext.auth.authorization.Authorization;
import io.vertx.ext.auth.authorization.AuthorizationContext;
import io.vertx.ext.auth.authorization.AuthorizationProvider;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.AuthorizationHandler;

public class SoileExperimentAuthorizationHandler implements AuthorizationHandler {

	
	final AuthorizationHandler authHandler;
	final MongoClient client;
	TimeStampedPropertyMap expDataMap;
	public SoileExperimentAuthorizationHandler(Authorization auth, MongoClient client)
	{
		authHandler = AuthorizationHandler.create(auth);
		this.client = client;		
		// Expire after an hour
		expDataMap = new TimeStampedPropertyMap(new DataRetriever(client, SoileConfigLoader.getDbCfg().getString("experimentCollection"), "_id"),3600000L); 
	}
	
	@Override
	public void handle(RoutingContext ctx) {
		// We will first get the configuration of the experiment from the RoutingContext, 
		// and then pass over to the auth-handler.
		String ExperimentID = ctx.pathParams().get("id");
		expDataMap.getProperties(ExperimentID, res -> {
			if(res.succeeded())
			{
				ctx.put("ExpData", res.result());
			}
			// set up the auth handler
			authHandler.handle(ctx);	
		});
		//TODO: Obtain the Privacy of the experiment and add the information to the context. 
		//vertx.eventBus().request(SoileCommUtils.getEventBusCommand(SoileConfigLoader.EXPERIMENT_CFG, SoileConfigLoader.get), ExperimentID)						
		

	}

	@Override
	public AuthorizationHandler addAuthorizationProvider(AuthorizationProvider authorizationProvider) {
		authHandler.addAuthorizationProvider(authorizationProvider);
		return this;
	}

	@Override
	public AuthorizationHandler variableConsumer(BiConsumer<RoutingContext, AuthorizationContext> handler) {
		authHandler.variableConsumer(handler);
		return this;
	}

}
