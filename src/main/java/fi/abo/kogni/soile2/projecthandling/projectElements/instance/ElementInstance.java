package fi.abo.kogni.soile2.projecthandling.projectElements.instance;

import java.util.List;

import fi.abo.kogni.soile2.projecthandling.participant.Participant;
import fi.abo.kogni.soile2.projecthandling.projectElements.Element;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl.ExperimentObjectInstance;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl.FieldSpecifications;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl.TaskObjectInstance;
import io.vertx.core.Future;

/**
 * Representation of an element in either a {@link Study} OR a {@link ExperimentObjectInstance}.  
 * @author Thomas Pfau
 *
 */
public interface ElementInstance {

	/**
	 * Get the UUID of the underlying object -> this refers to the underlying {@link Element} not this instance. 
	 * @return the uuid as string
	 */
	String getUUID();

	/**
	 * Set the UUID of the underlying object -> this refers to the underlying {@link Element} not this instance. 
	 * @param UUID the UUID to set 
	 */
	void setUUID(String UUID);

	/**
	 * Can be either be a commit ID or a Tag
	 * @return the version 
	 */
	String getVersion();

	/**
	 * Can be either be a commit ID or a Tag
	 * @param version the version to set
	 */
	void setVersion(String version);

	/**
	 * Get the Name of this Instance
	 * @return the name of the element
	 */
	String getName();

	/**
	 * A human readable name for a 
	 * @param name the name to set
	 */
	void setName(String name);

	/**
	 * Will normally be the Json Representation.
	 * @return a string representation of the element
	 */
	@Override
	String toString();

	/**
	 * like toString() but only using the fields indicated
	 * @param fields the field to restrict the string representation to
	 * @return a string representation restricted to the given fields
	 */
	String toString(List<String> fields);
	
	/**
	 * TODO: Check if this is actually necessary... (probably not)
	 * @return whether the instance is private
	 */
	Boolean getPrivate();
	/**
	 * Set whether the instance is private
	 * @param _private new private value
	 */
	void setPrivate(Boolean _private);
	
	/**
	 * Get the instanceID of the next Element after this. 
	 * @return the id of the next element
	 */
	String getNext();

	/**
	 * Set the instanceID of the next Element after this. 
	 * @param next the id of the next element
	 */
	void setNext(String next);

	/**
	 * Get the instance ID of this {@link ElementInstance}. This needs to be unique WITHIN its context (i.e. within the enclosing experiment AND the enclosing Project)  
	 * @return the id of this instance
	 */
	String getInstanceID();

	/**
	 * Set the instance ID of this {@link ElementInstance}. This method does not check for uniqueness, but assumes this is unique within its context.
	 * @param instanceID the new id for this instance
	 */
	void setInstanceID(String instanceID);

	/**
	 * Return the ID of the next {@link TaskObjectInstance} for the given user based on the assumption, 
	 * that the user has finished the task at its `getProjectPosition()`. This function needs to checkk what the next eligible Element Task the participant
	 * can use is (not necessarily "getNext")
	 * @param user the next element given the user (to retrieve the status
	 * @return A {@link Future} of the id of the next element 
	 */
	Future<String> nextTask(Participant user);

	/**
	 * Get the Field Specifications that make up this Element. Used for Initialization of an empty element.
	 * @return {@link FieldSpecifications} that can be used to initialize the fields of this element
	 */
	FieldSpecifications getElementSpecifications();
}