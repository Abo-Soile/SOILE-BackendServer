package fi.abo.kogni.soile2.http_server.routes;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.aalto.scicomp.zipper.FileDescriptor;
import fi.aalto.scicomp.zipper.Zipper;
import fi.abo.kogni.soile2.datamanagement.datalake.DataLakeFile;
import fi.abo.kogni.soile2.datamanagement.datalake.ParticipantDataLakeManager;
import fi.abo.kogni.soile2.http_server.auth.AccessHandler;
import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization;
import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.PermissionType;
import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.Roles;
import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.TargetElementType;
import fi.abo.kogni.soile2.http_server.userManagement.exceptions.UserDoesNotExistException;
import fi.abo.kogni.soile2.http_server.verticles.DataBundleGeneratorVerticle.DownloadStatus;
import fi.abo.kogni.soile2.projecthandling.participant.ParticipantHandler;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl.StudyHandler;
import fi.abo.kogni.soile2.utils.SoileCommUtils;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.HttpException;
import io.vertx.ext.web.validation.RequestParameter;
import io.vertx.ext.web.validation.RequestParameters;
import io.vertx.ext.web.validation.ValidationHandler;

/**
 * Router for ProjectInstance operations (i.e. project execution, and data retrieval).
 * Documentation for public functions (aside from the constructor) can be obtained from the 
 * API document, with operationIDs mapping 1:1 to method names 
 * @author Thomas Pfau
 *
 */
public class StudyRouter extends SoileRouter {

	StudyHandler instanceHandler;
	AccessHandler instanceAccessHandler;
	AccessHandler projectAccessHandler;
	ParticipantHandler partHandler;
	ParticipantDataLakeManager dataLakeManager;
	EventBus eb;
	Vertx vertx;

	static final Logger LOGGER = LogManager.getLogger(StudyRouter.class);	

	public StudyRouter(SoileAuthorization auth, Vertx vertx, MongoClient client, ParticipantHandler partHandler, StudyHandler projHandler) {
		super(auth,client);
		eb = vertx.eventBus();
		this.vertx = vertx;		
		instanceHandler = projHandler;
		this.partHandler = partHandler;						
		dataLakeManager = new ParticipantDataLakeManager(SoileConfigLoader.getServerProperty("soileResultDirectory"), vertx);
		instanceAccessHandler = new AccessHandler(instanceAuth, instanceIDAccessHandler, roleHandler);
		projectAccessHandler = new AccessHandler(projectAuth, projectIDAccessHandler, roleHandler);
	}

	public void startProject(RoutingContext context)
	{
		RequestParameters params = context.get(ValidationHandler.REQUEST_CONTEXT_KEY);
		String id = params.pathParameter("id").getString();
		String version = params.pathParameter("version").getString();
		JsonObject sourceProjectInfo = new JsonObject().put("UUID", id).put("version", version);
		JsonObject projectData = new JsonObject().put("sourceProject", sourceProjectInfo).mergeIn(context.body().asJsonObject());
		// we need to check, whether the user has access to the actual project indicated.
		projectAuth.getAuthorizations(context.user())
		.onSuccess(Void -> {
			projectIDAccessHandler.authorize(context.user(), id, false, PermissionType.READ)			
			.onSuccess(canCreate -> {
				instanceHandler.createProjectInstance(projectData)
				.onSuccess(instance -> {
					JsonObject permissionChange = new JsonObject().put("command", "add")
							.put("username", context.user().principal().getString("username"))
							.put("permissionsProperties", new JsonObject().put("elementType", TargetElementType.INSTANCE.toString())
									.put("permissionSettings", new JsonArray().add(new JsonObject().put("type", PermissionType.FULL.toString())
											.put("target", instance.getID()))));
					eb.request(SoileCommUtils.getEventBusCommand(SoileConfigLoader.USERMGR_CFG, "permissionOrRoleChange"), permissionChange)
					.onSuccess(success -> {
						// instance was created, access was updated, everything worked fine. Now
						context.response().setStatusCode(200)
						.putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
						.end(new JsonObject().put("projectID",instance.getID()).encode());												
					})
					.onFailure(err -> handleError(err, context));
				})
				.onFailure(err -> handleError(err, context));
			})
			.onFailure(err -> handleError(new HttpException(403, err.getMessage()), context));
		})
		.onFailure(err -> {
			handleError(err, context);
		});		
	}

	public void getRunningProjectList(RoutingContext context)
	{				
		RequestParameters params = context.get(ValidationHandler.REQUEST_CONTEXT_KEY);
		RequestParameter accessParam = params.queryParameter("access");
		String access = "general";
		Boolean restrictToPermissions = false;
		if(accessParam != null)
		{
			access = accessParam.getString();			
		}
		
		Future<JsonArray> permissionsFuture;
		switch(access)		
		{
			case "read": permissionsFuture = authorizationRertiever.getReadPermissions(context.user(), TargetElementType.INSTANCE); restrictToPermissions = true; break;
			case "write": permissionsFuture = authorizationRertiever.getWritePermissions(context.user(), TargetElementType.INSTANCE); restrictToPermissions = true; break; 
			case "full": permissionsFuture = authorizationRertiever.getFullPermissions(context.user(), TargetElementType.INSTANCE); restrictToPermissions = true; break;
			default: permissionsFuture = authorizationRertiever.getGeneralPermissions(context.user(),TargetElementType.INSTANCE); break;
		}			
		Boolean onlyPermissions = restrictToPermissions;
		permissionsFuture
		.onSuccess( permissions -> {
			instanceHandler.getProjectList(permissions, onlyPermissions)		
			.onSuccess(elementList -> {	
				// this list needs to be filtered by access

				context.response()
				.setStatusCode(200)
				.putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
				.end(elementList.encode());
			})
			// this is a re
			.onFailure(err -> handleError(err, context));
		})
		.onFailure(err -> {
			if(err instanceof UserDoesNotExistException)
			{
				instanceHandler.getProjectList(new JsonArray(), false)
				.onSuccess(elementList -> {	
					// this list needs to be filtered by access

					context.response()
					.setStatusCode(200)
					.putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
					.end(elementList.encode());
				})
				.onFailure(newerr -> handleError(newerr, context));
			}
			else
			{
				handleError(err, context);
			}
		});	
	}


	public void stopProject(RoutingContext context)
	{				
		RequestParameters params = context.get(ValidationHandler.REQUEST_CONTEXT_KEY);
		String requestedInstanceID = params.pathParameter("id").getString();

		instanceAccessHandler.checkAccess(context.user(),requestedInstanceID, Roles.Researcher,PermissionType.FULL,true)
		.onSuccess(Void -> 
		{
			instanceHandler.loadStudy(requestedInstanceID)
			.onSuccess(project -> {	
				// this list needs to be filtered by access
				project.deactivate()
				.onSuccess(success -> {
					context.response()
					.setStatusCode(200)						
					.end();
				})
				.onFailure(err -> handleError(err, context));
			})
			.onFailure(err -> handleError(err, context));
		})
		.onFailure(err -> handleError(err, context));			
	}

	public void restartProject(RoutingContext context)
	{				
		RequestParameters params = context.get(ValidationHandler.REQUEST_CONTEXT_KEY);
		String requestedInstanceID = params.pathParameter("id").getString();

		instanceAccessHandler.checkAccess(context.user(),requestedInstanceID, Roles.Researcher,PermissionType.FULL,true)
		.onSuccess(Void -> 
		{
			instanceHandler.loadStudy(requestedInstanceID)
			.onSuccess(project -> {	
				// this list needs to be filtered by access
				project.activate()
				.onSuccess(success -> {
					context.response()
					.setStatusCode(200)						
					.end();
				})
				.onFailure(err -> handleError(err, context));
			})
			.onFailure(err -> handleError(err, context));
		})
		.onFailure(err -> handleError(err, context));			
	}

	public void deleteProject(RoutingContext context)
	{				
		RequestParameters params = context.get(ValidationHandler.REQUEST_CONTEXT_KEY);
		String requestedInstanceID = params.pathParameter("id").getString();
		instanceAccessHandler.checkAccess(context.user(),requestedInstanceID, Roles.Researcher,PermissionType.FULL,true)
		.onSuccess(Void -> 
		{
			instanceHandler.deleteStudy(requestedInstanceID)
			.onSuccess(deletedObject -> {
				List<Future> deletionFutures = new LinkedList<Future>();
				for(int i = 0; i < deletedObject.getJsonArray("participants").size(); ++i)
				{
					deletionFutures.add(partHandler.deleteParticipant(deletedObject.getJsonArray("participants").getString(i), false));
				}
				CompositeFuture.all(deletionFutures)
				.onSuccess(done -> {					
					context.response()
					.setStatusCode(200)						
					.end();

				})
				.onFailure(err -> { 
					// the study was deleted from the handler and the db, but something went wrong when deleting the 
					LOGGER.error("Something went wrong when deleting the study. the original db entry was: " + deletedObject.encodePrettily());
					handleError(err, context);
					});
			})
			.onFailure(err -> handleError(err, context));
		})
		.onFailure(err -> handleError(err, context));			
	}

	public void resetStudy(RoutingContext context)
	{				
		RequestParameters params = context.get(ValidationHandler.REQUEST_CONTEXT_KEY);
		String requestedInstanceID = params.pathParameter("id").getString();
		instanceAccessHandler.checkAccess(context.user(),requestedInstanceID, Roles.Researcher,PermissionType.FULL,true)
		.onSuccess(Void -> 
		{
			instanceHandler.loadStudy(requestedInstanceID)
			.onSuccess(study -> {					
				//
				
				study.reset()
				.onSuccess(participantsToDelete -> {
					List<Future> deletionFutures = new LinkedList<Future>();
					for(int i = 0; i < participantsToDelete.size(); ++i)
					{
						deletionFutures.add(partHandler.deleteParticipant(participantsToDelete.getString(i), false));
					}
					CompositeFuture.all(deletionFutures)
					.onSuccess(done -> {
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
	
	public void listDownloadData(RoutingContext context)
	{
		RequestParameters params = context.get(ValidationHandler.REQUEST_CONTEXT_KEY);
		String requestedInstanceID = params.pathParameter("id").getString();

		instanceAccessHandler.checkAccess(context.user(),requestedInstanceID, Roles.Researcher,PermissionType.READ,false)
		.onSuccess(Void -> {
			instanceHandler.loadStudy(requestedInstanceID)
			.onSuccess(project -> {					
				//JsonArray taskData = project.getTasksWithNames();
				// this list needs to be filtered by access
				partHandler.getParticipantStatusForProject(project)				
				.onSuccess(participantStatus -> {									
					JsonObject response = new JsonObject();
					response.put("participants", participantStatus)
					.put("tasks", project.getTasksInstancesWithNames());
					context.response()
					.setStatusCode(200)
					.putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
					.end(response.encode());					
				})
				.onFailure(err -> handleError(err, context));
			})
			.onFailure(err -> handleError(err, context));
		})
		.onFailure(err -> handleError(err, context));		
	}
	
	public void updateStudy(RoutingContext context)
	{				
		RequestParameters params = context.get(ValidationHandler.REQUEST_CONTEXT_KEY);
		String requestedInstanceID = params.pathParameter("id").getString();
		//TODO: not implemented properly yet
		instanceAccessHandler.checkAccess(context.user(),requestedInstanceID, Roles.Researcher,PermissionType.READ_WRITE,false)
		.onSuccess(Void -> 
		{			
			LOGGER.debug("Access granted, updating Study");
			instanceHandler.updateStudy(requestedInstanceID,context.body().asJsonObject())
			.onSuccess(studyUpdated -> {
				context.response()
				.setStatusCode(200)				
				.end();
			})
			.onFailure(err -> handleError(err, context));										
		})
		.onFailure(err -> handleError(err, context));			
	}
	
	public void getStudyProperties(RoutingContext context)
	{				
		RequestParameters params = context.get(ValidationHandler.REQUEST_CONTEXT_KEY);
		String requestedInstanceID = params.pathParameter("id").getString();
		//TODO: not implemented properly yet
		instanceAccessHandler.checkAccess(context.user(),requestedInstanceID, Roles.Researcher,PermissionType.READ,false)
		.onSuccess(Void -> 
		{			
			instanceHandler.loadStudy(requestedInstanceID)
			.onSuccess(study-> {				
				context.response()
				.setStatusCode(200)				
				.end(study.toDBJson().encode());
			})
			.onFailure(err -> handleError(err, context));										
		})
		.onFailure(err -> handleError(err, context));			
	}
	
	
	public void getProjectResults(RoutingContext context)
	{				
		RequestParameters params = context.get(ValidationHandler.REQUEST_CONTEXT_KEY);
		String requestedInstanceID = params.pathParameter("id").getString();
		//TODO: not implemented properly yet
		instanceAccessHandler.checkAccess(context.user(),requestedInstanceID, Roles.Researcher,PermissionType.READ,false)
		.onSuccess(Void -> 
		{			
			// this list needs to be filtered by access
			JsonObject requestBody = null;
			try
			{
				requestBody = context.body().asJsonObject();
				if(requestBody.fieldNames().size() != 1)
				{
					handleError(new HttpException(400, "Invalid request"), context);
					return;
				}
				else
				{
					// just add whichever name is there as the request type.
					requestBody.put("requestType", requestBody.fieldNames().iterator().next());					
				}
			}
			catch(Exception e)
			{
				// this is a request All request.
				requestBody = new JsonObject().put("requestType", "all"); 
			}
			requestBody.put("projectID", requestedInstanceID);

			eb.request("fi.abo.soile.DLCreate", requestBody)
			.onSuccess(response -> {
				String dlID = response.body().toString();
				context.response()
				.setStatusCode(200)
				.putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
				.end(new JsonObject().put("downloadID",dlID).encode());
			})
			.onFailure(err -> handleError(err, context));										
		})
		.onFailure(err -> handleError(err, context));			
	}

	public void downloadTest(RoutingContext context)
	{				
		RequestParameters params = context.get(ValidationHandler.REQUEST_CONTEXT_KEY);
		String requestedInstanceID = params.pathParameter("id").getString();
		String dlID = params.pathParameter("downloadid").getString();
		instanceAccessHandler.checkAccess(context.user(),requestedInstanceID, Roles.Researcher,PermissionType.READ,false)
		.onSuccess(Void -> 
		{
			// this list needs to be filtered by access
			eb.request("fi.abo.soile.DLStatus", new JsonObject().put("downloadID",dlID))
			.onSuccess(response -> {
				JsonObject responseBody = (JsonObject) response.body();					
				context.response()
				.setStatusCode(200)
				.putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
				.end(responseBody.encode());
			})
			.onFailure(err -> handleError(err, context));										
		})
		.onFailure(err -> handleError(err, context));			
	}	

	public void downloadResults(RoutingContext context)
	{				
		RequestParameters params = context.get(ValidationHandler.REQUEST_CONTEXT_KEY);
		String requestedInstanceID = params.pathParameter("id").getString();
		String dlID = params.pathParameter("downloadid").getString();

		instanceAccessHandler.checkAccess(context.user(),requestedInstanceID, Roles.Researcher,PermissionType.READ,false)
		.onSuccess(Void -> 
		{
			// this list needs to be filtered by access
			LOGGER.debug("Requesting finished download file from Eventbus for id " + dlID);
			eb.request("fi.abo.soile.DLFiles", new JsonObject().put("downloadID",dlID))
			.onSuccess(response -> {				
				JsonObject responseBody = (JsonObject) response.body();
				if(responseBody.getString("status").equals(DownloadStatus.downloadReady.toString()))
				{
					List<FileDescriptor> dLFiles = new LinkedList<>();
					for(int i = 0; i < responseBody.getJsonArray("files").size(); i++)
					{
						dLFiles.add(new DataLakeFile(responseBody.getJsonArray("files").getJsonObject(i)));
					}
					try
					{
						Zipper pump = new Zipper(vertx, dLFiles.iterator());
						// the response is a chunked zip file.
						context.response().putHeader("content-type", "application/zip")
						.putHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + dlID + ".zip\"")
						.setStatusCode(200)
						.setChunked(true);
						pump.pipeTo(context.response()).onSuccess(success -> {
							LOGGER.debug("Download " + dlID + " successfullytransmitted");
						}).onFailure(err -> {
							LOGGER.error("Download " + dlID + " failed");
							LOGGER.error(err);
							context.response()
							.putHeader("content-type", "text/plain")
							.setStatusCode(500)
							.end("Failed because of: " + err.getMessage());
						});											
					}
					catch(IOException e)
					{
						handleError(e, context);
					}			
				}
				else
				{
					LOGGER.debug("Status was: " + responseBody.getString("status") + " // While ready status should be: " + DownloadStatus.downloadReady.toString());

					context.response()
					.setStatusCode(503)					
					.end("Download not yet ready");

				}
			})
			.onFailure(err -> handleError(err, context));										
		})
		.onFailure(err -> handleError(err, context));			
	}


	public void createTokens(RoutingContext context)
	{				
		RequestParameters params = context.get(ValidationHandler.REQUEST_CONTEXT_KEY);
		String requestedInstanceID = params.pathParameter("id").getString();

		boolean unique = params.queryParameter("unique") == null ? false : params.queryParameter("unique").getBoolean();
		int count = params.queryParameter("count") == null ? 0 : params.queryParameter("count").getInteger();

		instanceAccessHandler.checkAccess(context.user(),requestedInstanceID, Roles.Researcher,PermissionType.FULL,false)
		.onSuccess(Void -> 
		{
			instanceHandler.loadStudy(requestedInstanceID)
			.onSuccess(instance -> {
				if(unique)
				{
					instance.createPermanentAccessToken()
					.onSuccess(token -> {
						context.response()
						.setStatusCode(200)
						.putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
						.end(token);
					})
					.onFailure(err -> handleError(err, context));
				}
				else
				{

					instance.createAccessTokens(count).
					onSuccess(tokenArray -> {
						LOGGER.debug("Replying with: \n " + tokenArray.encodePrettily());
						context.response()
						.setStatusCode(200)
						.putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
						.end(tokenArray.encode());
					})
					.onFailure(err -> handleError(err, context));
				}

			})
			.onFailure(err -> handleError(err, context));
		})
		.onFailure(err -> handleError(err, context));			
	}	
}
