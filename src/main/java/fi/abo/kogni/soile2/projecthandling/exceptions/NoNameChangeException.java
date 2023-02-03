package fi.abo.kogni.soile2.projecthandling.exceptions;

/**
 * Names of elements cannot be changed.
 * @author Thomas Pfau
 *
 */
public class NoNameChangeException extends Exception {

	public NoNameChangeException()
	{
		super("Cannot change the name of an existing object. Names have to be stable");
	}
}
