package fi.abo.kogni.soile2.projecthandling.projectElements;

import fi.abo.kogni.soile2.datamanagement.datalake.DataLakeFile;
import fi.abo.kogni.soile2.datamanagement.git.GitFile;
import fi.abo.kogni.soile2.datamanagement.git.GitResourceManager;
import fi.abo.kogni.soile2.datamanagement.utils.TimeStampedMap;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;

public class TaskDataHandler{

	private GitResourceManager resourceManager;
	private TimeStampedMap<GitFile, DataLakeFile> gitElements;
	
	public TaskDataHandler(GitResourceManager manager)
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
	
	public Future<DataLakeFile> handleGetFile(String taskID, String taskVersion, String filename)
	{		
		Promise<DataLakeFile> filePromise = Promise.promise();
		String repoID = Task.typeID + taskID;
		GitFile f = new GitFile(filename, repoID, taskVersion);		
		gitElements.getData(f)
		.onSuccess(datalakeFile -> {			
			filePromise.complete(datalakeFile);
		});
		return filePromise.future();
	}
	public Future<String> handlePostFile(String taskID, String taskVersion, String filename, FileUpload upload)
	{
		Promise<String> successPromise = Promise.promise();
		String repoID = Task.typeID + taskID;
		GitFile f = new GitFile(filename, repoID, taskVersion);
		resourceManager.writeElement(f, upload )
		.onSuccess(version -> {
			successPromise.complete(version);				
		});		
		return successPromise.future();
	}
}
