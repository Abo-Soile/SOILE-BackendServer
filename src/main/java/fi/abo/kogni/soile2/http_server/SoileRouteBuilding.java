package fi.abo.kogni.soile2.http_server;


import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.abo.kogni.soile2.datamanagement.datalake.DataLakeResourceManager;
import fi.abo.kogni.soile2.elang.verticle.ExperimentLanguageVerticle;
import fi.abo.kogni.soile2.http_server.auth.JWTTokenCreator;
import fi.abo.kogni.soile2.http_server.auth.SoileAuthentication;
import fi.abo.kogni.soile2.http_server.auth.SoileAuthenticationBuilder;
import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization;
import fi.abo.kogni.soile2.http_server.auth.SoileCookieCreationHandler;
import fi.abo.kogni.soile2.http_server.auth.SoileFormLoginHandler;
import fi.abo.kogni.soile2.http_server.auth.SoileSessionHandler;
import fi.abo.kogni.soile2.http_server.requestHandling.IDSpecificFileProvider;
import fi.abo.kogni.soile2.http_server.routes.ElementRouter;
import fi.abo.kogni.soile2.http_server.routes.ParticipationRouter;
import fi.abo.kogni.soile2.http_server.routes.ProjectRouter;
import fi.abo.kogni.soile2.http_server.routes.SoileRouter;
import fi.abo.kogni.soile2.http_server.routes.StudyRouter;
import fi.abo.kogni.soile2.http_server.routes.TaskRouter;
import fi.abo.kogni.soile2.http_server.routes.UserRouter;
import fi.abo.kogni.soile2.http_server.verticles.CodeRetrieverVerticle;
import fi.abo.kogni.soile2.http_server.verticles.DataBundleGeneratorVerticle;
import fi.abo.kogni.soile2.http_server.verticles.ParticipantVerticle;
import fi.abo.kogni.soile2.http_server.verticles.TaskInformationverticle;
import fi.abo.kogni.soile2.projecthandling.apielements.APIExperiment;
import fi.abo.kogni.soile2.projecthandling.apielements.APIProject;
import fi.abo.kogni.soile2.projecthandling.participant.ParticipantHandler;
import fi.abo.kogni.soile2.projecthandling.projectElements.impl.ElementManager;
import fi.abo.kogni.soile2.projecthandling.projectElements.impl.Experiment;
import fi.abo.kogni.soile2.projecthandling.projectElements.impl.Project;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl.StudyHandler;
import fi.abo.kogni.soile2.qmarkup.verticle.QuestionnaireRenderVerticle;
import fi.abo.kogni.soile2.utils.DebugRouter;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.AuthenticationHandler;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.ChainAuthHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.JWTAuthHandler;
import io.vertx.ext.web.handler.LoggerHandler;
import io.vertx.ext.web.openapi.RouterBuilder;
import io.vertx.ext.web.sstore.LocalSessionStore;
/**
 * Verticle that handles Route Building for the Soile Backend Platform 
 * @author Thomas Pfau
 *
 */
public class SoileRouteBuilding extends AbstractVerticle{

	private static final Logger LOGGER = LogManager.getLogger(SoileRouteBuilding.class);

	private MongoClient client;
	private SoileCookieCreationHandler cookieHandler;
	private Router soileRouter;
	private SoileAuthenticationBuilder handler;
	private SoileAuthorization soileAuthorization;
	private DataLakeResourceManager resourceManager;
	private ParticipantHandler partHandler;
	private StudyHandler projHandler;
	private TaskRouter taskRouter;
	private ParticipationRouter partRouter;
	private IDSpecificFileProvider fileProvider;
	DeploymentOptions soileOpts;	
	AuthenticationHandler anyAuth;
	AuthenticationHandler userAuth;
	@SuppressWarnings("rawtypes")
	private List<MessageConsumer> consumers;

	
	@Override
	public void start(Promise<Void> startPromise) throws Exception {
		consumers = new LinkedList<>();
		cookieHandler = new SoileCookieCreationHandler(vertx.eventBus());	
		this.client = MongoClient.createShared(vertx, SoileConfigLoader.getMongoCfg(), "ROUTER_MONGO");
		resourceManager = new DataLakeResourceManager(vertx);
		soileAuthorization = new SoileAuthorization(client);
		projHandler = new StudyHandler(client, vertx);
		partHandler = new ParticipantHandler(client, projHandler, vertx);
		fileProvider = new IDSpecificFileProvider(resourceManager);
		LOGGER.debug("Starting Routerbuilder");
		deployVerticles()
		.compose(this::createRouter)		
		.compose(this::setupAuth)
		.compose(this::setupLogin)
		.compose(this::addHandlers)
		.compose(this::setupTaskAPI)
		.compose(this::setupExperimentAPI)
		.compose(this::setupProjectAPI)
		.compose(this::setupStudyAPI)
		.compose(this::setupParticipationAPI)
		.compose(this::setupUserAPI)					 
		.onSuccess( routerBuilder ->
		{
			// add Debug, Logger and Session Handlers.						
			soileRouter = routerBuilder.createRouter();						
			//as unfortunate as this is, we need to build a few routes manually, as they cannot be matched properly at the moment
			setUpSpecialRoutes(soileRouter);			
			// now, add the cleanup callBack for the different Routers, which will cache data.
			consumers.add(vertx.eventBus().consumer("soile.tempData.Cleanup", this::cleanUP));
			startPromise.complete();
		})
		.onFailure(fail ->
		{
			LOGGER.error("Failed Starting router with error:", fail);			
			startPromise.fail(fail);
		});


	}
	
	@Override
	@SuppressWarnings("rawtypes")
	public void stop(Promise<Void> stopPromise)
	{
		soileRouter.clear();
		List<Future> undeploymentFutures = new LinkedList<Future>();
		for(MessageConsumer consumer : consumers)
		{
			undeploymentFutures.add(consumer.unregister());
		}			
		
		CompositeFuture.all(undeploymentFutures).mapEmpty().
		onSuccess(v -> stopPromise.complete())
		.onFailure(err -> {
			LOGGER.error("Couldn't undeploy all child verticles");
			stopPromise.complete();
		});		
	}
	
	/**
	 * Deploy the required verticles.
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	private Future<Void> deployVerticles()
	{
		List<Future> deploymentFutures = new LinkedList<Future>();
		vertx.deployVerticle(new ExperimentLanguageVerticle(SoileConfigLoader.getVerticleProperty("elangAddress")), soileOpts);
		vertx.deployVerticle(new QuestionnaireRenderVerticle(SoileConfigLoader.getVerticleProperty("questionnaireAddress")), soileOpts);
		vertx.deployVerticle(new CodeRetrieverVerticle(), soileOpts);
		vertx.deployVerticle(new ParticipantVerticle(partHandler,projHandler), soileOpts);
		vertx.deployVerticle(new TaskInformationverticle(), soileOpts);
		vertx.deployVerticle(new DataBundleGeneratorVerticle(client,projHandler,partHandler), soileOpts);
		return CompositeFuture.all(deploymentFutures).mapEmpty();

	}	
	
	/**
	 * Set the Deployment options for the verticles required for routing.
	 * @param opts
	 */
	public void setDeploymentOptions(DeploymentOptions opts)
	{
		this.soileOpts = opts;
	}
	
	/**
	 * Create the router from the API file defined in the config.
	 * @param unused an unused Void input for composition.
	 * @return A Future of the {@link RouterBuilder} created
	 */
	private Future<RouterBuilder> createRouter(Void unused)
	{
		LOGGER.debug(config().getString("api"));
		return RouterBuilder.create(vertx, config().getString("api"));
	}

	
	public Router getRouter()
	{
		return this.soileRouter;
	}
	
	/**
	 * Clean up old cached data.
	 * @param cleanupRequest
	 */
	public void cleanUP(Message<Object> cleanupRequest)
	{
		partHandler.cleanup();
		projHandler.cleanup();
		taskRouter.cleanup();
	}
	/**
	 * Set up auth handling
	 * @param builder the Routerbuilder to be used.
	 * @return the routerbuilder in a future for composite use
	 */
	Future<RouterBuilder> setupAuth(RouterBuilder builder)
	{	
		handler = new SoileAuthenticationBuilder();
		
		AuthenticationHandler JWTAuth =  JWTAuthHandler.create(handler.getJWTAuthProvider(vertx));
		AuthenticationHandler tokenAuth =  handler.getTokenAuthProvider(partHandler, client);
		AuthenticationHandler cookieAuth =  handler.getCookieAuthProvider(vertx, client, cookieHandler);
		
		builder.securityHandler("cookieAuth",cookieAuth)
			   .securityHandler("JWTAuth", JWTAuth)
			   .securityHandler("tokenAuth", tokenAuth);
		
		anyAuth = ChainAuthHandler.any().add(JWTAuth).add(cookieAuth).add(tokenAuth);
		userAuth = ChainAuthHandler.any().add(JWTAuth).add(cookieAuth);
		return Future.<RouterBuilder>succeededFuture(builder);
	}
	
	Future<RouterBuilder> addHandlers(RouterBuilder builder)
	{
		builder.rootHandler(LoggerHandler.create());
		builder.rootHandler(new SoileSessionHandler(LocalSessionStore.create(vertx)));
		//TODO: Make flexible and set up for all front-end components
		CorsHandler cors = CorsHandler.create()											
				.allowedMethod(HttpMethod.POST)
				.allowedMethod(HttpMethod.GET)
				.allowedMethod(HttpMethod.OPTIONS)
				.allowCredentials(true)
			    .allowedHeader("Access-Control-Allow-Headers")
			    .allowedHeader("Authorization")
			    .allowedHeader("Access-Control-Allow-Method")
			    .allowedHeader("Access-Control-Allow-Origin")
			    .allowedHeader("Access-Control-Allow-Credentials")
			    .allowedHeader("Content-Type");
		JsonArray corsHosts = SoileConfigLoader.getConfig(SoileConfigLoader.HTTP_SERVER_CFG).getJsonArray("corsURLS", new JsonArray());
		for(int i = 0; i < corsHosts.size(); ++i)
		{
			cors.addOrigin(corsHosts.getString(i));
		}
		builder.rootHandler(cors);												
		builder.rootHandler(BodyHandler.create());
		builder.rootHandler(new DebugRouter());		
		return Future.<RouterBuilder>succeededFuture(builder);
	}
	
	Future<RouterBuilder> setupLogin(RouterBuilder builder)
	{
		
		SoileFormLoginHandler formLoginHandler = new SoileFormLoginHandler(new SoileAuthentication(client), "username", "password",new JWTTokenCreator(handler,vertx), cookieHandler);		
		builder.operation("loginUser").handler(formLoginHandler::handle);
		builder.operation("testAuth").handler(this::testAuth);
		builder.operation("logout").handler(this::logout);
		return Future.<RouterBuilder>succeededFuture(builder);
	}
	
	
	private void logout(RoutingContext ctx)
	{
		User currentUser = ctx.user();
		if(currentUser != null)
		{
			ctx.session().destroy();
			cookieHandler.invalidateSessionCookie(ctx)
			.onSuccess(loggedOut -> {
				ctx.response().setStatusCode(200)
				.end();				
			})
			.onFailure(err -> SoileRouter.handleError(err, ctx));
		}						
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
			.end(new JsonObject().put("authenticated", true)
								 .put("user", ctx.user().principal().getString("username"))
								 .put("roles", ctx.user().principal().getValue(SoileConfigLoader.getSessionProperty("userRoles")))
								 .encodePrettily());
		}
		else
		{
			ctx.request().response()
			.putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
			.end(new JsonObject().put("authenticated", false).put("user", null).encodePrettily());
		}
	}	
	
	/**
	 * Everything that needs to be done for the Task API
	 * @param builder
	 * @return
	 */
	private Future<RouterBuilder> setupTaskAPI(RouterBuilder builder)
	{
		taskRouter = new TaskRouter( client, fileProvider, vertx, soileAuthorization);
		builder.operation("getTaskList").handler(taskRouter::getElementList);
		builder.operation("getVersionsForTask").handler(taskRouter::getVersionList);
		builder.operation("createTask").handler(taskRouter::create);
		builder.operation("getTask").handler(taskRouter::getElement);
		builder.operation("getTaskFileList").handler(taskRouter::getTaskFileList);
		builder.operation("updateTask").handler(taskRouter::writeElement);
		builder.operation("compileCode").handler(taskRouter::compileCode);
		builder.operation("getExecution").handler(taskRouter::getCompiledTask);
		builder.operation("getTagForTaskVersion").handler(taskRouter::getTagForVersion);
		return Future.<RouterBuilder>succeededFuture(builder);
	}
	
	/**
	 * Everything that needs to be done for the Experiment API
	 * @param builder
	 * @return
	 */
	private Future<RouterBuilder> setupExperimentAPI(RouterBuilder builder)
	{
		ElementRouter<Experiment> router = new ElementRouter<Experiment>(new ElementManager<Experiment>(Experiment::new, APIExperiment::new, client, vertx), soileAuthorization, vertx.eventBus(), client );
		builder.operation("getExperimentList").handler(router::getElementList);
		builder.operation("getVersionsForExperiment").handler(router::getVersionList);
		builder.operation("createExperiment").handler(router::create);
		builder.operation("getExperiment").handler(router::getElement);
		builder.operation("updateExperiment").handler(router::writeElement);
		builder.operation("getTagForExperimentVersion").handler(router::getTagForVersion);
		return Future.<RouterBuilder>succeededFuture(builder);
	}

	/**
	 * Everything that needs to be done for the Project API
	 * @param builder
	 * @return
	 */
	private Future<RouterBuilder> setupProjectAPI(RouterBuilder builder)
	{
		ProjectRouter router = new ProjectRouter(new ElementManager<Project>(Project::new, APIProject::new, client, vertx), soileAuthorization, vertx.eventBus(), client );
		builder.operation("getProjectList").handler(router::getElementList);
		builder.operation("getVersionsForProject").handler(router::getVersionList);
		builder.operation("createProject").handler(router::create);
		builder.operation("getProject").handler(router::getElement);
		builder.operation("updateProject").handler(router::writeElement);
		builder.operation("getTagForProjectVersion").handler(router::getTagForVersion);
		builder.operation("isFilterValid").handler(router::isFilterValid);
		return Future.<RouterBuilder>succeededFuture(builder);
	}
		
	/**
	 * Everything that needs to be done for the project execution API
	 * The Project execution API will NOT use shortcuts!
	 * @param builder
	 * @return
	 */
	private Future<RouterBuilder> setupStudyAPI(RouterBuilder builder)
	{
		StudyRouter router = new StudyRouter(soileAuthorization, vertx, client, partHandler, projHandler);
		builder.operation("listDownloadData").handler(router::listDownloadData);
		builder.operation("startProject").handler(router::startProject);
		builder.operation("getRunningProjectList").handler(router::getRunningProjectList);
		builder.operation("stopProject").handler(router::stopProject);		
		builder.operation("restartProject").handler(router::restartProject);
		builder.operation("deleteProject").handler(router::deleteProject);
		builder.operation("getProjectResults").handler(router::getProjectResults);		
		builder.operation("downloadResults").handler(router::downloadResults);
		builder.operation("downloadTest").handler(router::downloadTest);
		builder.operation("createTokens").handler(router::createTokens);
		builder.operation("resetStudy").handler(router::resetStudy);
		builder.operation("updateStudy").handler(router::updateStudy);
		builder.operation("getStudyProperties").handler(router::getStudyProperties);
		builder.operation("getTokenInformation").handler(router::getTokenInformation);
		builder.operation("getCollaboratorsForStudy").handler(router::getCollaboratorsForStudy);		
		builder.operation("getStudyList").handler(router::getStudyList);		
		return Future.<RouterBuilder>succeededFuture(builder);
	}
	
	/**
	 * Everything that needs to be done for the project execution API
	 * @param builder
	 * @return
	 */
	private Future<RouterBuilder> setupParticipationAPI(RouterBuilder builder)
	{	
		partRouter = new ParticipationRouter(soileAuthorization, vertx, client, partHandler, projHandler, fileProvider);
		builder.operation("submitResults").handler(context -> {partRouter.handleRequest(context,partRouter::submitResults);});
		builder.operation("getTaskInfo").handler(context -> {partRouter.handleRequest(context,partRouter::getTaskInfo);});
		builder.operation("runTask").handler(context -> {partRouter.handleRequest(context,partRouter::runTask);});
		builder.operation("getID").handler(context -> {partRouter.handleRequest(context,partRouter::getID);});
		builder.operation("signUpForProject").handler(context -> {partRouter.handleRequest(context,partRouter::signUpForProject);});
		builder.operation("uploadData").handler(context -> {partRouter.handleRequest(context,partRouter::uploadData);});
		builder.operation("getPersistentData").handler(context -> {partRouter.handleRequest(context,partRouter::getPersistentData);});		
		return Future.<RouterBuilder>succeededFuture(builder);
	}
	/**
	 * Everything that needs to be done for the User API
	 * @param builder
	 * @return
	 */
	private Future<RouterBuilder> setupUserAPI(RouterBuilder builder)
	{
		UserRouter router = new UserRouter(soileAuthorization, vertx, client);
		builder.operation("registerUser").handler(router::registerUser);
		builder.operation("listUsers").handler(router::listUsers);
		builder.operation("createUser").handler(router::createUser);
		builder.operation("removeUser").handler(router::removeUser);
		builder.operation("getUserInfo").handler(router::getUserInfo);
		builder.operation("setUserInfo").handler(router::setUserInfo);
		builder.operation("setPassword").handler(router::setPassword);
		builder.operation("setRole").handler(router::setRole);
		builder.operation("permissionChange").handler(router::permissionChange);
		builder.operation("permissionOrRoleRequest").handler(router::permissionOrRoleRequest);
		builder.operation("getUserActiveProjects").handler(router::getUserActiveProjects);
		return Future.<RouterBuilder>succeededFuture(builder);
	}
	/**
	 * These are a few special routes which unfortunately cannot be 
	 * mapped directly from OPenAPI, as they require that all sub-pathes be properly matched. 
	 * @param router
	 */
	private void setUpSpecialRoutes(Router router)
	{
		// Order is important here! the /lib/* needs 
		// to match before /* because /* matches any /*/lib route as well and libs need to be loaded from the lib dir.
		// This actually means we need to reject any resource upload that starts with /lib!
		router.route(HttpMethod.GET, "/run/:id/:taskID/lib/*").handler(anyAuth).handler(context -> {partRouter.handleRequest(context,partRouter::getLib);});
		router.route(HttpMethod.GET, "/run/:id/:taskID/*").handler(anyAuth).handler(context -> {partRouter.handleRequest(context,partRouter::getResourceForExecution);});
		router.route(HttpMethod.GET, "/task/:id/:version/resource/*").handler(userAuth).handler(taskRouter::getResource);		
		router.route(HttpMethod.POST, "/task/:id/:version/resource/*").handler(userAuth).handler(taskRouter::postResource);
		router.route(HttpMethod.GET, "/task/:id/:version/execute").handler(userAuth).handler(taskRouter::getCompiledTask);
		router.route(HttpMethod.GET, "/task/:id/:version/execute/lib/*").handler(userAuth).handler(taskRouter::getLib);
		router.route(HttpMethod.GET, "/task/:id/:version/execute/*").handler(userAuth).handler(taskRouter::getResourceForExecution);
		router.route(HttpMethod.POST, "/task/:id/:version/resource/*").handler(userAuth).handler(taskRouter::postResource);
	}
	
	
	
	
}
