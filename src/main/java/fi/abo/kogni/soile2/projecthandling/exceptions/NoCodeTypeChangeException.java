package fi.abo.kogni.soile2.projecthandling.exceptions;

/**
 * Update indicated a change of the code type. This is not allowed.
 * @author Thomas Pfau
 *
 */
public class NoCodeTypeChangeException extends Exception {

	public NoCodeTypeChangeException()
	{
		super("Cannot change the code type of an existing task");
	}
}
