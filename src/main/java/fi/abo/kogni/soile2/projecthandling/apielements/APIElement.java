package fi.abo.kogni.soile2.projecthandling.apielements;


import com.fasterxml.jackson.annotation.JsonProperty;

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
public interface APIElement<T> {

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
	@JsonProperty("version")
	String getVersion();

	/**
	 * Set the Versions for this Element
	 * @param versions A JsonArray of the form indicated in {@link getVersions);
	 */
	@JsonProperty("version")
	void setVersion(String versions);


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
	@JsonProperty("tag")
	String getTag();

	/**
	 * Set the Versions for this Element
	 * @param versions A JsonArray of the form indicated in {@link getTags);
	 */
	@JsonProperty("tag")
	void setTag(String tag);

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

	Future<T> getDBElement(MongoClient client);
	
	JsonObject getGitJson();
}


