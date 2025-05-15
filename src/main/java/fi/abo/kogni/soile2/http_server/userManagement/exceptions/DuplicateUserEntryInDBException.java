package fi.abo.kogni.soile2.http_server.userManagement.exceptions;

/**
 * A Duplocate User was found (should never happen, as the Row should have a unique index...
 * @author Thomas Pfau
 *
 */
public class DuplicateUserEntryInDBException extends Exception{

	/**
	 * Defautl constructor
	 * @param username the username that is present in duplicate
	 */
	public DuplicateUserEntryInDBException(String username) {
		super("User " + username + " is present multiple times in the database" );
	}
	
}
