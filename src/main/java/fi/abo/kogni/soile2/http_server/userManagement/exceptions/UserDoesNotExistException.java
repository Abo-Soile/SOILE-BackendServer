package fi.abo.kogni.soile2.http_server.userManagement.exceptions;

/**
 * Resources requested for a user that does not exist.
 * @author Thomas Pfau
 *
 */
public class UserDoesNotExistException extends Exception{	

	/**
	 * The error code associated with this Exception
	 */
	public static int ERRORCODE = 1001;
	/**
	 * Default Constructor
	 * @param username the username that doesn't exist
	 */
	public UserDoesNotExistException(String username) {
		super("User " + username + " does not exist" );
	}
}
