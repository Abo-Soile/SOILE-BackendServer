package fi.abo.kogni.soile2.http_server.userManagement.exceptions;

/**
 * The username already exists, and cannot be created a second time.
 * @author Thomas Pfau
 *
 */
public class UserAlreadyExistingException extends Exception {
	
	public UserAlreadyExistingException(String username)
	{
		super("User " + username + " already present" );
	}
	

}
