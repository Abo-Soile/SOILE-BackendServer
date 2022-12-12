package fi.abo.kogni.soile2.datamanagement.verticle;

import fi.abo.kogni.soile2.datamanagement.datalake.DataLakeFile;
import fi.abo.kogni.soile2.datamanagement.git.GitFile;
import fi.abo.kogni.soile2.datamanagement.git.ResourceManager;
import fi.abo.kogni.soile2.datamanagement.utils.TimeStampedMap;
import fi.abo.kogni.soile2.projecthandling.projectElements.Task;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class TaskDataHandler{

	public static String TASKDATAADRESS = "data:taskData";
	private ResourceManager resourceManager;
	private TimeStampedMap<GitFile, DataLakeFile> gitElements;
	
	public TaskDataHandler(ResourceManager manager)
	{
		resourceManager = manager;
		gitElements = new TimeStampedMap<>(resourceManager, 3600*2);
	}
	
	/**
	 * Clean up the stored elements that are older than their ttl.
	 */
	public void cleanUp()
	{
		gitElements.cleanup();
	}
	
	public void handleGetFile(RoutingContext ctx)
	{		
		String repoID = Task.typeID + ctx.pathParam("id");
		String taskVersion = ctx.pathParam("version");
		String filename = ctx.pathParam("file");
		GitFile f = new GitFile(filename, repoID, taskVersion);		
		gitElements.getData(f)
		.onSuccess(datalakeFile -> {			
			ctx.response().
			setStatusCode(200)
			.putHeader(HttpHeaders.CONTENT_TYPE, datalakeFile.getFormat())
			.sendFile(datalakeFile.getAbsolutePath());		
		});
	}
	public void handlePostFile(RoutingContext ctx)
	{
		String repoID = Task.typeID + ctx.pathParam("id");
		String taskVersion = ctx.pathParam("version");
		String filename = ctx.pathParam("file");
		GitFile f = new GitFile(filename, repoID, taskVersion);
		if(ctx.fileUploads().size() != 1)
		{
			ctx.fail(400);
		}
		else
		{
			resourceManager.writeElement(f, ctx.fileUploads().get(0) )
			.onSuccess(version -> {				
				ctx.response().setStatusCode(200)
				.putHeader(HttpHeaders.CONTENT_TYPE, "application/json; charset=utf-8")
				.end(new JsonObject().put("version", version).encode());
			});
		}
	}
}
