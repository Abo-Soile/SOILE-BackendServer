package fi.abo.kogni.soile2.projecthandling.projectElements;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.abo.kogni.soile2.projecthandling.exceptions.ElementNameExistException;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
/**
 * This is the representation of Database elements for Project generation and modification.
 * The spec is:
 * 	  type: object
 *    properties
 * 		name:
 *        type: string
 *        description: "Name of the project"
 *      _id:
 *        type: string
 *        example: 12345asasdadf32fds
 *        description: "UUID of the project"
 *      private:
 *        type: boolean
 *        description: whether the project is private or publicly viewable
 *      tags:
 *        type: array
 *        items:
 *          type: object
 *          properties:
 *            version: 
 *              type: string
 *              description: This is a version (in form of a githash) of this project
 *              example: dd6de3fddf28e769ef80808db277608ae9b76ead
 *            timestamp:
 *              type: integer
 *              format: int64
 *              description: a timestamp in int64 (long format i.e. milliseconds)
 *      versions:
 *        type: array
 *        items:
 *          type: object
 *          properties:
 *            tag:
 *              type: string
 *              description: A tag identifying a version of the project
 *            version: 
 *              type: string
 *              description: This is a version (in form of a githash) of this project
 *              example: dd6de3fddf28e769ef80808db277608ae9b76ead
 * 
 * 
 */
public abstract class ElementBase implements Element {

	/**
	 * The underlying element data
	 */
	protected JsonObject data; 	
	/**
	 * the current version
	 */
	protected String currentVersion;
	/**
	 * The Version map (version {@literal <}-> date)
	 */
	protected HashMap<String, Date> versions;
	/**
	 * The tag map (tags {@literal <}-> versions)
	 */
	protected HashMap<String,String> tags;
	/**
	 * The database base collection for this element
	 */
	protected String baseCollection;
	/**
	 * The database collection to use for this element
	 */
	protected String collectionToUse;
	/**
	 * Dependencies of this object 
	 */
	protected JsonObject dependencies;
	static final Logger LOGGER = LogManager.getLogger(ElementBase.class);
	
	/**
	 * Default constructor
	 * @param data the data representing this element
	 * @param collectionToUse the database collection associated with this element
	 */
	public ElementBase(JsonObject data, String collectionToUse)
	{	
		// by default, an object is visible. 		
		versions = new HashMap<>();
		tags = new HashMap<>();		
		this.data = new JsonObject();
		loadfromJson(data);		
		this.collectionToUse = collectionToUse;		
	}		

	@Override
	public String getUUID()
	{
		return data.getString("_id");
	}	

	@Override
	public void setUUID(String UUID)
	{
		data.put("_id", UUID);
	}
			
	@Override
	public JsonObject getDependencies()
	{
		return this.dependencies;
	}

	@Override
	public void addDependencies(JsonObject dependencies)
	{
		for(String field : dependencies.fieldNames())
		{
			JsonArray cdeps = this.dependencies.getJsonArray(field,new JsonArray()); 			
			Set<Object> currentdeps = new HashSet<>();
			// add everything.
			for(Object o : cdeps)
			{
				currentdeps.add(o);
			}
			for(Object o : dependencies.getJsonArray(field))
			{
				currentdeps.add(o);
			}
			this.dependencies.put(field, new JsonArray(new LinkedList<Object>(currentdeps)));			
		}
	}
	@Override
	public void setDependencies(JsonObject dependencies)
	{
		this.dependencies = dependencies;
	}
	@Override
	public JsonArray getVersions() {
		JsonArray versionArray = new JsonArray();
		for(Entry<String,Date> key : versions.entrySet())
		{
			versionArray.add(new JsonObject().put("version", key.getKey()).put("timestamp", key.getValue().getTime()));
		}
		return versionArray;
	}
	
	@Override
	public void setVersions(JsonArray versions) {
		HashMap<String,Date> newVersions = new HashMap<>();
		String latestVersion = null;
		Date latestDate = new Date(0);		
		for(int i = 0; i < versions.size(); i++)
		{
			JsonObject cVersion = versions.getJsonObject(i);
			Date currentDate = new Date(cVersion.getLong("timestamp"));
			LOGGER.debug(currentDate + " found while latest date was: " + latestDate);
			if(currentDate.after(latestDate))
			{
				latestDate = currentDate;
				latestVersion = cVersion.getString("version");
			}
			newVersions.put(cVersion.getString("version"), currentDate);
		}	
		if(currentVersion == null)
		{
			currentVersion = latestVersion;
		}
		this.versions = newVersions;
	}
	@Override
	public String getName() {
		return data.getString("name");
	}
	
	@Override
	public void setName(String name) {
		data.put("name", name);
	}

	@Override
	public JsonArray getTags() {
		JsonArray tagArray = new JsonArray();
		for(Entry<String,String> key : tags.entrySet())
		{
			tagArray.add(new JsonObject().put("tag", key.getKey()).put("version", key.getValue()));
		}
		return tagArray;
	}
	
	@Override	
	public void setTags(JsonArray tags) {
		HashMap<String,String> newTags = new HashMap<>();
		for(int i = 0; i < tags.size(); i++)
		{
			JsonObject currentTag = tags.getJsonObject(i);
			newTags.put(currentTag.getString("tag"), currentTag.getString("version"));
		}	
		this.tags = newTags;
	}		
	
	@Override
	public Boolean getPrivate() {
		return data.getBoolean("private");
	}
	
	@Override
	public void setPrivate(Boolean isprivate) {
		data.put("private",isprivate);
	}

	
	@Override
	public Boolean getVisible() {
		return data.getBoolean("visible");
	}
	
	@Override
	public void setVisible(Boolean visible) {
		data.put("visible",visible);
	}
	
	
	
	@Override
	public String getVersionForTag(String tag)
	{
		return tags.get(tag);
	}
	
	@Override
	public void addVersion(String version)
	{
		// we only add this version 
		if(!versions.containsKey(version))
		{
			versions.put(version, new Date());
		}
		currentVersion = version;
	}	

	@Override
	public void setCurrentVersion(String current)
	{
		currentVersion = current;
	}

	
	@Override
	public String getCurrentVersion()
	{
		return currentVersion;
	}
	
	@Override
	public void addTag(String tag, String version)
	{
		tags.put(tag, version);
	}
	@Override
	public List<String> getVersionsStrings()
	{
		return List.copyOf(versions.keySet());
	}
	
	@Override
	public Date getVersionDate(String version)
	{
		return versions.get(version);
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
	
	/**
	 * Load data from Json. Implementing classes should override this function using super.loadFromJson(json) to
	 * set up the default fields and then add additional information 	
	 * @param json The json this class represents.
	 */
	public void loadfromJson(JsonObject json)
	{		
		setTags(json.getJsonArray("tags", new JsonArray()));
		setVersions(json.getJsonArray("versions", new JsonArray()));
		setName(json.getString("name",""));
		setVisible(json.getBoolean("visible", true));
		// either load from the UUID Field or from the _id field if the UUID does not exist.
		setUUID(json.getString("UUID", json.getString("_id")));
		setPrivate(json.getBoolean("private",false));
		setDependencies(json.getJsonObject("dependencies",new JsonObject()));
	}
	/**
	 * Get the Json object including the UUID as UUID field
	 * @return a {@link JsonObject} reprentation of the Object
	 */
	public JsonObject toJson()
	{
		return toJson(true);
	}
	
	/**
	 * Get the Json object including the UUID as UUID field
	 * @param provideUUID whether to provide the db uuid 
	 * @return a {@link JsonObject} representation of the object
	 */
	public JsonObject toJson(boolean provideUUID)
	{
		JsonObject result = new JsonObject().put("name", getName())
				   .put("versions", getVersions())
				   .put("tags", getTags())				   
				   .put("private", getPrivate())
				   .put("dependencies", getDependencies())
				   .put("visible",getVisible());
				   
		if(provideUUID)
		{
			result.put("_id", getUUID());	
		}		
		return result; 
	}
	@Override
	public String getTargetCollection()
	{
		return collectionToUse;
	}
	
	/**
	 * Save the current element, this will return the UUID of the project and 
	 * @param client A {@link MongoClient} for db access
	 * @return a Future of the UUID of the saved element.
	 */
	public Future<String> save(MongoClient client)
	{
		Promise<String> saveSuccess = Promise.<String>promise();
		// TODO: Set the UUID field as an Index, then this should work.
		if(getUUID() != null)
		{
			// So, we have a UUID in this element, which means it is not freshly created.
			JsonObject defaultSetUpdates = new JsonObject().put("name", getName()).put("private", getPrivate()).put("visible", getVisible());
			JsonObject dependencyUpdate = new JsonObject();			
			JsonObject dependencies = getDependencies();
			for(String field : dependencies.fieldNames())
			{
				dependencyUpdate.put("dependencies." + field , new JsonObject().put("$each", dependencies.getJsonArray(field)));
			}
			JsonObject updates = getUpdates();			
			if(!updates.containsKey("$set"))
			{
				updates.put("$set", defaultSetUpdates);
			}
			else
			{
				updates.getJsonObject("$set").mergeIn(defaultSetUpdates);
			}
			if(!updates.containsKey("$addToSet"))
			{
				updates.put("$addToSet", dependencyUpdate);
			}
			else
			{
				updates.getJsonObject("$addToSet").mergeIn(dependencyUpdate);
			}
			// now, check that the name we have here does not collide with a name in the db.
			JsonObject differingIDQuery = new JsonObject()
					.put("_id", new JsonObject()
							.put("$not", new JsonObject()
											 .put("$eq", getUUID())											  
							)
			)
			.put("name",getName());
			client.findOne(collectionToUse, differingIDQuery , null)
			.onSuccess(exists -> {
				if(exists != null)
				{
					saveSuccess.fail(new ElementNameExistException(getName(), exists.getString("_id")));	
				}
				else
				{					
					client.updateCollection(collectionToUse,
							new JsonObject().put("_id", this.getUUID().toString()), updates)
					.onSuccess(res -> {
						saveSuccess.complete(getUUID());
					})
					.onFailure(err -> saveSuccess.fail(err));		
				}
			})
			.onFailure(err -> {
				saveSuccess.fail(err);	
			});
			
		}
		else
		{
			// since the item did not exist in the db (i.e. did not have a UUID yet, we can simply return it.			
			client.save(collectionToUse,toJson(false))
			.onSuccess(res -> {
				setUUID(res);
				saveSuccess.complete(res);
			})
			.onFailure(err -> saveSuccess.fail(err));
		}
		return saveSuccess.future();
	}
		
	/**
	 * Get a JsonObject that contains all updates for this element based on its current state. 
	 * This contains push or set updates.
	 * @return a {@link JsonObject} that can be used with a {@link MongoClient}s update methods. 
	 */
	public abstract JsonObject getUpdates();
	
	@Override
	public JsonObject getListFields()
	{
		return new JsonObject();
	}
}
