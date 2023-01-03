package fi.abo.kogni.soile2.http_server.userManagement.exceptions;

public class InvalidRoleException extends Exception{

	public InvalidRoleException(String roleString) {
		// TODO Auto-generated constructor stub
		super(roleString + " is not a valid role");
	}
}
