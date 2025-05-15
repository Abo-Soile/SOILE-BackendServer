package fi.abo.kogni.soile2.datamanagement.git;

import fi.abo.kogni.soile2.datamanagement.utils.DataRetriever;
import fi.abo.kogni.soile2.http_server.verticles.GitManagerVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;

/**
 * This abstract class provides a general implementation of a {@link DataRetriever} for gitFiles and a target object.
 * It needs to know whether the retrieved Data is Json or String.
 * @author Thomas Pfau
 *
 * @param <T> The type of data this retriever provides.
 */
public abstract class GitDataRetriever<T> implements DataRetriever<GitFile, T> {
	
	/**
	 * The {@link EventBus} for communication
	 */
	protected EventBus eb;	
	/**
	 * Whether this retriever retrieves JSON or plain data
	 */
	protected boolean retrieveJson;
	/**
	 * Whether this retriever handles resources
	 */
	protected boolean handleResources;
	
	/**
	 * Construct a {@link DataRetriever} that expects {@link String}s in the createElement function and handles non-resource files 
	 * @param eb Eventbus to communicate with the {@link GitManagerVerticle}
	 */
	public GitDataRetriever(EventBus eb)
	{
		this(eb, false, false);				
	}
	/**
	 * Construct a {@link DataRetriever} that expects {@link JsonObject} or String based on the retrieveJson attribute in the createElement function and handles non-resource files 
	 * @param eb Eventbus to communicate with the {@link GitManagerVerticle}
	 * @param retrieveJson whether the createElement function expects {@link JsonObject}s (true) or {@link String}s (false)
	 */	
	public GitDataRetriever(EventBus eb, boolean retrieveJson)
	{
		this(eb,retrieveJson,false);
	}
	
	/**
	 * 
	 * @param eb Eventbus to communicate with the {@link GitManagerVerticle}
	 * @param retrieveJson whether the createElement function expects {@link JsonObject}s (true) or {@link String}s (false)
	 * @param handleResources whether this {@link DataRetriever} looks up resources or general git Files. 
	 */
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
	
	/**
	 * Create an Element of the indicated type from the data retrieved for the given gitFile. 
	 * @param elementData the Data (as retrieved from the {@link GitManagerVerticle} for the given {@link GitFile} The type is either String or JsonObject, depending on the settings of this {@link GitDataRetriever}
	 * @param key the GitFile that was used to retrieve the Object data.
	 * @return An Object of Type T
	 */
	public abstract T createElement(Object elementData, GitFile key);
	
}
