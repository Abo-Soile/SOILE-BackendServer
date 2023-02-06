package fi.abo.kogni.soile2.http_server.routes;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.abo.kogni.soile2.datamanagement.datalake.DataLakeManager;
import fi.abo.kogni.soile2.http_server.auth.AccessHandler;
import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization;
import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.PermissionType;
import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.Roles;
import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.TargetElementType;
import fi.abo.kogni.soile2.http_server.auth.SoileIDBasedAuthorizationHandler;
import fi.abo.kogni.soile2.http_server.auth.SoileRoleBasedAuthorizationHandler;
import fi.abo.kogni.soile2.http_server.requestHandling.NonStaticHandler;
import fi.abo.kogni.soile2.projecthandling.participant.Participant;
import fi.abo.kogni.soile2.projecthandling.participant.ParticipantHandler;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.AccessProjectInstance;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.ProjectInstance;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl.ProjectInstanceHandler;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl.TaskObjectInstance;
import fi.abo.kogni.soile2.utils.SoileCommUtils;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.FileSystemAccess;
import io.vertx.ext.web.handler.HttpException;
import io.vertx.ext.web.validation.RequestParameters;
import io.vertx.ext.web.validation.ValidationHandler;

public class ParticipationRouter extends SoileRouter{

	
	NonStaticHandler libraryHandler;
	ProjectInstanceHandler instanceHandler;
	AccessHandler accessHandler;
	SoileAuthorization authorizationRertiever;
	ParticipantHandler partHandler;
	TargetElementType instanceType = TargetElementType.INSTANCE;
	DataLakeManager dataLakeManager;
	EventBus eb;
	Vertx vertx;
	
	static final Logger LOGGER = LogManager.getLogger(ProjectInstanceRouter.class);


	public ParticipationRouter(SoileAuthorization auth, Vertx vertx, MongoClient client, ParticipantHandler partHandler, ProjectInstanceHandler projHandler) {
		eb = vertx.eventBus();
		this.vertx = vertx;
		
		authorizationRertiever = auth;
		instanceHandler = projHandler;
		this.partHandler = partHandler;		
		accessHandler = new AccessHandler(auth.getAuthorizationForOption(instanceType), new SoileIDBasedAuthorizationHandler(new AccessProjectInstance().getTargetCollection(), client), new SoileRoleBasedAuthorizationHandler());		 	
		dataLakeManager = new DataLakeManager(SoileConfigLoader.getServerProperty("soileResultDirectory"), vertx);
		libraryHandler = new NonStaticHandler(FileSystemAccess.RELATIVE, "data/libs/", "/lib/");
	}
	
	public void submitResults(RoutingContext context)
	{				
		RequestParameters params = context.get(ValidationHandler.REQUEST_CONTEXT_KEY);
		String requestedInstanceID = params.pathParameter("id").getString();
		accessHandler.checkAccess(context.user(),requestedInstanceID, Roles.Participant,PermissionType.READ,false)
		.onSuccess(Void -> 
		{
			instanceHandler.loadProject(requestedInstanceID)
			.onSuccess(project -> {					
				// this list needs to be filtered by access
				getParticpantForUser(context.user(), project)				
				.onSuccess(participant-> {
					project.finishStep(participant, context.body().asJsonObject())
					.onSuccess(res -> {
						context.response()
						.setStatusCode(200)						
						.end();
					})
					.onFailure(err -> handleError(err, context));
				})
				.onFailure(err -> handleError(err, context));
			})
			.onFailure(err -> handleError(err, context));
		})
		.onFailure(err -> handleError(err, context));			
	}
	
	public void uploadData(RoutingContext context)
	{
		RequestParameters params = context.get(ValidationHandler.REQUEST_CONTEXT_KEY);
		String requestedInstanceID = params.pathParameter("id").getString();
		
		accessHandler.checkAccess(context.user(),requestedInstanceID, Roles.Researcher,PermissionType.READ,true)
		.onSuccess(Void -> 
		{
			if(context.fileUploads().size() != 1)
			{
				handleError(new HttpException(400, "Only one Upload allowed at a time"), context);
				return;
			}
			FileUpload currentUpload = context.fileUploads().get(0);
			instanceHandler.loadProject(requestedInstanceID)
			.onSuccess(project -> {				
				//JsonArray taskData = project.getTasksWithNames();
				// this list needs to be filtered by access
				getParticpantForUser(context.user(), project)				
				.onSuccess(participant-> {
					participant.getCurrentStep()
					.onSuccess(step -> {
						dataLakeManager.storeParticipantData(participant.getID(), step, participant.getProjectPosition(), currentUpload)
						.onSuccess(id -> {
							context.response()
							.setStatusCode(200)						
							.putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
							.end(new JsonObject().put("id", id).encode());
							
						})
						.onFailure(err -> handleError(err, context));

					})
					.onFailure(err -> handleError(err, context));
				})
				.onFailure(err -> handleError(err, context));
			})
			.onFailure(err -> handleError(err, context));
		})
		.onFailure(err -> handleError(err, context));
	}
	
	public void signUpForProject(RoutingContext context)
	{
		RequestParameters params = context.get(ValidationHandler.REQUEST_CONTEXT_KEY);
		String requestedInstanceID = params.pathParameter("id").getString();
		String token = params.queryParameter("token").getString();
		instanceHandler.loadProject(requestedInstanceID)
		.onSuccess(project -> {				
			project.useToken(token)
			.onSuccess( tokenUsed -> {
				if(context.user() != null)
				{
					// we will add execute access to the current user.					
					
					JsonObject userData = new JsonObject();
					userData.put("username", context.user().principal().getString("username"));
					userData.put("command", "add");
					userData.put("permissions", new JsonArray().add(new JsonObject().put("target", requestedInstanceID).put("type", PermissionType.EXECUTE.toString())));							
					eb.request(SoileCommUtils.getEventBusCommand(SoileConfigLoader.USERMGR_CFG, "permissionOrRoleChange"),userData).onSuccess( response ->
					{
						// all done;
						context.response()
						.setStatusCode(200)						
						.end();
					})
					.onFailure(err -> handleError(err, context));
				}
				else
				{
					// we don't have a user, so we set up a oken user.
					partHandler.createTokenUser(project)
					.onSuccess(participant -> {
						context.response()
						.setStatusCode(200)						
						.putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
						.end(new JsonObject().put("token", participant.getToken()).encode());
					})
					.onFailure(err -> handleError(err, context));
				}
			})
			.onFailure(err -> handleError(err, context));
		})
		.onFailure(err -> handleError(err, context));
	}
	
	public void getTaskType(RoutingContext context)
	{
		RequestParameters params = context.get(ValidationHandler.REQUEST_CONTEXT_KEY);
		String requestedInstanceID = params.pathParameter("id").getString();
		accessHandler.checkAccess(context.user(),requestedInstanceID, Roles.Participant,PermissionType.READ,false)
		.onSuccess(Void -> {
			instanceHandler.loadProject(requestedInstanceID)
			.onSuccess(project -> {					
				//JsonArray taskData = project.getTasksWithNames();
				// this list needs to be filtered by access
				getParticpantForUser(context.user(), project)				
				.onSuccess(participant-> {		
					if(participant.isFinished())
					{
						context.response()
						.setStatusCode(200)	
						.putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
						.end(new JsonObject().put("finished", true).put("codeType", "").encode());
					}					
					else
					{
						try {
							TaskObjectInstance currentTask = (TaskObjectInstance)project.getElement(participant.getProjectPosition());
							eb.request(SoileConfigLoader.getVerticleProperty("getTaskInformationAddress"), new JsonObject().put("taskID", currentTask.getUUID()))
							.onSuccess(response -> {								
								JsonObject responseBody = ((JsonObject) response.body()).getJsonObject(SoileCommUtils.DATAFIELD);
								context.response()
								.setStatusCode(200)	
								.putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
								.end(new JsonObject().put("finished", false).put("codeType", responseBody.getString("codeType")).encode());
							})
							.onFailure(err -> handleError(err, context));
						}
						catch(ClassCastException e)
						{
							// This should not happen, but just in case...
							LOGGER.error("The current Task for user " + participant.getID() + "is NOT a task!!");
							context.response()
							.setStatusCode(500)					
							.end("Problem retrieving task");
						}						
					}
				})
				.onFailure(err -> handleError(err, context));
			})
			.onFailure(err -> handleError(err, context));
		})
		.onFailure(err -> handleError(err, context));		
	}
	
	
	public void runTask(RoutingContext context)
	{
		RequestParameters params = context.get(ValidationHandler.REQUEST_CONTEXT_KEY);
		String requestedInstanceID = params.pathParameter("id").getString();
		
		accessHandler.checkAccess(context.user(),requestedInstanceID, Roles.Participant,PermissionType.READ,false)
		.onSuccess(Void -> {
			instanceHandler.loadProject(requestedInstanceID)
			.onSuccess(project -> {					
				//JsonArray taskData = project.getTasksWithNames();
				// this list needs to be filtered by access
				getParticpantForUser(context.user(), project)				
				.onSuccess(participant-> {		
					if(participant.isFinished())
					{
						context.response()
						.setStatusCode(406)							
						.end("User is finished");
					}					
					else
					{
						// Try catch block.
						TaskObjectInstance currentTask = (TaskObjectInstance) project.getElement(participant.getProjectPosition());
						eb.request(SoileConfigLoader.getVerticleProperty("gitCompilationAddress"),
							   new JsonObject().put("taskID", currentTask.getUUID())
									   		   .put("type", currentTask.getCodeType())
									   		   .put("version", currentTask.getVersion()))
						.onSuccess(response -> {
							JsonObject responseBody = (JsonObject) response.body();
							context.response()
							.setStatusCode(200)
							.putHeader(HttpHeaders.CONTENT_TYPE, "application/javascript")
							.end(responseBody.getString("code"));
						})
						.onFailure(err -> handleError(err, context));
					}
				})
				.onFailure(err -> handleError(err, context));
			})
			.onFailure(err -> handleError(err, context));
		})
		.onFailure(err -> handleError(err, context));		
	}			
		
	public void getLib(RoutingContext context)
	{
		RequestParameters params = context.get(ValidationHandler.REQUEST_CONTEXT_KEY);
		String requestedInstanceID = params.pathParameter("id").getString();
		
		accessHandler.checkAccess(context.user(),requestedInstanceID, Roles.Participant,PermissionType.READ,false)
		.onSuccess(Void -> {
			instanceHandler.loadProject(requestedInstanceID)
			.onSuccess(project -> {					
				//JsonArray taskData = project.getTasksWithNames();
				// this list needs to be filtered by access
				getParticpantForUser(context.user(), project)				
				.onSuccess(participant-> {		
					if(participant.isFinished())
					{
						context.response()
						.setStatusCode(406)							
						.end("User is finished");
					}					
					else
					{
						// get the current Task
						TaskObjectInstance currentTask = (TaskObjectInstance) project.getElement(participant.getProjectPosition());
						// This is supposedly a request for a static library required for this specific element, so here you are.
						eb.reqcurrentTask.getUUID()
					}
				})
				.onFailure(err -> handleError(err, context));
			})
			.onFailure(err -> handleError(err, context));
		})
		.onFailure(err -> handleError(err, context));		
	}
	
	public void getResourceForExecution(RoutingContext context)
	{
		RequestParameters params = context.get(ValidationHandler.REQUEST_CONTEXT_KEY);
		String requestedInstanceID = params.pathParameter("id").getString();
		
		accessHandler.checkAccess(context.user(),requestedInstanceID, Roles.Participant,PermissionType.READ,false)
		.onSuccess(Void -> {
			instanceHandler.loadProject(requestedInstanceID)
			.onSuccess(project -> {					
				//JsonArray taskData = project.getTasksWithNames();
				// this list needs to be filtered by access
				getParticpantForUser(context.user(), project)				
				.onSuccess(participant-> {		
					if(participant.isFinished())
					{
						context.response()
						.setStatusCode(406)							
						.end("User is finished");
					}					
					else
					{
						// This is supposedly a request for a static library required for this specific element, so here you are.
						libraryHandler.handle(context);
					}
				})
				.onFailure(err -> handleError(err, context));
			})
			.onFailure(err -> handleError(err, context));
		})
		.onFailure(err -> handleError(err, context));		
	}
	
	
	/**
	 * Get the participant for the current user. 
	 * @param user the authenticated {@link User} from a routing context
	 * @param project the {@link ProjectInstance} for which the participant is requested. If there is none yet, one will be created.
	 * @return
	 */
	Future<Participant> getParticpantForUser(User user, ProjectInstance project)
	{
		
		if(user.principal().getString("username") == null)
		{
			// This is a Token User!
			return partHandler.getParticipantForToken(user.principal().getString("access_token"), project.getID());
			
		}
		else
		{
			Promise<Participant> partPromise = Promise.promise();
			JsonObject request = new JsonObject().put("username", user.principal().getString("username")).put("projectInstanceID", project.getID());
			eb.request(SoileConfigLoader.getCommand(SoileConfigLoader.USERMGR_CFG, "getParticipantForUserInProject"), request)
			.onSuccess(response -> {
				JsonObject responseObject = (JsonObject) response;
				if(responseObject.getString("participantID") != null)
				{
					partHandler.getParticipant(responseObject.getString("participantID"))
					.onSuccess(particpant -> {
						partPromise.complete(particpant);
					})
					.onFailure(err -> partPromise.fail(err));
				}
				// doesn't have one in this project yet, so we create one.
				else
				{
					partHandler.create(project)
					.onSuccess(particpant -> {
						// update the user.
						request.put("participantID", particpant.getID());
						eb.request(SoileConfigLoader.getCommand(SoileConfigLoader.USERMGR_CFG, "makeUserParticipantInProject"), request)
						.onSuccess( success -> {
							partPromise.complete(particpant);
						})
						.onFailure(err -> partPromise.fail(err));
					})
					.onFailure(err -> partPromise.fail(err));
				}
			})
			.onFailure(err -> partPromise.fail(err));
			return partPromise.future();
		}
		
	}
}
