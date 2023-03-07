package fi.abo.kogni.soile2.http_server.routes;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.abo.kogni.soile2.datamanagement.datalake.ParticipantDataLakeManager;
import fi.abo.kogni.soile2.datamanagement.git.GitFile;
import fi.abo.kogni.soile2.http_server.auth.AccessHandler;
import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization;
import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.PermissionType;
import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.Roles;
import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.TargetElementType;
import fi.abo.kogni.soile2.http_server.requestHandling.IDSpecificFileProvider;
import fi.abo.kogni.soile2.http_server.requestHandling.NonStaticHandler;
import fi.abo.kogni.soile2.projecthandling.exceptions.ObjectDoesNotExist;
import fi.abo.kogni.soile2.projecthandling.participant.Participant;
import fi.abo.kogni.soile2.projecthandling.participant.ParticipantHandler;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.ProjectInstance;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl.ProjectInstanceHandler;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl.TaskObjectInstance;
import fi.abo.kogni.soile2.utils.SoileCommUtils;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.eventbus.ReplyFailure;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.impl.HttpUtils;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.impl.URIDecoder;
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
	IDSpecificFileProvider resourceHandler;
	ProjectInstanceHandler instanceHandler;
	AccessHandler accessHandler;
	SoileAuthorization authorizationRertiever;
	ParticipantHandler partHandler;
	TargetElementType instanceType = TargetElementType.INSTANCE;
	ParticipantDataLakeManager dataLakeManager;
	EventBus eb;
	Vertx vertx;

	static final Logger LOGGER = LogManager.getLogger(ProjectInstanceRouter.class);


	public ParticipationRouter(SoileAuthorization auth, Vertx vertx, MongoClient client, ParticipantHandler partHandler, ProjectInstanceHandler projHandler, IDSpecificFileProvider fileProvider) {
		super(auth,client);
		eb = vertx.eventBus();
		this.vertx = vertx;			
		instanceHandler = projHandler;
		this.partHandler = partHandler;		
		accessHandler = new AccessHandler(getAuthForType(instanceType), instanceIDAccessHandler, roleHandler);		 	
		dataLakeManager = new ParticipantDataLakeManager(SoileConfigLoader.getServerProperty("soileResultDirectory"), vertx);
		libraryHandler = new NonStaticHandler(FileSystemAccess.ROOT, SoileConfigLoader.getServerProperty("taskLibraryFolder"), "/lib/");		
		this.resourceHandler = fileProvider;
	}

	public void submitResults(RoutingContext context)
	{				
		RequestParameters params = context.get(ValidationHandler.REQUEST_CONTEXT_KEY);
		String requestedInstanceID = params.pathParameter("id").getString();
		accessHandler.checkAccess(context.user(),requestedInstanceID, Roles.Participant,PermissionType.EXECUTE,false)
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

	/**
	 * End point for data upload
	 * TODO: Create cleanup method for orphaned files
	 * TODO: Try to develop method to avoid upload spamming.
	 * @param context
	 */
	public void uploadData(RoutingContext context)
	{
		RequestParameters params = context.get(ValidationHandler.REQUEST_CONTEXT_KEY);
		String requestedInstanceID = params.pathParameter("id").getString();

		accessHandler.checkAccess(context.user(),requestedInstanceID, Roles.Participant,PermissionType.EXECUTE,true)
		.onSuccess(Void -> 
		{
			if(context.fileUploads().size() != 1)
			{
				handleError(new HttpException(400, "Only one Upload allowed at a time"), context);
				return;
			}
			FileUpload currentUpload = context.fileUploads().get(0);
			LOGGER.info("Loading project");
			instanceHandler.loadProject(requestedInstanceID)
			.onSuccess(project -> {				
				//JsonArray taskData = project.getTasksWithNames();
				// this list needs to be filtered by access
				LOGGER.info("Retrieving Participant");
				getParticpantForUser(context.user(), project)				
				.onSuccess(participant-> {
					LOGGER.info("Obtaining current step for participant");
					participant.getCurrentStep()
					.onSuccess(step -> {
						LOGGER.info("Saving participant data");
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
		String token = params.queryParameter("token") != null ? params.queryParameter("token").getString() : null;
		// no token, so this is either an invalid call or a user signup.
		if(token == null)
		{ 
			// if we don't have a token, an authed user can also sign up to a project (even though it's not needed)
			accessHandler.checkAccess(context.user(),requestedInstanceID, Roles.Participant,PermissionType.EXECUTE,false)
			.onSuccess(authed -> {
				instanceHandler.loadProject(requestedInstanceID)
				.onSuccess(project -> {
					// this will connect the user with a new participant if they haven't already got one.
					getParticpantForUser(context.user(), project)
					.onSuccess( participant ->
					{
						handleError(new HttpException(400, "Participant already exists for user"), context);
						})
						.onFailure(doesntExists -> {
							if( doesntExists instanceof ObjectDoesNotExist)
							{
								createParticipantForUser(context.user(), project)
								.onSuccess( res -> 
								{
									context.response()
									.setStatusCode(200)						
									.putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
									.end();
								})
								.onFailure(err -> handleError(err, context));
							}
							else 
							{
								handleError(doesntExists, context);
							}
							
						});

					})
					.onFailure(err -> handleError(err, context));
				})
				.onFailure(err -> handleError(err, context));

		}
		else
		{
			// we got a token. This can be either associated with a user or not.
			instanceHandler.loadProject(requestedInstanceID)
			.onSuccess(project -> {				
				project.useToken(token)
				.onSuccess( tokenUsed -> {
					partHandler.createTokenParticipant(project, token)
					.onSuccess(participant -> {
						// if the principal is empty, that means we have not used authentication, but passed through 
						// the auth-less route
						if(context.user().principal().isEmpty())
						{
							// we don't have a user, so we just respond with the token after we started the project for this user. 
							project.startProject(participant)
							.onSuccess(position -> {
								context.response()
								.setStatusCode(200)						
								.putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
								.end(new JsonObject().put("token", participant.getToken()).encode());
							})
							.onFailure(err -> handleError(err, context));	
						}
						else
						{
							// so the user is nt empty, i.e. we build a new participant for this user (if it doesn't exist)
							// we will add execute access to the current user.					
							LOGGER.debug("Found user: \n" + context.user().principal().encodePrettily());
							LOGGER.debug("Found user: \n" + context.user().attributes().encodePrettily());
							JsonObject userData = new JsonObject();
							userData.put("username", context.user().principal().getString("username"));
							userData.put("command", "add");
							userData.put("permissions", new JsonArray().add(new JsonObject().put("target", requestedInstanceID).put("type", PermissionType.EXECUTE.toString())));							
							eb.request(SoileCommUtils.getEventBusCommand(SoileConfigLoader.USERMGR_CFG, "permissionOrRoleChange"),userData)
							.onSuccess( permissionAdded ->
							{	
								getParticpantForUser(context.user(), project)
								.onSuccess(oops -> 
								{
									handleError(new HttpException(400, "Participant already exists for user"), context);
								})
								.onFailure(doesntExists -> {
									if( doesntExists instanceof ObjectDoesNotExist)
									{
										JsonObject partData = new JsonObject().put("username", context.user().principal().getString("username"))
										.put("projectID", requestedInstanceID)
										.put("participantID", participant.getID());
									eb.request(SoileCommUtils.getEventBusCommand(SoileConfigLoader.USERMGR_CFG, "makeUserParticipantInProject"),partData)
									.onSuccess( participantAdded ->
									{
										project.startProject(participant)
										.onSuccess(position -> {
											context.response()
											.setStatusCode(200)						
											.end(new JsonObject().put("token", participant.getToken()).encode());
										})
										.onFailure(err -> handleError(err, context));
									})
									.onFailure(err -> handleError(err, context));
									}
									else
									{
										handleError(doesntExists, context);
									}

								});
								// all done;

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
	}

	public void getTaskInfo(RoutingContext context)
	{
		RequestParameters params = context.get(ValidationHandler.REQUEST_CONTEXT_KEY);
		String requestedInstanceID = params.pathParameter("id").getString();
		accessHandler.checkAccess(context.user(),requestedInstanceID, Roles.Participant,PermissionType.EXECUTE,false)
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

							eb.request("soile.task.getVersionInfo", new JsonObject().put("taskID", currentTask.getUUID()).put("version", currentTask.getVersion()))
							.onSuccess(response -> {								
								JsonObject responseBody = ((JsonObject) response.body()).getJsonObject(SoileCommUtils.DATAFIELD);
								context.response()
								.setStatusCode(200)	
								.putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
								.end(new JsonObject().put("finished", false).put("codeType", responseBody.getJsonObject("codeType")).put("id", participant.getProjectPosition()).encode());
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

		accessHandler.checkAccess(context.user(),requestedInstanceID, Roles.Participant,PermissionType.EXECUTE,false)
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

	
	public void getID(RoutingContext context)
	{
		RequestParameters params = context.get(ValidationHandler.REQUEST_CONTEXT_KEY);
		String requestedInstanceID = params.pathParameter("id").getString();

		accessHandler.checkAccess(context.user(),requestedInstanceID, Roles.Participant,PermissionType.EXECUTE,false)
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
						context.response()
						.setStatusCode(200)
						.putHeader(HttpHeaders.CONTENT_TYPE, "application/javascript")
						.end(currentTask.getInstanceID());
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
		String requestedInstanceID = context.pathParam("id");

		accessHandler.checkAccess(context.user(),requestedInstanceID, Roles.Participant,PermissionType.EXECUTE,false)
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
						//ok handle this with the NonStatic Handler
						libraryHandler.handle(context);

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
		String requestedInstanceID = context.pathParam("id");

		accessHandler.checkAccess(context.user(),requestedInstanceID, Roles.Participant,PermissionType.EXECUTE,false)
		.onSuccess(Void -> {
			instanceHandler.loadProject(requestedInstanceID)
			.onSuccess(project -> {					
				//JsonArray taskData = project.getTasksWithNames();
				// this list needs to be filtered by access
				getParticpantForUser(context.user(), project)				
				.onSuccess(participant  -> {		
					if(participant.isFinished())
					{
						context.response()
						.setStatusCode(406)							
						.end("User is finished");
					}					
					else
					{
						// Ok, this is a request for resources for a file referenced from git. 
						TaskObjectInstance currentTask = (TaskObjectInstance) project.getElement(participant.getProjectPosition());
						String uriDecodedPath = URIDecoder.decodeURIComponent(context.normalizedPath(), false);
						// if the normalized path is null it cannot be resolved
						if (uriDecodedPath == null) {
							context.next();
							return;
						}
						// will normalize and handle all paths as UNIX paths
						String treatedPath = HttpUtils.removeDots(uriDecodedPath.replace('\\', '/'));
						// +1 because we need to ignore the first / 
						String path = treatedPath.substring(treatedPath.indexOf(requestedInstanceID)+requestedInstanceID.length()+1);
						// For now we just add the "T"
						GitFile targetResource = new GitFile(path, "T" + currentTask.getUUID(),currentTask.getVersion());
						resourceHandler.returnResource(context, targetResource);						
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
	 * @param failIfExist Fail the retrieval if the participant already exists (to avoid double signup);
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
			eb.request(SoileCommUtils.getEventBusCommand(SoileConfigLoader.USERMGR_CFG, "getParticipantForUserInProject"), request)
			.onSuccess(response -> {
				JsonObject responseObject = (JsonObject) response.body();
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
					partPromise.fail(new ObjectDoesNotExist("Participant for user"));
				}
			})
			.onFailure(err -> partPromise.fail(err));
			return partPromise.future();
		}

	}		

	
	public Future<Participant> createParticipantForUser(User user, ProjectInstance project)
	{			
		Promise<Participant> partPromise = Promise.promise();
		partHandler.create(project)
		.onSuccess(particpant -> {
			// update the user.
			JsonObject request = new JsonObject().put("username", user.principal().getString("username")).put("projectInstanceID", project.getID());
			request.put("participantID", particpant.getID());
			eb.request(SoileCommUtils.getEventBusCommand(SoileConfigLoader.USERMGR_CFG, "makeUserParticipantInProject"), request)
			.onSuccess( success -> {
				partPromise.complete(particpant);
			})
			.onFailure(err -> partPromise.fail(err));
		})
		.onFailure(err -> partPromise.fail(err));
		
		return partPromise.future();
		
	}
	
	public void handleRequest(RoutingContext context, Handler<RoutingContext> method)
	{
		instanceHandler.getProjectIDForPath(context.pathParam("id"))
		.onSuccess(newID -> {
			context.pathParams().put("id", newID);
			method.handle(context);
		})
		.onFailure(err -> handleError(err, context));
	}
}
