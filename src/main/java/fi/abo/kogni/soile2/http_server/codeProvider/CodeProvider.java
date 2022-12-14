package fi.abo.kogni.soile2.http_server.codeProvider;

import fi.abo.kogni.soile2.datamanagement.git.GitFile;
import io.vertx.core.Future;

public interface CodeProvider {

	Future<String> getCode(GitFile file);

	void cleanup();

	Future<String> compileCode(String code);

}