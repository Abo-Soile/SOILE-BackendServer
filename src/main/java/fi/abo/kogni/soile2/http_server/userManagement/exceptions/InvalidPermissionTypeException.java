package fi.abo.kogni.soile2.http_server.userManagement.exceptions;

/**
 * Could not determine the type of permission, seems an invalid type was used
 * @author Thomas Pfau
 *
 */
public class InvalidPermissionTypeException extends Exception {

	public InvalidPermissionTypeException(String message)
	{
		super(message);
	}
}
