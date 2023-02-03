package fi.abo.kogni.soile2.projecthandling.exceptions;

/**
 * The shortcut that was assigned to this project instance already exists. Shortcuts have to be unique
 * @author Thomas Pfau
 *
 */
public class ShortCutExistsException extends Exception {
	
	public ShortCutExistsException(String shortcut)
	{
		super("The provided shortcut: " + shortcut + "already exists");
	}
	
}
