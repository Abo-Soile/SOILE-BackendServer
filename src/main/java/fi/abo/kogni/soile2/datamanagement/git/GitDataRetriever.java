package fi.abo.kogni.soile2.datamanagement.git;

import fi.abo.kogni.soile2.datamanagement.utils.DataRetriever;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;

public abstract class GitDataRetriever<T> implements DataRetriever<GitFile, T> {
	
	protected GitManager manager;	
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
		this.manager = new GitManager(eb); 	
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
				gitObject = manager.getGitResourceContentsAsJson(key);
			}
			else
			{
				gitObject = manager.getGitFileContentsAsJson(key);
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
				gitObject = manager.getGitResourceContents(key);
			}
			else
			{
				gitObject = manager.getGitFileContents(key);
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
