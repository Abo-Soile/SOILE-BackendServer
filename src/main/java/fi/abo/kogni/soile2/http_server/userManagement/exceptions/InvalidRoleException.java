package fi.abo.kogni.soile2.http_server.userManagement.exceptions;

/**
 * The provided Role was not a valid role
 * @author Thomas Pfau
 *
 */
public class InvalidRoleException extends Exception{

	/**
	 * Default Constructor
	 * @param roleString the role string that wasn't valid
	 */
	public InvalidRoleException(String roleString) {
		super(roleString + " is not a valid role");
	}
}
