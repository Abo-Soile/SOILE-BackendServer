package fi.abo.kogni.soile2.projecthandling.projectElements;

import java.util.Date;
import java.util.List;

import fi.abo.kogni.soile2.http_server.authentication.utils.AccessElement;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
/**
 * Objects that implement this class reflect elements in a database that allow linking to 
 * a gitRepository. They also store all Versions 
 * @author Thomas Pfau
 *
 */
public interface Element extends AccessElement {

	/**
	 * This UUID is a mongoDB ID. 
	 * @return the UUID of this Element (if it is saved in a DB yet)
	 */
	String getUUID();

	/**
	 * Set the UUID of the element
	 * @param UUID the new UUID
	 */
	void setUUID(String UUID);

	
	/**
	 * The Elements, this element depends on, separated by type.
	 * i.e. { tasks : [ id1, id2,...], experiments: [ exp1,exp2,...]} 
	 * @return the dependencies as a {@link JsonObject} as described above
	 */
	JsonObject getDependencies();

	/**
	 * Set the dependencies of this object
	 * i.e. { tasks : [ id1, id2,...], experiments: [ exp1,exp2,...]} 
	 * @param dependencies the dependencies to set (as speced above)
	 */
	void setDependencies(JsonObject dependencies);
	
	/**
	 * Add elements, this element depends on, separated by type.
	 * i.e. { tasks : [ id1, id2,...], experiments: [ exp1,exp2,...]} 
	 * @param dependencies this element depends on.
	 */
	void addDependencies(JsonObject dependencies);
	
	/**
	 * A JsonArray of the form: 
	 * [
	 * 	{
	 * 		version: "12345abdcde"
	 * 		timestamp: 1234567
	 * 	}
	 * ]
	 * @return the versions as a json array of {@link JsonObject} as described above
	 */
	JsonArray getVersions();

	/**
	 * Set the Versions for this Element
	 * @param versions A JsonArray of the form indicated in {@link #getVersions()};
	 */
	void setVersions(JsonArray versions);	
	
	/**
	 * A JsonArray of the form: 
	 * [
	 * 	{
	 * 		tag: "Version 1" 
	 * 		timestamp: "12345abcdf"
	 * 	}
	 * ]
	 * @return the tags in the above mentioned format 
	 */
	JsonArray getTags();

	/**
	 * Set the Versions for this Element
	 * @param tags A JsonArray of the form indicated in {@link #getTags()};
	 */
	void setTags(JsonArray tags);
	
	/**
	 * The name of this element
	 * @return the name of the element
	 */
	String getName();

	/**
	 * Set the name of the element 
	 * @param name the new name of the element
	 */
	void setName(String name);

	/**
	 * The private property of this element
	 * @return whether the element is private (i.e. needs special access)
	 */
	Boolean getPrivate();
	/**
	 * Set the private property of this object.
	 * @param _private the new private value
	 */
	void setPrivate(Boolean _private);

	
	/**
	 * The visible property of this element
	 * @return whether the element is visible (if not visible, it is in the process of deletion and will be removed if no other element refers to it any more.)
	 */
	Boolean getVisible();
	/**
	 * Set the visible property of this object.
	 * @param visible the new visible value
	 */
	void setVisible(Boolean visible);
	
	
	
	/**
	 * Convert to a string
	 * @return Most likely the data in Json Format.
	 */
	String toString();

	/**
	 * Convert the represented json object to a string, restricting to the specific fields.
	 * @param fields the fields to restrict to
	 * @return the requested string representation of the object
	 */
	String toString(List<String> fields);
	
	/**
	 * Save this element into its database. Returns a future with the ID.
	 * @param client the Mongoclient to use for saving
	 * @return A {@link Future} of the ID of the stored element
	 */
	public Future<String> save(MongoClient client);
	
	
	/**
	 * Get the version stored for a tag.
	 * @param tag the tag to retrieve the version hash for
	 * @return the version hash
	 */
	public String getVersionForTag(String tag);
	
	/**
	 * Add a version to this project. Note that this does not keep the associated git in Sync, this needs to be achieved elsewhere. 
	 * @param version the version to add
	 */	
	public void addVersion(String version);
	
	/**
	 * Set the version to be used for this element
	 * @param current the version to be used as currentVersion
	 */	
	public void setCurrentVersion(String current);
	/**
	 * Get the version to be used for this element
	 * @return the current version 
	 */	
	public String getCurrentVersion();
	/**
	 * Add a tag to a version for better retrievability.
	 * @param tag the tag to add
	 * @param version the version for the tag
	 */
	public void addTag(String tag, String version);
	/**
	 * Get the Versions available for this project.
	 * @return a List of version strings
	 */
	public List<String> getVersionsStrings();
	
	/**
	 * Get the date for a specific version.
	 * @param version the version for which to retrieve a date
	 * @return the Date of the version
	 */
	public Date getVersionDate(String version);	
	
	/**
	 * Get the Type identifier, to be used for git retrieval.
	 * @return the Type (Experiment, Task, Project) of the element
	 */
	public String getTypeID();		

	/**
	 * Get the fields of the database that should be returned when a list of these objects is requested.
	 * name and UUID do not need to get included in this object
	 * @return A {@link JsonObject} containing the fields to include in the list as in "{"author" : 1 } 
	 */
	public JsonObject getListFields();

}