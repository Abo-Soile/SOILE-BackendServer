package fi.abo.kogni.soile2.projecthandling.exceptions;

/**
 * The position requested for the participant in the project does not match the expected position.
 * @author Thomas Pfau
 *
 */
public class FileContentInvalidException extends Exception {
	
	public FileContentInvalidException()
	{
		super("Could not retrieve code and configuration from task bundle");
	}
	
}
