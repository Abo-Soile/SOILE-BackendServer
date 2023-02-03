package fi.abo.kogni.soile2.http_server.userManagement.exceptions;

/**
 * The login was invalid (either Username or password or both)
 * @author Thomas Pfau
 *
 */
public class InvalidLoginException extends Exception {

	public InvalidLoginException(String username) {
		super("Invalid username or invalid password for username: " + username);
	}
}
