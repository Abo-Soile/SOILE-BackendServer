package fi.abo.kogni.soile2.datamanagement.git;

import fi.abo.kogni.soile2.datamanagement.utils.DataRetriever;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;

public abstract class GitDataRetriever<T> implements DataRetriever<GitFile, T> {
	
	protected EventBus eb;	
	protected boolean retrieveJson;
	protected boolean handleResources;
	public GitDataRetriever(EventBus eb)
	{
		this(eb, false, false);				
	}
	
	public GitDataRetriever(EventBus eb, boolean retrieveJson)
	{
		this(eb,retrieveJson,false);
	}
	
	public GitDataRetriever(EventBus eb, boolean retrieveJson, boolean handleResources)
	{
		this.eb = eb; 	
		this.retrieveJson = retrieveJson;
		this.handleResources = handleResources; 
	}
	
	@Override
	public Future<T> getElement(GitFile key) {
		Promise<T> projectPromise = Promise.<T>promise();
		
		if(retrieveJson)
		{	
			Future<JsonObject> gitObject;
			if(handleResources)
			{
				gitObject = eb.request("soile.git.getGitResourceContentsAsJson", key.toJson()).map(message -> { return (JsonObject) message.body();});
			}
			else
			{
				gitObject = eb.request("soile.git.getGitFileContentsAsJson", key.toJson()).map(message -> { return (JsonObject) message.body();});
			}
			gitObject.onSuccess(contents -> {
				projectPromise.complete(createElement(contents, key));
			})
			.onFailure(err -> {
				projectPromise.fail(err);
			});
		}
		else
		{
			Future<String> gitObject;
			if(handleResources)
			{
				gitObject = eb.request("soile.git.getGitResourceContents", key.toJson()).map(message -> { return (String) message.body();});
			}
			else
			{
				gitObject = eb.request("soile.git.getGitFileContents", key.toJson()).map(message -> { return (String) message.body();});
			}
			gitObject.onSuccess(contents -> {
				projectPromise.complete(createElement(contents, key));
			})
			.onFailure(err -> {
				projectPromise.fail(err);
			});
		}
		
		return projectPromise.future();
	}

	@Override
	public void getElement(GitFile key, Handler<AsyncResult<T>> handler) {
		handler.handle(getElement(key));
	}
	
	public abstract T createElement(Object elementData, GitFile key);
	
}
