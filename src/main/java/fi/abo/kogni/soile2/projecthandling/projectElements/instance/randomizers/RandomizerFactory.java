package fi.abo.kogni.soile2.projecthandling.projectElements.instance.randomizers;

import fi.abo.kogni.soile2.projecthandling.exceptions.UnknownRandomizerException;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.Study;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.randomizers.impl.BasicRandomizer;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.randomizers.impl.BlockRandomizer;
import io.vertx.core.json.JsonObject;

public class RandomizerFactory {

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
