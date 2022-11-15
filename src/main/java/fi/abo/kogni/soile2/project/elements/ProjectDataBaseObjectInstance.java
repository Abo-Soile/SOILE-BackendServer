package fi.abo.kogni.soile2.project.elements;

import com.fasterxml.jackson.annotation.JsonProperty;

import fi.abo.kogni.soile2.project.elements.impl.TaskObjectInstance;
import fi.abo.kogni.soile2.project.participant.Participant;

public interface ProjectDataBaseObjectInstance {
	
	@JsonProperty("next")
	String getNext();

	@JsonProperty("next")
	void setNext(String next);

	@JsonProperty("instanceID")
	String getInstanceID();

	@JsonProperty("instanceID")
	void setInstanceID(String instanceID);

	/**
	 * Return the ID of the next {@link TaskObjectInstance} for the given user based on the assumption, that 
	 * has finished the task at its `getProjectPosition()`. This should check, whether    
	 * @param user
	 * @return
	 */
	String nextTask(Participant user);

}