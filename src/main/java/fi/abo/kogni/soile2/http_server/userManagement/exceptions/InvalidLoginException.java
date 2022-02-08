package fi.abo.kogni.soile2.http_server.userManagement.exceptions;

public class InvalidLoginException extends Exception {

	public InvalidLoginException(String username) {
		super("Invalid username or invalid password for username: " + username);
	}
}
