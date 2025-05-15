package fi.abo.kogni.soile2.projecthandling.projectElements.instance.randomizers;

import fi.abo.kogni.soile2.projecthandling.exceptions.UnknownRandomizerException;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.Study;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.randomizers.impl.BasicRandomizer;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.randomizers.impl.BlockRandomizer;
import io.vertx.core.json.JsonObject;

/**
 * Factory for Randomizer creation
 * @author Thomas Pfau
 *
 */
public class RandomizerFactory {

	/**
	 * Factory function to creat Randomizers
	 * @param randomizerData jsonData for the object
	 * @param sourceStudy The {@link Study} the randomizer will be in
	 * @return a new {@link Randomizer} 
	 * @throws UnknownRandomizerException if the Randomizer is unknown.
	 */
	public static Randomizer create(JsonObject randomizerData, Study sourceStudy) throws UnknownRandomizerException
	{
		String randomizerType = randomizerData.getString("type");
		switch(randomizerType)
		{
		case "random": return new BasicRandomizer(randomizerData, sourceStudy); 
		case "block":  return new BlockRandomizer(randomizerData, sourceStudy);
		default: throw new UnknownRandomizerException(randomizerType);
		}
	}
}
