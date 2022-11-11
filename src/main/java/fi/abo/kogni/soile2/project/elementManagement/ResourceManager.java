package fi.abo.kogni.soile2.project.elementManagement;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import fi.aalto.scicomp.gitFs.gitProviderVerticle;
import fi.abo.kogni.soile2.project.GitFile;
import fi.abo.kogni.soile2.project.GitManager;
import fi.abo.kogni.soile2.utils.DataLakeFile;
import fi.abo.kogni.soile2.utils.DataRetriever;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.impl.MessageImpl;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.FileUpload;
public class ResourceManager implements DataRetriever<GitFile,DataLakeFile> {

	private String dataLakeBaseFolder;
	private GitManager manager;
	
	public ResourceManager(EventBus bus)
	{
		manager = new GitManager(bus);
		dataLakeBaseFolder = SoileConfigLoader.getServerProperty("soileGitDataLakeFolder");
	}
	
	@Override
	public Future<DataLakeFile> getElement(GitFile key) {
		Promise<DataLakeFile> filePromise = Promise.<DataLakeFile>promise();
		JsonObject gitRequest = gitProviderVerticle.createGetCommand(key.getRepoID(), key.getRepoVersion(), key.getFileName());
		manager.getGitResourceContentsAsJson(key).onSuccess(json -> 
		{
			DataLakeFile target = new DataLakeFile(dataLakeBaseFolder + File.separator + key.getRepoID() + File.separator + json.getString("targetFile"), json.getString("filename"));
			filePromise.complete(target);
		}).onFailure(fail ->{
			filePromise.fail(fail.getMessage());
		});							
		return filePromise.future();
	}

	@Override
	public void getElement(GitFile key, Handler<AsyncResult<DataLakeFile>> handler) {
		handler.handle(getElement(key));
	}			
	
	/**
	 * Write an element to the git Repository and return the new Version.  
	 *  
	 * @return
	 */
	public Future<String> writeElement(GitFile target, FileUpload targetUpload)	
	{		
		String gitFileName = target.getFileName() == null ? targetUpload.fileName() : target.getFileName();
		String targetFileName = Paths.get(targetUpload.uploadedFileName()).getFileName().toString();
		
		JsonObject fileContents = new JsonObject().put("filename", gitFileName)
												  .put("targetFile", targetFileName);		
		return manager.writeGitResourceFile(target, fileContents);		
	}
	
	/**
	 * Test, whether the git repository for a specific element (Task/Experiment/Project) exists
	 * @param elementID the UUID of the element
	 * @return A future whether the element exists
	 */
	public Future<Boolean> existElementRepo(String elementID)
	{
		return manager.doesRepoExist(elementID);
	}
	
}
