package fi.abo.kogni.soile2.projecthandling.projectElements;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.mongo.MongoClient;
/**
 * Objects that implement this class reflect elements in a database that allow linking to 
 * a gitRepository. They also store all Versions 
 * @author Thomas Pfau
 *
 */
public interface Element {

	/**
	 * This UUID is a mongoDB ID. 
	 * @return the UUID of this Element (if it is saved in a DB yet)
	 */
	@JsonProperty("UUID")
	String getUUID();

	@JsonProperty("UUID")
	void setUUID(String uuid);

	/**
	 * A JsonArray of the form: 
	 * [
	 * 	{
	 * 		version: "12345abdcde"
	 * 		timestamp: 1234567
	 * 	}
	 * ]
	 * @return
	 */
	@JsonProperty("versions")
	JsonArray getVersions();

	/**
	 * Set the Versions for this Element
	 * @param versions A JsonArray of the form indicated in {@link getVersions);
	 */
	@JsonProperty("versions")
	void setVersions(JsonArray versions);

	
	/**
	 * A JsonArray of the form: 
	 * [
	 * 	{
	 * 		tag: "Version 1" 
	 * 		timestamp: "12345abcdf"
	 * 	}
	 * ]
	 * @return
	 */
	@JsonProperty("tags")
	JsonArray getTags();

	/**
	 * Set the Versions for this Element
	 * @param versions A JsonArray of the form indicated in {@link getTags);
	 */
	@JsonProperty("tags")
	void setTags(JsonArray tags);
	
	/**
	 * The name of this element
	 * @return
	 */
	@JsonProperty("name")
	String getName();

	/**
	 * Set the name of the element 
	 * @param name
	 */
	@JsonProperty("name")
	void setName(String name);

	/**
	 * The private property of this element
	 * @return whether the element is private (i.e. needs special access)
	 */
	@JsonProperty("private")
	Boolean getPrivate();
	/**
	 * Set the private property of this object.
	 * @param _private
	 */
	@JsonProperty("private")
	void setPrivate(Boolean _private);

	
	/**
	 * The visible property of this element
	 * @return whether the element is visible (if not visible, it is in the process of deletion and will be removed if no other element refers to it any more.)
	 */
	@JsonProperty("visible")
	Boolean getVisible();
	/**
	 * Set the visible property of this object.
	 * @param _private
	 */
	@JsonProperty("visible")
	void setVisible(Boolean visible);
	
	
	
	/**
	 * Convert to a string
	 * @return Most likely the data in Json Format.
	 */
	String toString();

	/**
	 * Convert the represented json object to a string, restricting to the specific fields.
	 * @param fields
	 * @return
	 */
	String toString(List<String> fields);
	
	/**
	 * Save this element into its database. Returns a future with the ID.
	 * @param client
	 * @return
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
	 * @param version
	 */	
	public void addVersion(String version);
	
	/**
	 * Set the version to be used for this project
	 * @param current the version to be used as currentVersion
	 */	
	public void setCurrentVersion(String current);
	/**
	 * Get the version to be used for this project
	 * @param current the version to be used as currentVersion
	 */	
	public String getCurrentVersion();
	/**
	 * Add a tag to a version for better retrievability.
	 * @param tag
	 * @param version
	 */
	public void addTag(String tag, String version);
	/**
	 * Get the Vrsions available for this project.
	 * @return
	 */
	public List<String> getVersionsStrings();
	
	/**
	 * Get the date for a specific version.
	 * @param version
	 * @return
	 */
	public Date getVersionDate(String version);
	
	/**
	 * Get the database collection to use for this Element
	 * @return
	 */
	public String getTargetCollection();
	
	
	/**
	 * Get the Type identifier, to be used for git retrieval.
	 * @return
	 */
	public String getTypeID();
		
}