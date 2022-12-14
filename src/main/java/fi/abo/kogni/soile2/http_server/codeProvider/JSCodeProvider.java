package fi.abo.kogni.soile2.http_server.codeProvider;

import fi.abo.kogni.soile2.datamanagement.git.GitFile;
import fi.abo.kogni.soile2.datamanagement.git.GitManager;
import io.vertx.core.Future;
import io.vertx.core.Promise;

public class JSCodeProvider implements CodeProvider {

	GitManager manager;
	
	public JSCodeProvider(GitManager manager)
	{
		this.manager = manager;
	}
	@Override
	public Future<String> getCode(GitFile file) {
		Promise<String> codePromise = Promise.promise();
				
		manager.getGitFileContentsAsJson(file)
		.onSuccess(objectJson -> {
				codePromise.complete(objectJson.getString("code"));
		})
		.onFailure(err -> codePromise.fail(err));
		return codePromise.future();
	}

	@Override
	public void cleanup() {
		return;
	}

	@Override
	public Future<String> compileCode(String code) {
		return Future.succeededFuture(code);
	}

}
