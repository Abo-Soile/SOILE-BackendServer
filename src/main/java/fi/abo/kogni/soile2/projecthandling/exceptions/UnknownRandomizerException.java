package fi.abo.kogni.soile2.projecthandling.exceptions;

public class UnknownRandomizerException extends Exception {

	public UnknownRandomizerException(String randomizerName)
	{
		super("Cannot create a randomizer of type " +  randomizerName + ". This randomizer type is unknown.");
	}
}
