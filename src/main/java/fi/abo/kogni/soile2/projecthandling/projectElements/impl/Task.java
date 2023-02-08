package fi.abo.kogni.soile2.projecthandling.projectElements.impl;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.TargetElementType;
import fi.abo.kogni.soile2.projecthandling.projectElements.ElementBase;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Class representing a Task in the Database.
 * @author Thomas Pfau
 *
 */
public class Task extends ElementBase {

	public static String typeID = "T";
	public Task()
	{
		this(new JsonObject());
	}
	
	public Task(JsonObject data)
	{		
		super(data, SoileConfigLoader.getdbProperty("taskCollection"));
		if(data.getJsonArray("resources") == null)
		{
			data.put("resources", new JsonArray());
		}
	}

	/**
	 * Add a specific resource
	 * @param filename
	 */
	public void addResource(String filename) {		
		data.getJsonArray("resources").add(filename);
	}
	@Override
	public void loadfromJson(JsonObject json)
	{		
		super.loadfromJson(json);
	}
	@Override
	public JsonObject toJson(boolean provideUUID)
	{		
		return super.toJson(provideUUID); 
	}
	@Override
	public JsonObject getUpdates()
	{
			JsonObject updateVersions = new JsonObject().put("versions", new JsonObject().put("$each", getVersions()));
			JsonObject updateTags = new JsonObject().put("tags", new JsonObject().put("$each", getTags()));
			JsonObject updates = new JsonObject().put("$addToSet", new JsonObject().mergeIn(updateVersions).mergeIn(updateTags));
			return updates;
	}

	@Override
	public String getTypeID() {
		return typeID;
	}	
	@Override
	public TargetElementType getElementType() {
		return TargetElementType.TASK;
	}
	
}
