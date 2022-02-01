package fi.abo.kogni.soile2.http_server.userManagement.exceptions;

public class DuplicateUserEntryInDBException extends Exception{

	public DuplicateUserEntryInDBException(String username) {
		super("User " + username + " is present multiple times in the database" );
	}
	
}
