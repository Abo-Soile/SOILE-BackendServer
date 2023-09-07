package fi.abo.kogni.soile2.projecthandling.projectElements.instance;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.abo.kogni.soile2.projecthandling.participant.Participant;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl.FieldSpecifications;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl.TaskObjectInstance;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

/**
 * This represents an instance of a Object as stored in the Underlying database. 
 * All Instances must implement a "nextTask()" method, that returns the ID of the 
 * next task that has to be run for a user.    
 * @author Thomas Pfau
 *
 */
public abstract class ElementInstanceBase implements ElementInstance {

	protected Study sourceStudy;	 
	protected JsonObject data;
	  static final Logger LOGGER = LogManager.getLogger(ElementInstanceBase.class);

	public ElementInstanceBase(JsonObject data, Study source)
	{
		setupFieldsAccordingToSpec();
		this.data.mergeIn(data);
		this.sourceStudy = source; 
	}
	
	@Override
	public String getUUID()
	{
		return data.getString("UUID");
	}	

	@Override
	public void setUUID(String UUID)
	{
		data.put("UUID", UUID);
	}


	/**
	 * Can be either be a commit ID or a Tag
	 * @return
	 */
	@Override
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
	public Boolean getPrivate() {
		return data.getBoolean("private");
	}
	@Override
	public void setPrivate(Boolean _private) {
		data.put("private", _private);
	}
	
	@Override
	public String getNext() {
		return data.getString("next");
	}
	
	@Override
	public void setNext(String next) {
		data.put("next", next);
	}
	
	@Override
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
	public abstract Future<String> nextTask(Participant user);

	/**
	 * Checks, whether "next" is possible and if not return <code>null</code>.  
	 * @param user The user for which to get the next element
	 * @param next the next element.
	 * @return The instanceID of the next {@link TaskObjectInstance} or null if there is no next. 
	 */
	protected Future<String> getNextIfThereIsOne(Participant user, String next)
	{
		ElementInstance element = sourceStudy.getElement(next);
		// if we happen to have a no next in the source project, we are at the end.
		if(element == null)
		{
			return Future.succeededFuture(null);
		}
		else
		{
			return element.nextTask(user);
		}
	}	
	private void setupFieldsAccordingToSpec()
	{
		this.data = new JsonObject();
		FieldSpecifications specs = getElementSpecifications(); 
		for(String field : specs.getFields())
		{
			this.data.put(field, specs.getDefaultForField(field));
		}
	}
}
