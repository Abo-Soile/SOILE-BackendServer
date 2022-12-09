package fi.abo.kogni.soile2.projecthandling.projectElements.instance;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import fi.abo.kogni.soile2.projecthandling.participant.Participant;
import fi.abo.kogni.soile2.projecthandling.participant.DataParticipant;
import fi.abo.kogni.soile2.projecthandling.projectElements.ElementBase;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl.TaskObjectInstance;
import io.vertx.core.Future;

public interface ElementInstance {

	@JsonProperty("UUID")
	String getUUID();

	@JsonProperty("UUID")
	void setUUID(String uuid);

	/**
	 * Can be either be a commit ID or a Tag
	 * @return
	 */
	@JsonProperty("version")
	String getVersion();

	/**
	 * Can be either be a commit ID or a Tag
	 */
	@JsonProperty("version")
	void setVersion(String version);

	@JsonProperty("name")
	String getName();

	/**
	 * A human readable name for a 
	 * @param name
	 */
	@JsonProperty("name")
	void setName(String name);

	String toString();

	String toString(List<String> fields);
	@JsonProperty("private")
	Boolean getPrivate();
	@JsonProperty("private")
	void setPrivate(Boolean _private);
	
	@JsonProperty("next")
	String getNext();

	@JsonProperty("next")
	void setNext(String next);

	@JsonProperty("instanceID")
	String getInstanceID();

	@JsonProperty("instanceID")
	void setInstanceID(String instanceID);

	/**
	 * Return the ID of the next {@link TaskObjectInstance} for the given user based on the assumption, 
	 * that the user has finished the task at its `getProjectPosition()`. 
	 * @param user
	 * @return
	 */
	String nextTask(Participant user);

}