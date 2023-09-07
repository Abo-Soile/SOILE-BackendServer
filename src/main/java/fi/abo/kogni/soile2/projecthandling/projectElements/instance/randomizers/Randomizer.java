package fi.abo.kogni.soile2.projecthandling.projectElements.instance.randomizers;

import fi.abo.kogni.soile2.projecthandling.projectElements.instance.ElementInstanceBase;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.Study;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl.FieldSpecification;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl.FieldSpecifications;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * A Study level filter is a filter that assigns a participant to a specific group for the study.
 * This is achieved by querying data for the study and assigning a the next task based on that data
 * @author Thomas Pfau
 *
 */
public abstract class Randomizer extends ElementInstanceBase{

	
	@Override
	public FieldSpecifications getElementSpecifications() {
		// TODO Auto-generated method stub
		return getFieldSpecifications();		
	}
	
	public Randomizer(JsonObject data, Study source)
	{
		super(data,source);
	}	
	
	public static FieldSpecifications getFieldSpecifications() {
		// TODO Auto-generated method stub
		return new FieldSpecifications().put(new FieldSpecification("instanceID", String.class, String::new, false))																																								
				.put(new FieldSpecification("options", JsonArray.class, JsonArray::new, false))
				.put(new FieldSpecification("settings", JsonArray.class, JsonArray::new, true))
				.put(new FieldSpecification("type", String.class, () -> "random", false))
				.put(new FieldSpecification("position", JsonObject.class, () -> new JsonObject().put("x", 0).put("y", 0), true));		
	}
}
