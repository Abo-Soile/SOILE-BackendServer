package fi.abo.kogni.soile2.projecthandling.exceptions;

public class InvalidPositionException extends Exception {
	
	public InvalidPositionException(String expected, String obtained)
	{
		super("Position expected was " + expected +  " while provided position was " + obtained);
	}
	
}
