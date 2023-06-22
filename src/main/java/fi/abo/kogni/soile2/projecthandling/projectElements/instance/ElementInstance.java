package fi.abo.kogni.soile2.projecthandling.projectElements.instance;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import fi.abo.kogni.soile2.projecthandling.participant.Participant;
import fi.abo.kogni.soile2.projecthandling.projectElements.Element;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl.ExperimentObjectInstance;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl.FieldSpecifications;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl.TaskObjectInstance;

/**
 * Representation of an element in either a {@link Study} OR a {@link ExperimentObjectInstance}.  
 * @author Thomas Pfau
 *
 */
public interface ElementInstance {

	/**
	 * Get the UUID of the underlying object -> this refers to the underlying {@link Element} not this instance. 
	 * @return
	 */
	@JsonProperty("UUID")
	String getUUID();

	/**
	 * Set the UUID of the underlying object -> this refers to the underlying {@link Element} not this instance. 
	 * @return
	 */
	@JsonProperty("UUID")
	void setUUID(String UUID);

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

	/**
	 * Get the Name of this Instance
	 * @return
	 */
	@JsonProperty("name")
	String getName();

	/**
	 * A human readable name for a 
	 * @param name
	 */
	@JsonProperty("name")
	void setName(String name);

	/**
	 * Will normally be the Json Representation.
	 * @return
	 */
	@Override
	String toString();

	/**
	 * like toString() but only using the fields indicated
	 * @param fields
	 * @return
	 */
	String toString(List<String> fields);
	
	/**
	 * TODO: Check if this is actually necessary... (probably not)
	 * @return
	 */
	@JsonProperty("private")
	Boolean getPrivate();
	@JsonProperty("private")
	void setPrivate(Boolean _private);
	
	/**
	 * Get the instanceID of the next Element after this. 
	 * @return
	 */
	@JsonProperty("next")
	String getNext();

	/**
	 * Set the instanceID of the next Element after this. 
	 * 
	 */
	@JsonProperty("next")
	void setNext(String next);

	/**
	 * Get the instance ID of this {@link ElementInstance}. This needs to be unique WITHIN its context (i.e. within the enclosing experiment AND the enclosing Project)  
	 * @return
	 */
	@JsonProperty("instanceID")
	String getInstanceID();

	/**
	 * Set the instance ID of this {@link ElementInstance}. This method does not check for uniqueness, but assumes this is unique within its context.
	 * @param instanceID
	 */
	@JsonProperty("instanceID")
	void setInstanceID(String instanceID);

	/**
	 * Return the ID of the next {@link TaskObjectInstance} for the given user based on the assumption, 
	 * that the user has finished the task at its `getProjectPosition()`. This function needs to checkk what the next eligible Element Task the participant
	 * can use is (not necessarily "getNext")
	 * @param user
	 * @return
	 */
	String nextTask(Participant user);

	/**
	 * Get the Field Specifications that make up this Element. Used for Initialization of an empty element.
	 * @return Fieldspecifications that can be used to initialize the fields of this element
	 */
	FieldSpecifications getElementSpecifications();
}