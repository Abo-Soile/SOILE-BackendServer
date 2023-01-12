package fi.abo.kogni.soile2.projecthandling.exceptions;

public class NoCodeTypeChangeException extends Exception {

	public NoCodeTypeChangeException()
	{
		super("Cannot change the code type of an existing task");
	}
}
