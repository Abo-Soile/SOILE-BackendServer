package fi.abo.kogni.soile2.projecthandling.exceptions;

/**
 * The position requested for the participant in the project does not match the expected position.
 * @author Thomas Pfau
 *
 */
public class InvalidPositionException extends Exception {
	
	public InvalidPositionException(String expected, String obtained)
	{
		super("Position expected was " + expected +  " while provided position was " + obtained);
	}
	
}
