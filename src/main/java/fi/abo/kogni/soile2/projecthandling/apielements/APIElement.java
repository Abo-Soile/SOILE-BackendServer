package fi.abo.kogni.soile2.projecthandling.apielements;


import java.util.function.Function;

import fi.abo.kogni.soile2.projecthandling.projectElements.ElementBase;
import fi.abo.kogni.soile2.projecthandling.projectElements.ElementFactory;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
/**
 * Objects that implement this class reflect elements in a database that allow linking to 
 * a gitRepository. They also store all Versions 
 * @author Thomas Pfau
 *
 */
public interface APIElement<T extends ElementBase> {

	/**
	 * This UUID is a mongoDB ID. 
	 * @return the UUID of this Element (if it is saved in a DB yet)
	 */
	String getUUID();

	/**
	 * Set the UUID of this element
	 * @param UUID The UUID String to set. 
	 */
	void setUUID(String UUID);

	/**
	 * Get the Version of the APIElement. This in the form of 
	 * A JsonArray of the form: 
	 * [
	 * 	{
	 * 		version: "12345abdcde"
	 * 		timestamp: 1234567
	 * 	}
	 * ]
	 * @return The version String 
	 */
	String getVersion();

	/**
	 * Set the Versions for this Element
	 * @param versions A JsonArray of the form indicated in {@link #getVersion()};
	 */
	void setVersion(String versions);


	/**
	 * A JsonArray of the form: 
	 * [
	 * 	{
	 * 		tag: "Version 1" 
	 * 		timestamp: "12345abcdf"
	 * 	}
	 * ]
	 * @return the Tag in the described format
	 */
	String getTag();

	/**
	 * Set the tag for this element form of the tag as in getTag
	 * @param tag The tag string
	 */
	void setTag(String tag);

	/**
	 * The name of this element
	 * @return The name of this element
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
	 * @param _private whether this element is private
	 */
	void setPrivate(Boolean _private);

	/**
	 * Get a Future of an (updated) element from the database. Note, this element will Be updated with the information provided 
	 * in the API Element and can directly be saved in order to update the database object.
	 * @param client the mongoclient to use to save the data
	 * @param elementFactory the elementFactory used to build the element  
	 * @return A {@link Future} of the Instance of the DB Element represented by this API Element
	 */
	Future<T> getDBElement(MongoClient client, ElementFactory<T> elementFactory);
	
	/**
	 * Load data from a database element fitting to this API elements type 
	 * @param element The element to load data from 
	 */
	void loadFromDBElement(T element);	
	
	/**
	 * Test whether this API Element has additional Content stored in the git repository besides the pure object data.
	 * If true, the additional data will be stored using storeAdditonalGitData and loaded via its loadAdditionalGitData functions
	 * @return this API Element has additional Content stored in the git repository
	 */
	boolean hasAdditionalGitContent();
		
	/**
	 * Store additional data to the git Manager, returns a future of the new git repo version with the data stored.
	 * @param currentVersion the version on which to base the additions
	 * @param eb {@link EventBus} used for to contact git
	 * @param targetRepository name of the target repository
	 * @return A Future of the updated version
	 */
	Future<String> storeAdditionalData(String currentVersion, EventBus eb, String targetRepository);
	
	/**
	 * Load additional data to from git and add it to this object.
	 * @param eb {@link EventBus} used for to contact git
	 * @param targetRepository name of the target repository
	 * @return if the loading operation was successfull
	 */
	Future<Boolean> loadAdditionalData(EventBus eb, String targetRepository);
	
	/**
	 * Get the json as stored in Git.
	 * @return the Json of this API Element that should go into git
	 */
	JsonObject getGitJson();
	
	/**
	 * Load data from a Json as stored in Git
	 * @param json the Json to load into the API node
	 */
	void loadGitJson(JsonObject json);
	
	/**
	 * Get the Json for this 
	 * @return the {@link JsonObject} representing this APIElement
	 */
	JsonObject getAPIJson();
	
	/**
	 * Load APIElement Data from a json object. 
	 * @param json the JsonObject to create this object from
	 */
	void loadFromAPIJson(JsonObject json);

	/**
	 * Calculate the dependencies of this element 
	 * @return The dependencies of this object
	 */
	JsonObject calcDependencies();
	
	/**
	 * Update this APIElement from a Json. but only use fields that are present in this element.
	 * @param update The Json Object to update this from
	 */
	void updateFromJson(JsonObject update);
	
	/**
	 * Get the filter for the specified field.
	 * @param fieldName The field name for the filter
	 * @return A Function filtering for the specific field
	 */
	Function<Object,Object> getFieldFilter(String fieldName); 
}


