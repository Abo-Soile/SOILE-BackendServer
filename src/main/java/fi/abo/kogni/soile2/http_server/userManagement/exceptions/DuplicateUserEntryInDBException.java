package fi.abo.kogni.soile2.http_server.userManagement.exceptions;

/**
 * A Duplocate User was found (should never happen, as the Row should have a unique index...
 * @author Thomas Pfau
 *
 */
public class DuplicateUserEntryInDBException extends Exception{

	public DuplicateUserEntryInDBException(String username) {
		super("User " + username + " is present multiple times in the database" );
	}
	
}
