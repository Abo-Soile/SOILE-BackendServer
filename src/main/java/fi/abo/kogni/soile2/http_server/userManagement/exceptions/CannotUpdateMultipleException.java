package fi.abo.kogni.soile2.http_server.userManagement.exceptions;

public class CannotUpdateMultipleException extends Exception {

	private static final long serialVersionUID = 1L;

	public CannotUpdateMultipleException()
	{
		super("Cannot update permission levels for multiple elements at once.");
	}
	
}
