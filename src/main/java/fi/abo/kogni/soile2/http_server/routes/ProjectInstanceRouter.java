package fi.abo.kogni.soile2.http_server.routes;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.aalto.scicomp.zipper.FileDescriptor;
import fi.aalto.scicomp.zipper.Zipper;
import fi.abo.kogni.soile2.datamanagement.datalake.DataLakeFile;
import fi.abo.kogni.soile2.http_server.auth.AccessHandler;
import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization;
import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.PermissionType;
import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.Roles;
import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.TargetElementType;
import fi.abo.kogni.soile2.http_server.auth.SoileIDBasedAuthorizationHandler;
import fi.abo.kogni.soile2.http_server.auth.SoileRoleBasedAuthorizationHandler;
import fi.abo.kogni.soile2.http_server.verticles.DataBundleGeneratorVerticle.DownloadStatus;
import fi.abo.kogni.soile2.projecthandling.participant.Participant;
import fi.abo.kogni.soile2.projecthandling.participant.ParticipantHandler;
import fi.abo.kogni.soile2.projecthandling.projectElements.Project;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.AccessProjectInstance;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.ProjectInstance;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl.ProjectInstanceHandler;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.mongo.MongoAuthorization;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.validation.RequestParameters;
import io.vertx.ext.web.validation.ValidationHandler;

public class ProjectInstanceRouter extends SoileRouter {

	ProjectInstanceHandler instanceHandler;
	AccessHandler instanceAccessHandler;
	AccessHandler projectAccessHandler;
	SoileAuthorization authorizationRertiever;
	SoileIDBasedAuthorizationHandler projectIDAccessHandler;
	ParticipantHandler partHandler;
	TargetElementType instanceType = TargetElementType.INSTANCE;
	MongoAuthorization mongoAuth;
	EventBus eb;
	Vertx vertx;
	
	static final Logger LOGGER = LogManager.getLogger(ProjectInstanceRouter.class);


	public ProjectInstanceRouter(SoileAuthorization auth, Vertx vertx, MongoClient client, ParticipantHandler partHandler, ProjectInstanceHandler projHandler) {
		eb = vertx.eventBus();
		this.vertx = vertx;
		
		authorizationRertiever = auth;
		instanceHandler = projHandler;
		this.partHandler = partHandler;
		MongoAuthorization mongoAuth = auth.getAuthorizationForOption(instanceType);
		SoileRoleBasedAuthorizationHandler roleHandler = new SoileRoleBasedAuthorizationHandler();
		
		SoileIDBasedAuthorizationHandler instanceIDAccessHandler = new SoileIDBasedAuthorizationHandler(new AccessProjectInstance().getTargetCollection(), client);
		projectIDAccessHandler = new SoileIDBasedAuthorizationHandler(new Project().getTargetCollection(), client);
		instanceAccessHandler = new AccessHandler(mongoAuth, instanceIDAccessHandler, roleHandler);
	}

	public void startProject(RoutingContext context)
	{
		RequestParameters params = context.get(ValidationHandler.REQUEST_CONTEXT_KEY);
		String id = params.pathParameter("id").getString();
		String version = params.pathParameter("version").getString();		
		JsonObject projectData = params.body().getJsonObject().put("uuid", id).put("version", version).mergeIn(context.body().asJsonObject());
		// we need to check, whether the user has access to the actual project indicated.
		mongoAuth.getAuthorizations(context.user())
		.onSuccess(Void -> {
			projectIDAccessHandler.authorize(context.user(), id, false, PermissionType.READ)			
			.onSuccess(canCreate -> {
				instanceHandler.createProjectInstance(projectData)
				.onSuccess(instance -> {
					JsonObject permissionChange = new JsonObject().put("command", "addCommand")
							.put("username", context.user().principal().getString("username"))
							.put("permissions", new JsonObject().put("elementType", SoileConfigLoader.INSTANCE)
									.put("permissions", new JsonArray().add(new JsonObject().put("type", PermissionType.FULL.toString())
											.put("target", instance.getID()))));
					eb.request(SoileConfigLoader.getCommand(SoileConfigLoader.USERMGR_CFG,"permissionOrRoleChange"), permissionChange)
					.onSuccess(success -> {
						// instance was created, access was updated, everything worked fine. Now
						context.response().setStatusCode(200)
						.putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
						.end(new JsonObject().put("projectID",instance.getID()).encode());												
					})
					.onFailure(err -> context.fail(500, err));
				})
				.onFailure(err -> context.fail(500, err));
			})
			.onFailure(err -> context.fail(403, err));
		})
		.onFailure(err -> {
			context.fail(500,err);
		});		
	}

	public void getRunningProjectList(RoutingContext context)
	{				
		instanceAccessHandler.checkAccess(context.user(),null, Roles.Researcher,null,true)
		.onSuccess(Void -> 
		{
			authorizationRertiever.getGeneralPermissions(context.user(),instanceType)
			.onSuccess( permissions -> {
				instanceHandler.getProjectList(permissions)
				.onSuccess(elementList -> {	
					// this list needs to be filtered by access

					context.response()
					.setStatusCode(200)
					.putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
					.end(elementList.encode());
				})
				.onFailure(err -> handleError(err, context));
			})
			.onFailure(err -> handleError(err, context));	
		})
		.onFailure(err -> handleError(err, context));
	}


	public void stopProject(RoutingContext context)
	{				
		RequestParameters params = context.get(ValidationHandler.REQUEST_CONTEXT_KEY);
		String requestedInstanceID = params.pathParameter("id").getString();
				
		instanceAccessHandler.checkAccess(context.user(),requestedInstanceID, Roles.Researcher,PermissionType.FULL,true)
		.onSuccess(Void -> 
		{
			instanceHandler.loadProject(requestedInstanceID)
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
			instanceHandler.loadProject(requestedInstanceID)
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
			instanceHandler.loadProject(requestedInstanceID)
			.onSuccess(project -> {					
				// this list needs to be filtered by access
				project.delete()
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




	public void listDownloadData(RoutingContext context)
	{
		RequestParameters params = context.get(ValidationHandler.REQUEST_CONTEXT_KEY);
		String requestedInstanceID = params.pathParameter("id").getString();
		
		instanceAccessHandler.checkAccess(context.user(),requestedInstanceID, Roles.Researcher,PermissionType.READ,false)
		.onSuccess(Void -> {
			instanceHandler.loadProject(requestedInstanceID)
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

	public void getProjectResults(RoutingContext context)
	{				
		RequestParameters params = context.get(ValidationHandler.REQUEST_CONTEXT_KEY);
		String requestedInstanceID = params.pathParameter("id").getString();
		//TODO: not implemented properly yet
		instanceAccessHandler.checkAccess(context.user(),requestedInstanceID, Roles.Researcher,PermissionType.READ,false)
		.onSuccess(Void -> 
		{			
			// this list needs to be filtered by access
			JsonObject requestBody = context.body().asJsonObject();
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
			eb.request("fi.abo.soile.DLCreate", new JsonObject().put("downloadID",dlID))
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
							LOGGER.info("Download " + dlID + " successfullytransmitted");
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
					context.response()
					.setStatusCode(406)					
					.end("Download not ready");

				}
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
	
	/**
	 * Start preparing a download for the indicated data.
	 * @param participants the participants to prepare a download for.
	 * @return Whether the preparation started.
	 */
	private Future<String> prepareParticipantDownload(JsonArray participants)
	{
		Promise<String> downloadIDPromise = Promise.promise();
		
		return downloadIDPromise.future();
	}

}
