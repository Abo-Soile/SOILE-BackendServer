package fi.abo.kogni.soile2.projecthandling.exceptions;

public class ShortCutExistsException extends Exception {
	
	public ShortCutExistsException(String shortcut)
	{
		super("The provided shortcut: " + shortcut + "already exists");
	}
	
}
