package fi.abo.kogni.soile2.project.itemManagement;

import fi.aalto.scicomp.gitFs.gitProviderVerticle;
import fi.abo.kogni.soile2.utils.DataRetriever;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;

public abstract class ItemRetriever<T> implements DataRetriever<ResourceVersion, T> {
	
	private EventBus eb;	
	public ItemRetriever(EventBus eb)
	{
		this.eb = eb; 	
	}
	
	
	@Override
	public Future<T> getElement(ResourceVersion key) {
		Promise<T> projectPromise = Promise.<T>promise();
		JsonObject fetchCommand = gitProviderVerticle.createGetCommand(key.getElementID().toString(), key.getVersion(), key.getFilename());
		eb.request(SoileConfigLoader.getServerProperty("gitVerticleAddress"),fetchCommand).onSuccess( res -> {
			
			T retrievedElement = createElement(((JsonObject)res.body()).getString(gitProviderVerticle.DATAFIELD));
			projectPromise.complete(retrievedElement);
		}).onFailure(err -> 
		{
			projectPromise.fail(err.getCause());
		});
		
		
		return projectPromise.future();
	}

	@Override
	public void getElement(ResourceVersion key, Handler<AsyncResult<T>> handler) {
		handler.handle(getElement(key));
	}
	
	public abstract T createElement(String elementData);
	
}
