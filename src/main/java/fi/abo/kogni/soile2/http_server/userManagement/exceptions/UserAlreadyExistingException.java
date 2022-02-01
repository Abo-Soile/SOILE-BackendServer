package fi.abo.kogni.soile2.http_server.userManagement.exceptions;

public class UserAlreadyExistingException extends Exception {
	
	public UserAlreadyExistingException(String username)
	{
		super("User " + username + " already present" );
	}
	

}
