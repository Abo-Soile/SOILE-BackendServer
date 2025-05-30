package fi.abo.kogni.soile2.http_server.userManagement.exceptions;

/**
 * The provided email is invalid (i.e. wrong format)
 * @author Thomas Pfau
 *
 */
public class InvalidEmailAddress extends Exception {

	/**
	 * Default Constructor
	 * @param Email the email that's already in use
	 */
	public InvalidEmailAddress(String Email)
	{
		super(Email + " is not a valid email address.");
	}
}
