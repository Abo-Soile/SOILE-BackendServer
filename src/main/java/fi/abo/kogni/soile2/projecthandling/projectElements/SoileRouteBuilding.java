package fi.abo.kogni.soile2.projecthandling.projectElements;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.abo.kogni.soile2.datamanagement.git.GitManager;
import fi.abo.kogni.soile2.datamanagement.git.ResourceManager;
import fi.abo.kogni.soile2.http_server.auth.JWTTokenCreator;
import fi.abo.kogni.soile2.http_server.auth.SoileAuthenticationBuilder;
import fi.abo.kogni.soile2.http_server.auth.SoileFormLoginHandler;
import fi.abo.kogni.soile2.http_server.authentication.SoileAuthentication;
import fi.abo.kogni.soile2.http_server.authentication.SoileCookieCreationHandler;
import fi.abo.kogni.soile2.http_server.routes.ElementRouter;
import fi.abo.kogni.soile2.http_server.routes.TaskRouter;
import fi.abo.kogni.soile2.utils.DebugRouter;
import fi.abo.kogni.soile2.utils.MessageResponseHandler;
import fi.abo.kogni.soile2.utils.SoileCommUtils;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.authorization.PermissionBasedAuthorization;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.JWTAuthHandler;
import io.vertx.ext.web.handler.LoggerHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.openapi.RouterBuilder;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.ext.web.validation.RequestParameters;
import io.vertx.ext.web.validation.ValidationHandler;

public class SoileRouteBuilding extends AbstractVerticle{

	private static final Logger LOGGER = LogManager.getLogger(SoileRouteBuilding.class);
	private MongoClient client;
	private SoileCookieCreationHandler cookieHandler;
	private Router soileRouter;
	private SoileAuthenticationBuilder handler;
	private GitManager gitManager;
	private ResourceManager resourceManager;
	@Override
	public void start(Promise<Void> startPromise) throws Exception {		
		cookieHandler = new SoileCookieCreationHandler(vertx.eventBus());	
		this.client = MongoClient.createShared(vertx, config().getJsonObject("db"));
		gitManager = new GitManager(vertx.eventBus());
		resourceManager = new ResourceManager(vertx.eventBus());
		LOGGER.debug("Starting Routerbuilder");
		RouterBuilder.create(vertx, config().getString("api"))
					 .compose(this::setupAuth)
					 .compose(this::setupLogin)
					 .compose(this::addHandlers)
					 .compose(this::setupTaskAPI)
					 .onSuccess( routerBuilder ->
					 {
						// add Debug, Logger and Session Handlers.						
						soileRouter = routerBuilder.createRouter();
						startPromise.complete();
					 })
					 .onFailure(fail ->
					 {
						 startPromise.fail(fail.getCause());
					 }
					 );
		
		
	}
	
	public Router getRouter()
	{
		return this.soileRouter;
	}
	/**
	 * Set up auth handling
	 * @param builder the Routerbuilder to be used.
	 * @return the routerbuilder in a future for composite use
	 */
	Future<RouterBuilder> setupAuth(RouterBuilder builder)
	{	
		handler = new SoileAuthenticationBuilder();
		builder.securityHandler("cookieAuth",handler.getCookieAuthProvider(vertx, client, cookieHandler))
			   .securityHandler("JWTAuth", JWTAuthHandler.create(handler.getJWTAuthProvider(vertx)));
		return Future.<RouterBuilder>succeededFuture(builder);
	}
	
	Future<RouterBuilder> addHandlers(RouterBuilder builder)
	{
		builder.rootHandler(LoggerHandler.create());
		builder.rootHandler(SessionHandler.create(LocalSessionStore.create(vertx)));
		builder.rootHandler(BodyHandler.create());
		builder.rootHandler(new DebugRouter());
		return Future.<RouterBuilder>succeededFuture(builder);
	}
	
	Future<RouterBuilder> setupLogin(RouterBuilder builder)
	{
		builder.operation("addUser").handler(handleUserManagerCommand("addUser", MessageResponseHandler.createDefaultHandler(201)));
		SoileFormLoginHandler formLoginHandler = new SoileFormLoginHandler(new SoileAuthentication(client), "username", "password",new JWTTokenCreator(handler,vertx), cookieHandler);
		builder.operation("loginUser").handler(formLoginHandler::handle);
		builder.operation("testAuth").handler(this::testAuth);
		return Future.<RouterBuilder>succeededFuture(builder);
	}
	
	
	Handler<RoutingContext> handleUserManagerCommand(String command, MessageResponseHandler messageHandler)
	{
		Handler<RoutingContext> handler = new Handler<RoutingContext>() {

			@Override
			public void handle(RoutingContext routingContext) {
				RequestParameters params = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);
				JsonObject body = params.body().getJsonObject();
				vertx.eventBus().request(SoileCommUtils.getEventBusCommand(SoileConfigLoader.USERMGR_CFG, command),body).onSuccess( response ->
				{
					if(response.body() instanceof JsonObject)
					{
						
						messageHandler.handle(((JsonObject)response.body()), routingContext);
						routingContext.response().end();
					}
				}).onFailure( failure -> {
					
					if(failure.getCause() instanceof ReplyException)
					{
						ReplyException err = (ReplyException)failure.getCause();
						routingContext.response()
							.setStatusCode(err.failureCode())
							.setStatusMessage(err.getMessage())
							.end();
					}
					else
					{
						routingContext.response()
						.setStatusCode(500)
						.end();
						LOGGER.error("Something went wrong when trying to register a new user");					
						LOGGER.error(failure.getCause());
					}
				});
				
			}
		};		
		return handler;			
	}
	
	public void testAuth(RoutingContext ctx)
	{
		LOGGER.debug("AuthTest got a request");
		if(ctx.user() != null)
		{
			LOGGER.debug(ctx.user());
			LOGGER.debug(ctx.user().principal().encodePrettily());
			LOGGER.debug(ctx.user().attributes().encodePrettily());
			LOGGER.debug(ctx.user().authorizations().toString());
			ctx.request().response()
			.putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
			.end(new JsonObject().put("authenticated", true).put("user", ctx.user().principal().getString("username")).encodePrettily());
		}
		else
		{
			ctx.request().response()
			.putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
			.end(new JsonObject().put("authenticated", false).put("user", null).encodePrettily());
		}
	}	
	
	
	public Future<RouterBuilder> setupTaskAPI(RouterBuilder builder)
	{
		TaskRouter router = new TaskRouter(gitManager, client, resourceManager);
		builder.operation("getTaskList").handler(router::getElementList);
		builder.operation("getVersionsForTask").handler(router::getVersionList);
		builder.operation("createTask").handler(router::create);
		builder.operation("getTask").handler(router::getElement);
		builder.operation("updateTask").handler(router::writeElement);
		builder.operation("getResource").handler(router::getResource);
		builder.operation("putResource").handler(router::postResource);
		return Future.<RouterBuilder>succeededFuture(builder);
	}
}
