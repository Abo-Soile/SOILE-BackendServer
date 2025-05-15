package fi.abo.kogni.soile2.http_server.codeProvider.exceptions;

/**
 * Any problems that happened during compilation
 * @author Thomas Pfau
 *
 */
public class CompilationException extends Exception{

	private static final long serialVersionUID = 1L;

	/**
	 * Default constructor
	 * @param reason reason for the exception
	 */
	public CompilationException(String reason)
	{
		super(reason);
	}
}
