package fi.abo.kogni.soile2.projecthandling.exceptions;

/**
 * An Exception indicating a request for an unknown randomizer
 * @author Thomas Pfau
 *
 */
public class UnknownRandomizerException extends Exception {

	/**
	 * Default constructor
	 * @param randomizerName the name of the expected (but unknown) randomizer
	 */
	public UnknownRandomizerException(String randomizerName)
	{
		super("Cannot create a randomizer of type " +  randomizerName + ". This randomizer type is unknown.");
	}
}
