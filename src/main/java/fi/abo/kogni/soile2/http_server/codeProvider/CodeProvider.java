package fi.abo.kogni.soile2.http_server.codeProvider;

import fi.abo.kogni.soile2.datamanagement.git.GitFile;
import io.vertx.core.Future;

/**
 * Interface providing code for a particular Task.
 * Implementing classes are assumed to keep the code (and compiled code) in memory for some time.
 * @author Thomas Pfau
 *
 */
public interface CodeProvider {

	/**
	 * Get the original Code associated with the given {@link GitFile}.
	 * @param file
	 * @return
	 */
	Future<String> getCode(GitFile file);

	/**
	 * Compile the given code 
	 * @param code
	 * @return A {@link Future} of the compild code as string
	 */
	Future<String> compileCode(String code);

	/**
	 * Clean up code that has been compiled/retrieved as to not needing to store it any longer. 
	 */
	void cleanUp();
}