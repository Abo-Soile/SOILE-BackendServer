package fi.abo.kogni.soile2.http_server.userManagement.exceptions;

/**
 * A Class indicating a problem updating multiple things at once
 * @author Thomas Pfau
 *
 */
public class CannotUpdateMultipleException extends Exception {

	private static final long serialVersionUID = 1L;

	/**
	 * Default constructor
	 */
	public CannotUpdateMultipleException()
	{
		super("Cannot update permission levels for multiple elements at once.");
	}
	
}
