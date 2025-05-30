package fi.abo.kogni.soile2.http_server.routes;

import java.util.regex.Pattern;

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
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.Study;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl.StudyHandler;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl.TaskObjectInstance;
import fi.abo.kogni.soile2.utils.SoileCommUtils;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.Future;
import io.vertx.core.Handler;
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

/**
 * Class implementing routes for Participation
 * @author Thomas Pfau
 *
 */
public class ParticipationRouter extends SoileRouter{


	NonStaticHandler libraryHandler;
	IDSpecificFileProvider resourceHandler;
	StudyHandler studyHandler;
	AccessHandler accessHandler;
	SoileAuthorization authorizationRertiever;
	ParticipantHandler partHandler;
	TargetElementType studyType = TargetElementType.STUDY;
	ParticipantDataLakeManager dataLakeManager;
	EventBus eb;
	Vertx vertx;

	static final Logger LOGGER = LogManager.getLogger(StudyRouter.class);

	/**
	 * Default constructor for router
	 * @param auth The {@link SoileAuthorization} for auth checks
	 * @param vertx The {@link Vertx} instance for communication
	 * @param client the {@link MongoClient} for db access
	 * @param partHandler the {@link ParticipantHandler} for participant access
	 * @param projHandler the {@link StudyHandler} for study access
	 * @param fileProvider A {@link IDSpecificFileProvider} for file retrieval
	 */
	public ParticipationRouter(SoileAuthorization auth, Vertx vertx, MongoClient client, ParticipantHandler partHandler, StudyHandler projHandler, IDSpecificFileProvider fileProvider) {
		super(auth,client);
		eb = vertx.eventBus();
		this.vertx = vertx;			
		studyHandler = projHandler;
		this.partHandler = partHandler;		
		accessHandler = new AccessHandler(getAuthForType(studyType), studyIDAccessHandler, roleHandler);		 	
		dataLakeManager = new ParticipantDataLakeManager(SoileConfigLoader.getServerProperty("soileResultDirectory"), vertx);
		libraryHandler = new NonStaticHandler(FileSystemAccess.ROOT, SoileConfigLoader.getServerProperty("taskLibraryFolder"), "/lib/");		
		this.resourceHandler = fileProvider;
	}

	/**
	 * Submit results for the {@link Participant} contained in the context
	 * @param context The {@link RoutingContext} containing the results and the {@link Participant}
	 */
	public void submitResults(RoutingContext context)
	{				
		String requestedInstanceID = context.pathParam("id");;
		accessHandler.checkAccess(context.user(),requestedInstanceID, Roles.Participant,PermissionType.EXECUTE,false)
		.onSuccess(Void -> 
		{
			loadStudy(requestedInstanceID)
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
	 * @param context the {@link RoutingContext} this happens in
	 */
	public void uploadData(RoutingContext context)
	{

		String requestedInstanceID = context.pathParam("id");;

		accessHandler.checkAccess(context.user(),requestedInstanceID, Roles.Participant,PermissionType.EXECUTE,true)
		.onSuccess(Void -> 
		{
			if(context.fileUploads().size() != 1)
			{
				handleError(new HttpException(400, "Only one Upload allowed at a time"), context);
				return;
			}
			FileUpload currentUpload = context.fileUploads().get(0);
			LOGGER.debug("Loading project " + requestedInstanceID);
			loadStudy(requestedInstanceID)
			.onSuccess(project -> {				
				//JsonArray taskData = project.getTasksWithNames();
				// this list needs to be filtered by access
				LOGGER.debug("Retrieving Participant for project " + project.getID());
				getParticpantForUser(context.user(), project)				
				.onSuccess(participant-> {
					LOGGER.debug("Obtaining current step for participant " + participant.getID());
					participant.getCurrentStep()
					.onSuccess(step -> {
						LOGGER.debug("Saving participant data");
						dataLakeManager.storeParticipantData(participant.getID(), step, participant.getStudyPosition(), currentUpload)
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

	/**
	 * Withdraw the participant in the given Context from the Study
	 * @param context The {@link RoutingContext} to retrieve the user/participant from
	 */
	public void withdrawFromStudy(RoutingContext context)
	{
		RequestParameters params = context.get(ValidationHandler.REQUEST_CONTEXT_KEY);
		String requestedInstanceID = params.pathParameter("id").getString();					

		accessHandler.checkAccess(context.user(),requestedInstanceID, Roles.Participant,PermissionType.EXECUTE,true)
		.onSuccess(Void -> 
		{			
			loadStudy(requestedInstanceID)
			.onSuccess(study -> {				
				//JsonArray taskData = project.getTasksWithNames();
				// this list needs to be filtered by access
				LOGGER.debug("Retrieving Participant for project " + study.getID());
				getParticpantForUser(context.user(), study)
				.onSuccess(participant -> {				
					partHandler.deleteParticipant(participant.getID(), false)
					.onSuccess(deleted -> {
						if(!isTokenUser(context.user()))
						{
							JsonObject partData = new JsonObject().put("username", context.user().principal().getString("username"))
									.put("studyID", study.getID())
									.put("participantID", participant.getID());
							// we also need to remove the participant from the current user.
							eb.request("soile.umanager.removeParticipantFromStudy", partData)
							.onSuccess( success -> {
								context.response()
								.setStatusCode(200)														
								.end();
							})
							.onFailure(err -> handleError(err, context));
						}
						else
						{
							context.response()
							.setStatusCode(200)														
							.end();
						}

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
	 * Sign the user/participant from the context up to the study in the context
	 * @param context the {@link RoutingContext} to extract the user/participant from
	 */
	public void signUpForProject(RoutingContext context)
	{
		RequestParameters params = context.get(ValidationHandler.REQUEST_CONTEXT_KEY);
		String requestedInstanceID = context.pathParam("id");
		String token = params.queryParameter("token") != null ? params.queryParameter("token").getString() : null;		
		// no token, so this is either an invalid call or a user signup.
		if(token == null)
		{ 
			accessHandler.checkRestricted(requestedInstanceID)
			.onSuccess(priv -> {
				if(priv)	
				{
					// if we don't have a token, an authed user can also sign up to a project (even though it's not needed)
					accessHandler.checkAccess(context.user(),requestedInstanceID, Roles.Participant,PermissionType.EXECUTE,false)
					.onSuccess(authed -> {				
						loadStudy(requestedInstanceID)
						.onSuccess(project -> {
							// this will connect the user with a new participant if they haven't already got one.
							createParticipant(project, "", context)
							.onSuccess(tokenForUser -> {
								context.response()
								.setStatusCode(200)						
								.putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
								.end(new JsonObject().put("token", tokenForUser).encode());
							})
							.onFailure(err -> handleError(err, context));
						})
						.onFailure(err -> handleError(err, context));

					})
					.onFailure(err -> handleError(err, context));
				}
				else
				{
					loadStudy(requestedInstanceID)
					.onSuccess(project -> {
						// this will connect the user with a new participant if they haven't already got one.
						createParticipant(project, "", context)
						.onSuccess(tokenForUser -> {
							context.response()
							.setStatusCode(200)						
							.putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
							.end(new JsonObject().put("token", tokenForUser).encode());
						})
						.onFailure(err -> handleError(err, context));
					})
					.onFailure(err -> handleError(err, context));
				}
			})
			.onFailure(err -> handleError(err, context));

		}
		else
		{
			// we got a token. This can be either associated with a user or not.
			loadStudy(requestedInstanceID)
			.onSuccess(project -> {				
				project.useToken(token)
				.onSuccess( tokenUsed -> {
					createParticipant(project, token, context)
					.onSuccess(tokenUserToken ->
					{
						context.response()
						.setStatusCode(200)						
						.putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
						.end(new JsonObject().put("token", tokenUserToken).encode());
					})
					.onFailure(err -> handleError(err, context));



				})
				.onFailure(err -> handleError(err, context));
			})
			.onFailure(err -> handleError(err, context));
		}
	}

	/**
	 * Create a participant based on whether the context has an actual (non-empty) authentication and whether a token was provided. 
	 * @param study the Study to create a participant in
	 * @param token the Token (can be empty) indicating the user
	 * @param user the User (can be <code>null</code>)
	 * @return
	 */
	Future<Participant> createParticipant(Study study, String token, User user)
	{

		if(user != null  && !user.principal().isEmpty())
		{						
			return partHandler.createParticipant(study, token, false);
		}
		else
		{
			return partHandler.createParticipant(study, token, true);
		}
	}


	private Future<String> createParticipant(Study study, String token, RoutingContext context)
	{

		Promise<String> tokenPromise = Promise.<String>promise();
		// if there is no user in the request, create a new token (and participant)
		if(context.user()  == null || context.user().principal().isEmpty())
		{		
			createParticipant(study, token, context.user())
			.onSuccess(participant -> {
				// if the principal is empty, that means we have not used authentication, but passed through 
				// the auth-less route
				// we don't have a user, so we just respond with the token after we started the project for this participant. 
				study.startStudy(participant)
				.onSuccess(position -> {
					tokenPromise.complete(participant.getToken());					
				})
				.onFailure(err -> tokenPromise.fail(err));	
			})
			.onFailure(err -> tokenPromise.fail(err));
		}
		else
		{
			getParticpantForUser(context.user(), study)
			.onSuccess(oops -> 
			{
				tokenPromise.fail(new HttpException(400, "Participant already exists for user"));
			})
			.onFailure(noParticipant -> {	
				if( noParticipant instanceof ObjectDoesNotExist)
				{
					// so the user is not empty AND there is no Participant for the current user. We have to create one.
					createParticipant(study, token, context.user())
					.onSuccess(participant -> {
						// we will add execute access to the current user.
						// This has to be a DB user, as token users are not authenticated at this end-point
						JsonArray permissionSettings = new JsonArray().add(new JsonObject().put("target", study.getID())
								.put("type", PermissionType.EXECUTE.toString()));
						JsonObject permissionsProperties = new JsonObject().put("elementType", TargetElementType.STUDY.toString())
								.put("permissionSettings", permissionSettings);
						JsonObject userData = new JsonObject();
						userData.put("username", context.user().principal().getString("username"));
						userData.put("command", "add");
						userData.put("permissionsProperties", permissionsProperties);
						eb.request("soile.umanager.permissionOrRoleChange",userData)
						.onSuccess( permissionAdded ->
						{	
							JsonObject partData = new JsonObject().put("username", context.user().principal().getString("username"))
									.put("studyID", study.getID())
									.put("participantID", participant.getID());
							eb.request("soile.umanager.makeUserParticipantInStudy",partData)
							.onSuccess( participantAdded ->
							{
								study.startStudy(participant)
								.onSuccess(position -> {
									tokenPromise.complete(participant.getToken());								
								})
								.onFailure(err -> tokenPromise.fail(err));
							})
							.onFailure(err -> tokenPromise.fail(err));						

						})
						.onFailure(err -> handleError(err, context));
					})
					.onFailure(err -> tokenPromise.fail(err));				
				}			
				else
				{
					tokenPromise.fail(noParticipant);
				}
			});
		}
		return tokenPromise.future();
	}

	/**
	 * Get Information about the Task
	 * @param context The {@link RoutingContext} indicating the Task
	 */
	public void getTaskInfo(RoutingContext context)
	{
		String requestedInstanceID = context.pathParam("id");;
		accessHandler.checkAccess(context.user(),requestedInstanceID, Roles.Participant,PermissionType.EXECUTE,false)
		.onSuccess(Void -> {
			loadStudy(requestedInstanceID)
			.onSuccess(study -> {					
				//JsonArray taskData = project.getTasksWithNames();
				// this list needs to be filtered by access
				getParticpantForUser(context.user(), study)				
				.onSuccess(participant-> {		
					if(participant.isFinished())
					{
						context.response()
						.setStatusCode(200)	
						.putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
						.end(new JsonObject().put("finished", true).put("codeType", "").put("taskID","").encode());
					}					
					else
					{
						try {
							TaskObjectInstance currentTask = (TaskObjectInstance)study.getElement(participant.getStudyPosition());							
							eb.request("soile.task.getVersionInfo", new JsonObject().put("UUID", currentTask.getUUID()).put("version", currentTask.getVersion()))
							.onSuccess(response -> {								
								JsonObject responseBody = ((JsonObject) response.body()).getJsonObject(SoileCommUtils.DATAFIELD);
								context.response()
								.setStatusCode(200)	
								.putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
								.end(new JsonObject().put("finished", false)
													 .put("codeType", responseBody.getJsonObject("codeType"))
													 .put("outputs", currentTask.getOutputs())
													 .put("persistent", currentTask.getPersistent())
													 .put("id", participant.getStudyPosition())
													 .put("participantID", participant.getID())
													 .encode());
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

	/**
	 * Get the persistent data for the User in the given Context (Study etc)
	 * @param context The {@link RoutingContext} indicating which Persistent data to obtain for which user
	 */
	public void getPersistentData(RoutingContext context)
	{
		String requestedInstanceID = context.pathParam("id");;
		accessHandler.checkAccess(context.user(),requestedInstanceID, Roles.Participant,PermissionType.EXECUTE,false)
		.onSuccess(Void -> {
			loadStudy(requestedInstanceID)
			.onSuccess(project -> {					
				//JsonArray taskData = project.getTasksWithNames();
				// this list needs to be filtered by access
				getParticpantForUser(context.user(), project)				
				.onSuccess(participant-> {							
					context.response()
					.setStatusCode(200)	
					.putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
					.end(participant.getPersistentData().encode());

				})
				.onFailure(err -> handleError(err, context));
			})
			.onFailure(err -> handleError(err, context));
		})
		.onFailure(err -> handleError(err, context));		
	}

	/**
	 * Run a given task (i.e. extract the task, compile it and return the corresponding code
	 * @param context The {@link RoutingContext} indicating the code to extract
	 */
	public void runTask(RoutingContext context)
	{
		String requestedInstanceID = context.pathParam("id");

		accessHandler.checkAccess(context.user(),requestedInstanceID, Roles.Participant,PermissionType.EXECUTE,false)
		.onSuccess(Void -> {
			loadStudy(requestedInstanceID)
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
						TaskObjectInstance currentTask = (TaskObjectInstance) project.getElement(participant.getStudyPosition());
						eb.request(SoileConfigLoader.getVerticleProperty("gitCompilationAddress"),
								new JsonObject().put("UUID", currentTask.getUUID())
								.put("type", currentTask.getCodeType())
								.put("version", currentTask.getVersion()))
						.onSuccess(response -> {
							JsonObject responseBody = (JsonObject) response.body();
							context.response()
							.setStatusCode(200)
							.putHeader(HttpHeaders.CONTENT_TYPE,SoileConfigLoader.getMimeTypeForTaskLanugage(currentTask.getCodeType().getString("language")))
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


	/**
	 * Get the ID of the current Task the {@link Participant} in this context should be pointed to.
	 * @param context The {@link RoutingContext} to extract the {@link Participant} from 
	 */
	public void getID(RoutingContext context)
	{
		String requestedInstanceID = context.pathParam("id");

		accessHandler.checkAccess(context.user(),requestedInstanceID, Roles.Participant,PermissionType.EXECUTE,false)
		.onSuccess(Void -> {
			loadStudy(requestedInstanceID)
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
						TaskObjectInstance currentTask = (TaskObjectInstance) project.getElement(participant.getStudyPosition());
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
	
	/**
	 * Get the Library specified in the RoutingContext
	 * @param context The {@link RoutingContext} to extract the target from
	 */
	public void getLib(RoutingContext context)
	{
		String requestedInstanceID = context.pathParam("id");

		accessHandler.checkAccess(context.user(),requestedInstanceID, Roles.Participant,PermissionType.EXECUTE,false)
		.onSuccess(Void -> {
			loadStudy(requestedInstanceID)
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

	/**
	 * Get the Resource (source) for the given Context request
	 * @param context The {@link RoutingContext} indicating the location
	 */
	public void getResourceForExecution(RoutingContext context)
	{
		String requestedInstanceID = context.pathParam("id");
		String taskInstanceID = context.pathParam("taskID");
		String pathPrefix = requestedInstanceID + "/" + taskInstanceID; 
		accessHandler.checkAccess(context.user(),requestedInstanceID, Roles.Participant,PermissionType.EXECUTE,false)
		.onSuccess(Void -> {
			loadStudy(requestedInstanceID)
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
						TaskObjectInstance currentTask = (TaskObjectInstance) project.getElement(participant.getStudyPosition());
						String treatedPath = normalizePath(context.normalizedPath());
						if (treatedPath == null) {
							context.next();
							return;
						}
						// +1 because we need to ignore the first / 
						String path = treatedPath.substring(treatedPath.indexOf(pathPrefix)+pathPrefix.length()+1);
						LOGGER.debug("Requested path is: " + path);
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
	 * @param project the {@link Study} for which the participant is requested. If there is none yet, one will be created.
	 * @param failIfExist Fail the retrieval if the participant already exists (to avoid double signup);
	 * @return The {@link Participant} associated with the User in the given {@link Study}
	 */
	Future<Participant> getParticpantForUser(User user, Study project)
	{

		if(isTokenUser(user))
		{
			// This is a Token User!
			return partHandler.getParticipantForToken(user.principal().getString("access_token"), project.getID());
		}
		else
		{
			Promise<Participant> partPromise = Promise.promise();
			JsonObject request = new JsonObject().put("username", user.principal().getString("username")).put("studyID", project.getID());
			eb.request("soile.umanager.getParticipantForUserInStudy", request)
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

	/**
	 * Handle the given RoutingContext with the given Handler and handle arising errors
	 * Specific to Study routes, i.e. will check Study is available etc pp.
	 * @param context The {@link RoutingContext} to handle
	 * @param method The Handler method that should be applied.
	 */
	public void handleRequest(RoutingContext context, Handler<RoutingContext> method)
	{
		String projectID = context.pathParam("id");
		studyHandler.loadUpToDateStudy(projectID)
		.onSuccess(exists -> {		
			method.handle(context);
		})
		.onFailure( lookupError -> {
			if(lookupError instanceof ObjectDoesNotExist)
			{
				studyHandler.getProjectIDForPath(projectID)
				.onSuccess(newID -> {
					LOGGER.debug("Rerouting from " +  context.normalizedPath() + " to : " + context.normalizedPath().replaceFirst("/"+Pattern.quote(projectID) + "(?=[/$])", "/" + newID ));
					context.reroute(context.normalizedPath().replaceFirst("/"+Pattern.quote(projectID) + "(?=[/$])", "/" + newID ));
				})
				.onFailure(err -> handleError(err, context));
			}
			else
			{
				handleError(lookupError, context);
			}
		});
	}

	/**
	 * Load the study with the given ID
	 * @param id the ID of the Study 
	 * @return A {@link Future} of the requested {@link Study} or a failed future if it doesn't exist, 
	 */
	private Future<Study> loadStudy(String id)
	{
		Promise<Study> studyPromise = Promise.promise();
		studyHandler.loadUpToDateStudy(id).
		onSuccess(project -> {
			project.isActive()
			.onSuccess(active -> {
				if(active)
				{
					studyPromise.complete(project);	
				}
				else
				{
					studyPromise.fail(new HttpException(410,"Project is currently inactive"));	
				}
			})
			.onFailure(err -> studyPromise.fail(err));			
		})
		.onFailure(err -> studyPromise.fail(err));
		return studyPromise.future();

	}
}
