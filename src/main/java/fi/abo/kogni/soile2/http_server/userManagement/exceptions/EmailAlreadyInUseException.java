package fi.abo.kogni.soile2.http_server.userManagement.exceptions;

/**
 * The Users Email already exists.
 * @author Thomas Pfau
 *
 */
public class EmailAlreadyInUseException extends Exception {

	public EmailAlreadyInUseException(String Email)
	{
		super(Email + " is already in use.");
	}
}
