package fi.abo.kogni.soile2.http_server.codeProvider;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.abo.kogni.soile2.datamanagement.git.GitFile;
import fi.abo.kogni.soile2.datamanagement.utils.DataRetriever;
import fi.abo.kogni.soile2.http_server.codeProvider.exceptions.CompilationException;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;

public class CompiledCodeRetriever implements DataRetriever<GitFile, String> {

	EventBus eb;
	String targetAddress;		
	static final Logger LOGGER = LogManager.getLogger(CompiledCodeRetriever.class);

	public CompiledCodeRetriever(EventBus eb, String targetAddress) {
		super();
		this.eb = eb;
		this.targetAddress = targetAddress;
	}

	@Override
	public Future<String> getElement(GitFile key) {
		Promise<String> codePromise = Promise.promise();
		eb.request("soile.git.getGitFileContentsAsJson", key.toJson())
		.onSuccess(reply -> {
			JsonObject objectJson = (JsonObject) reply.body();
			compileCode(objectJson.getString("code"))
			.onSuccess(compiledCode-> {
				codePromise.complete(compiledCode);
			})
			.onFailure(err -> codePromise.fail(err));
		})
		.onFailure(err -> codePromise.fail(err));
		
		return codePromise.future();
		
	}

	@Override
	public void getElement(GitFile key, Handler<AsyncResult<String>> handler) {
		handler.handle(getElement(key));		
	}		
	
	public Future<String> compileCode(String code)
	{
		if(code == null)
		{
			return Future.failedFuture(new CompilationException("There was no code in the requested resource"));
		}
		Promise<String> codePromise = Promise.promise();
		JsonObject message = new JsonObject().put("code",code);
		LOGGER.debug("Requesting response from address: " + targetAddress);
		eb.request(targetAddress, message)
		.onSuccess(res -> {
			JsonObject response = (JsonObject) res.body();
			LOGGER.debug("Response was:\n" + response.encodePrettily());
			if(response.containsKey("errors"))
			{
				codePromise.fail(new CompilationException(response.getString("errors")));
			}
			else
			{
				codePromise.complete(response.getString("code"));
			}
		})
		.onFailure(err -> codePromise.fail(err));
		return codePromise.future();
	}
	
}
