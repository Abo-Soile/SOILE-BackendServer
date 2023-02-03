package fi.abo.kogni.soile2.http_server.codeProvider;

import fi.abo.kogni.soile2.datamanagement.git.GitFile;
import fi.abo.kogni.soile2.datamanagement.utils.TimeStampedMap;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;

/**
 * A Code provider that provides Javascript code. 
 * Will not be pre-compiled.
 * TODO: Check whether this should also keep code loaded, or whether the current way is quick enough
 * @author Thomas Pfau
 *
 */
public class JSCodeProvider implements CodeProvider {

	EventBus eb;
	TimeStampedMap<GitFile, String> codeMap;
	public JSCodeProvider(EventBus eb)
	{
		codeMap = new TimeStampedMap<>(this::getCodeFromGit, 3600*2);
		this.eb = eb;
	}
	@Override
	public Future<String> getCode(GitFile file) {
		return codeMap.getData(file);
	}

	private Future<String> getCodeFromGit(GitFile file)
	{
		Promise<String> codePromise = Promise.promise();

		eb.request("soile.git.getGitFileContentsAsJson", file.toJson())
		.onSuccess(reply -> {
			JsonObject objectJson = (JsonObject) reply.body();
			codePromise.complete(objectJson.getString("code"));
		})
		.onFailure(err -> codePromise.fail(err));
		return codePromise.future();
	}
	
	@Override
	public void cleanUp() {
		return;
	}

	@Override
	public Future<String> compileCode(String code) {
		return Future.succeededFuture(code);
	}

}
