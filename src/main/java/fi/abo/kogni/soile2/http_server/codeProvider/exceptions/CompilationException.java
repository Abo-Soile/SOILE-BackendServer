package fi.abo.kogni.soile2.http_server.codeProvider.exceptions;

/**
 * Any problems that happened during compilation
 * @author Thomas Pfau
 *
 */
public class CompilationException extends Exception{

	public CompilationException(String reason)
	{
		super(reason);
	}
}
