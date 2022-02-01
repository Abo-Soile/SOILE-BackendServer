package fi.abo.kogni.soile2.http_server.userManagement.exceptions;

public class UserDoesNotExistException extends Exception{	

	public UserDoesNotExistException(String username) {
		super("User " + username + " does not exist" );
	}
}
