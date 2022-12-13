package fi.abo.kogni.soile2.projecthandling.projectElements;

import com.fasterxml.jackson.annotation.JsonProperty;

import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.TargetElementType;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

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

	@JsonProperty("codetype")
	public String getCodetype() {
		return data.getString("codetype");
	}
	public void setCodetype(String codetype) {
		data.put("codetype", codetype);
	}

	/**
	 * This represents ALL resources used in any version of the task. 
	 * @return
	 */
	@JsonProperty("resources")
	public JsonArray getResources() {
		return data.getJsonArray("resources");
	}
	public void setResources(JsonArray resources) {
		data.put("resources", resources);
	}

	public void addResource(String filename) {		
		data.getJsonArray("resources").add(filename);
	}
	@Override
	public void loadfromJson(JsonObject json)
	{		
		super.loadfromJson(json);
		setResources(json.getJsonArray("resources",new JsonArray()));
		setCodetype(json.getString("codetype", "javascript"));
	}
	@Override
	public JsonObject toJson(boolean provideUUID)
	{		
		return super.toJson(provideUUID).put("resources", getResources()).put("codetype", getCodetype()); 
	}
	@Override
	public JsonObject getUpdates()
	{
			JsonObject updateVersions = new JsonObject().put("versions", new JsonObject().put("$each", getVersions()));
			JsonObject updateTags = new JsonObject().put("tags", new JsonObject().put("$each", getTags()));
			JsonObject updateResources = new JsonObject().put("resources", new JsonObject().put("$each", getResources()));
			JsonObject updates = new JsonObject().put("$addToSet", new JsonObject().mergeIn(updateVersions).mergeIn(updateResources).mergeIn(updateTags))
												 .put("$set", new JsonObject().put("private", getPrivate()).put("name", getName()));
			System.out.println(updates.encodePrettily());
			return updates;
	}

	@Override
	public String getTypeID() {
		// TODO Auto-generated method stub
		return typeID;
	}	
	@Override
	public TargetElementType getElementType() {
		// TODO Auto-generated method stub
		return TargetElementType.TASK;
	}
}
