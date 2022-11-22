package fi.abo.kogni.soile2.projecthandling.projectElements.instance;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import fi.abo.kogni.soile2.projecthandling.participant.Participant;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl.TaskObjectInstance;
import io.vertx.core.json.JsonObject;

/**
 * This represents an instance of a Object as stored in the Underlying database. 
 * All Instances must implement a "nextTask()" method, that returns the ID of the 
 * next task that has to be run for a user.    
 * @author Thomas Pfau
 *
 */
public abstract class ElementInstanceBase implements ElementInstance {

	protected ProjectInstance sourceProject;	 
	protected JsonObject data;
	public ElementInstanceBase(JsonObject data, ProjectInstance source)
	{
		this.data = data;
		this.sourceProject = source; 
	}
	
	@Override
	@JsonProperty("UUID")
	public String getUUID()
	{
		return data.getString("UUID");
	}	

	@Override
	public void setUUID(String uuid)
	{
		data.put("UUID", uuid);
	}


	/**
	 * Can be either be a commit ID or a Tag
	 * @return
	 */
	@Override
	@JsonProperty("version")
	public String getVersion()
	{
		return data.getString("version");
	}

	/**
	 * Can be either be a commit ID or a Tag
	 */
	@Override
	public void setVersion(String version)
	{
		data.put("version", version);
	}


	@Override
	@JsonProperty("name")
	public String getName()
	{
		return data.getString("name");
	}
	/**
	 * A human readable name for a 
	 * @param name
	 */
	@Override
	public void setName(String name)
	{
		data.put("name", name);
	}

	@Override
	public String toString()
	{
		return data.encodePrettily();
	}

	@Override
	public String toString(List<String> fields)
	{
		JsonObject restricted = new JsonObject();
		for(String field : fields)
		{
			restricted.put(field,data.getValue(field));
		}
		return restricted.encodePrettily();
	}

	@Override
	@JsonProperty("private")
	public Boolean getPrivate() {
		return data.getBoolean("private");
	}
	@Override
	public void setPrivate(Boolean _private) {
		data.put("private", _private);
	}
	
	@Override
	@JsonProperty("next")
	public String getNext() {
		return data.getString("next");
	}
	
	@Override
	public void setNext(String next) {
		data.put("next", next);
	}
	
	@Override
	@JsonProperty("instanceID")
	public String getInstanceID() {
		return data.getString("instanceID");
	}
	
	@Override
	public void setInstanceID(String instanceID) {
		data.put("instanceID", instanceID);
	}
	
	/**
	 * Return the ID of the next {@link TaskObjectInstance} for the given user based on the assumption, that 
	 * has finished the task at its `getProjectPosition()`. This should check, whether    
	 * @param user
	 * @return
	 */
	@Override
	public abstract String nextTask(Participant user);
	
}
