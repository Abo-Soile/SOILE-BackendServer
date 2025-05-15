package fi.abo.kogni.soile2.projecthandling.exceptions;

/**
 * General Exception that can be thrown if an object does not exist. 
 * @author Thomas Pfau
 *
 */
public class ObjectDoesNotExist extends Exception {

	/**
	 * Default constructor
	 * @param ID the ID that doesn't exist
	 */
	public ObjectDoesNotExist(String ID)
	{
		super("Object with the id " + ID + " does not exist");
	}
}
