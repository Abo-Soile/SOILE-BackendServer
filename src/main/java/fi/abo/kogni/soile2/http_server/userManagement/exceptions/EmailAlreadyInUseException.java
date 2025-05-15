package fi.abo.kogni.soile2.http_server.userManagement.exceptions;

/**
 * The Users Email already exists.
 * @author Thomas Pfau
 *
 */
public class EmailAlreadyInUseException extends Exception {

	/**
	 * Default constructor
	 * @param Email the email already in use
	 */
	public EmailAlreadyInUseException(String Email)
	{
		super(Email + " is already in use.");
	}
}
