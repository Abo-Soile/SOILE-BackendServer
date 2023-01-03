package fi.abo.kogni.soile2.http_server.userManagement.exceptions;

public class InvalidEmailAddress extends Exception {

	public InvalidEmailAddress(String Email)
	{
		super(Email + " is not a valid email address.");
	}
}
