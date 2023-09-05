package fi.abo.kogni.soile2.projecthandling.projectElements;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.abo.kogni.soile2.projecthandling.exceptions.ElementNameExistException;
import io.vertx.core.Future;
import io.vertx.core.Promise;
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

	protected JsonObject data; 	
	protected String currentVersion;
	protected HashMap<String, Date> versions;
	protected HashMap<String,String> tags;
	protected String baseCollection;
	protected String collectionToUse;
	static final Logger LOGGER = LogManager.getLogger(ElementBase.class);

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
		
	/**
	 * Get the versions as a JsonArray of JsonObjects with:
	 * {
	 * 	version: abcdefabcdef12456
	 * 	timestamp: 123456688
	 * }
	 * @return the versions array.
	 */
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
	
	
	/**
	 * Get the version stored for a tag.
	 * @param tag the tag to retrieve the version hash for
	 * @return the version hash
	 */
	@Override
	public String getVersionForTag(String tag)
	{
		return tags.get(tag);
	}
	/**
	 * Add a version to this project. Note that this does not keep the associated git in Sync, this needs to be achieved elsewhere. 
	 * @param version
	 */
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
	/**
	 * Set the version to be used for this project
	 * @param current the version to be used as currentVersion
	 */
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
	
	/**
	 * Add a tag to a version for better retrievability.
	 * @param tag
	 * @param version
	 */
	@Override
	public void addTag(String tag, String version)
	{
		tags.put(tag, version);
	}
	/**
	 * Get the Versions available for this project.
	 * @return
	 */
	@Override
	public List<String> getVersionsStrings()
	{
		return List.copyOf(versions.keySet());
	}
	
	/**
	 * Get the date for a specific version.
	 * @param version
	 * @return
	 */
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
	}
	/**
	 * Get the Json object including the UUID as UUID field
	 * @return
	 */
	public JsonObject toJson()
	{
		return toJson(true);
	}
	
	/**
	 * 
	 * @param provideUUID
	 * @return
	 */
	public JsonObject toJson(boolean provideUUID)
	{
		JsonObject result = new JsonObject().put("name", getName())
				   .put("versions", getVersions())
				   .put("tags", getTags())				   
				   .put("private", getPrivate())
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
	 * 
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
			JsonObject updates = getUpdates();			
			if(!updates.containsKey("$set"))
			{
				updates.put("$set", defaultSetUpdates);
			}
			else
			{
				updates.getJsonObject("$set").mergeIn(defaultSetUpdates);
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
		
}
