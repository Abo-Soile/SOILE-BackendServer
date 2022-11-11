package fi.abo.kogni.soile2.project.elementManagement;

import fi.aalto.scicomp.gitFs.gitProviderVerticle;
import fi.abo.kogni.soile2.project.GitFile;
import fi.abo.kogni.soile2.project.GitManager;
import fi.abo.kogni.soile2.utils.DataRetriever;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
public class ObjectManager implements DataRetriever<GitFile,JsonObject> {

	private String dataLakeBaseFolder;
	private GitManager manager;
	
	public ObjectManager(EventBus bus)
	{
		manager = new GitManager(bus);
		dataLakeBaseFolder = SoileConfigLoader.getServerProperty("soileGitDataLakeFolder");
	}
	
	@Override
	public Future<JsonObject> getElement(GitFile key) {
		Promise<JsonObject> filePromise = Promise.<JsonObject>promise();
		JsonObject gitRequest = gitProviderVerticle.createGetCommand(key.getRepoID(), key.getRepoVersion(), key.getFileName());
		manager.getGitFileContentsAsJson(key).onSuccess(json -> 
		{			
			filePromise.complete(json);
		}).onFailure(fail ->{
			filePromise.fail(fail.getMessage());
		});							
		return filePromise.future();
	}

	@Override
	public void getElement(GitFile key, Handler<AsyncResult<JsonObject>> handler) {
		handler.handle(getElement(key));
	}			
	
	/**
	 * Write an element to the git Repository and return the new Version.  
	 * @param target the target file containing the git file information (i.e. repo, filename and version to build on ) 
	 * @param targetUpload the Upload containing the information on where the target file (linked by the new github file) is stored. 
	 * @return a future of the new git revision after this change
	 */
	 
	
	public Future<String> writeElement(GitFile target, JsonObject content)	
	{		
		String gitFileName = target.getFileName();		
		
		return manager.writeGitFile(target, content);		
	}
}
