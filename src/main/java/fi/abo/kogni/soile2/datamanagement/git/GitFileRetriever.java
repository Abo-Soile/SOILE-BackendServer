package fi.abo.kogni.soile2.datamanagement.git;

import java.util.function.Function;

import fi.abo.kogni.soile2.datamanagement.utils.DataRetriever;
import fi.abo.kogni.soile2.datamanagement.utils.TimeStampedMap;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;

/**
 * Utility Class that allows a function that takes a gitFile to be used as a DataRetriever for a {@link TimeStampedMap}
 * @author Thomas Pfau
 *
 * @param <T>
 */
public class GitFileRetriever<T> implements DataRetriever<GitFile, T> {

	private Function<GitFile, Future<T>> retrievalFunction;
	
	/**
	 * A Cinstructor with the function to be used.
	 * @param retrievalFunction
	 */
	public GitFileRetriever(Function<GitFile, Future<T>> retrievalFunction)
	{
		this.retrievalFunction = retrievalFunction;
	}
	
	@Override
	public Future<T> getElement(GitFile key) {
		// TODO Auto-generated method stub
		return retrievalFunction.apply(key);
	}

	@Override
	public void getElement(GitFile key, Handler<AsyncResult<T>> handler) {
		handler.handle(getElement(key));
		
	}

}
