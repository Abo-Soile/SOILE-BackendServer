package fi.abo.kogni.soile2.projecthandling.projectElements;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;

import fi.aalto.scicomp.zipper.FileDescriptor;
import fi.aalto.scicomp.zipper.Zipper;
import fi.abo.kogni.soile2.datamanagement.datalake.DataLakeResourceManager;
import fi.abo.kogni.soile2.datamanagement.git.GitFile;
import fi.abo.kogni.soile2.projecthandling.projectElements.impl.ElementManager;
import fi.abo.kogni.soile2.projecthandling.projectElements.impl.Task;
import fi.abo.kogni.soile2.projecthandling.projectElements.impl.TaskResourceFile;
import fi.abo.kogni.soile2.utils.SoileCommUtils;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class TaskBundler {
	
	Zipper zipper;
	DataLakeResourceManager dlrmgr;
	EventBus eb;
	Vertx vertx;
	ElementManager<Task> taskManager;
	public Future< List<FileDescriptor> > generateTaskFileList(String taskUUID, String version)
	{
		Promise<List<FileDescriptor>> listPromise = Promise.promise();
		eb.request("soile.git.getResourceList", new JsonObject().put("repoID", taskManager.getGitIDForUUID(taskUUID)).put("version", version))
		.onSuccess(resourceList -> {
			JsonArray resourceArray = ((JsonObject)resourceList.body()).getJsonArray(SoileCommUtils.DATAFIELD);
			// this has the structure: [ { label : file/foldername , children: [ {} ] <optional> }];
			List<Future> fileRetrievalFutures = new LinkedList<>();
			ConcurrentLinkedDeque<FileDescriptor> resources = new ConcurrentLinkedDeque<FileDescriptor>();
			buildResourceList(resourceArray, fileRetrievalFutures, resources, Path.of(""), taskUUID, version);
			CompositeFuture.all(fileRetrievalFutures)
			.onSuccess(allFilesRetrieved -> {
				// now we got all The resource files now. 
				listPromise.complete(List.copyOf(resources));
				vertx.fileSystem().createTempFile(taskUUID+version, ".code")
				.onSuccess(codeFileName -> {
					
				});
			});
		});		
		
		return listPromise.future();
	}
	
	private void buildResourceList(JsonArray fileList, List<Future> fileRetrievalFutures, ConcurrentLinkedDeque<FileDescriptor> files, Path currentPath, String taskUUID, String version)
	{
		for(int i = 0; i < fileList.size(); ++i)
		{
			JsonObject currentFile = fileList.getJsonObject(i);
			if(currentFile.containsKey("children"))
			{
				buildResourceList(currentFile.getJsonArray("children"), fileRetrievalFutures, files, currentPath.resolve(currentFile.getString("label")), taskUUID, version);
			}
			else
			{
				Promise fileRetrivalPromise = Promise.promise();
				fileRetrievalFutures.add(fileRetrivalPromise.future());
				GitFile target = new GitFile(currentFile.getString("label"), taskManager.getGitIDForUUID(taskUUID), version); 
				dlrmgr.getElement(target)
				.onSuccess(datalakeFile -> {
					files.add(datalakeFile);
					fileRetrivalPromise.complete();
				})
				.onFailure(err -> fileRetrivalPromise.fail(err));
			}
		}	
	}
		
}
