package fi.abo.kogni.soile2.project.elements.impl;

import com.fasterxml.jackson.annotation.JsonProperty;

import fi.abo.kogni.soile2.project.elements.ProjectDataBaseObjectInstance;
import fi.abo.kogni.soile2.project.instance.ProjectInstance;
import fi.abo.kogni.soile2.project.participant.Participant;
import io.vertx.core.json.JsonObject;

/**
 * This represents an instance of a Object as stored in the Underlying database. 
 * All Instances must implement a "nextTask()" method, that returns the ID of the 
 * next task that has to be run for a user.    
 * @author Thomas Pfau
 *
 */
public abstract class ProjectDataBaseObjectInstanceImpl extends ProjectDataBaseObjectImpl implements ProjectDataBaseObjectInstance {

	ProjectInstance sourceProject;	 
		
	public ProjectDataBaseObjectInstanceImpl(JsonObject data, ProjectInstance source)
	{
		super(data);		
		this.sourceProject = source; 
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
