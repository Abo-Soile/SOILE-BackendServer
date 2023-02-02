package fi.abo.kogni.soile2.datamanagement.git;

import fi.aalto.scicomp.gitFs.gitProviderVerticle;
import fi.abo.kogni.soile2.datamanagement.utils.DataRetriever;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class GitFileListRetriever implements DataRetriever<GitElement, JsonArray> {

	private EventBus eb;
	
	public GitFileListRetriever(EventBus eb)
	{
		this.eb = eb;
	}
	
	@Override
	public Future<JsonArray> getElement(GitElement key) {

		Promise<JsonArray> resourceListPromise = Promise.promise();
		JsonObject getFilesCommand = gitProviderVerticle.createCommandForRepoAtVersion(key.getRepoID(), key.getRepoVersion())
				 .put(gitProviderVerticle.COMMANDFIELD, gitProviderVerticle.LIST_FILES_COMMAND);
		eb.request(SoileConfigLoader.getServerProperty("gitVerticleAddress"), getFilesCommand).onSuccess(fileData ->{
			
		});
		return resourceListPromise.future();
	}

	@Override
	public void getElement(GitElement key, Handler<AsyncResult<JsonArray>> handler) {
		// TODO Auto-generated method stub
		
	}

}
