package fi.abo.kogni.soile2.projecthandling.exceptions;

public class NoNameChangeException extends Exception {

	public NoNameChangeException()
	{
		super("Cannot change the name of an existing object. Names have to be stable");
	}
}
