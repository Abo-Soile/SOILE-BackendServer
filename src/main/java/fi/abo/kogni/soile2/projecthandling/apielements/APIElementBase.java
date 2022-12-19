package fi.abo.kogni.soile2.projecthandling.apielements;

import com.fasterxml.jackson.annotation.JsonProperty;

import fi.abo.kogni.soile2.datamanagement.git.GitManager;
import fi.abo.kogni.soile2.projecthandling.projectElements.ElementBase;
import fi.abo.kogni.soile2.projecthandling.projectElements.ElementFactory;
import io.vertx.core.Future;
import io.vertx.core.Promise;
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
public abstract class APIElementBase<T extends ElementBase> implements APIElement<T> {

	protected JsonObject data; 	

	public APIElementBase()
	{
		this.data = new JsonObject();
	}
	
	public APIElementBase(JsonObject data)
	{	
		this.data = data;
		// make sure fields are initialised
	}
	

	@Override
	@JsonProperty("UUID")
	public String getUUID()
	{
		return data.getString("UUID");
	}	

	@Override
	@JsonProperty("UUID")
	public void setUUID(String uuid)
	{
		data.put("UUID", uuid);
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
	@JsonProperty("version")
	public String getVersion() {		
		return data.getString("version","");
	}
	
	@Override
	@JsonProperty("version")
	public void setVersion(String version) {
		data.put("version", version); 
	}
	@Override
	@JsonProperty("name")
	public String getName() {
		return data.getString("name");
	}
	
	@Override
	@JsonProperty("name")
	public void setName(String name) {
		data.put("name", name);
	}

	@Override
	@JsonProperty("tag")
	public String getTag() {
		return data.getString("tag");
	}
	
	@Override	
	@JsonProperty("tag")
	public void setTag(String tag) {
		data.put("tag", tag);
	}		
	
	@Override
	@JsonProperty("private")
	public Boolean getPrivate() {
		return data.getBoolean("private", false);
	}
	
	@Override
	@JsonProperty("private")
	public void setPrivate(Boolean isprivate) {
		data.put("private",isprivate);
	}

	/**
	 * Update the versions and tags of a DBElement to reflect this elements data in it.
	 * @param target
	 */
	public void setDefaultProperties(T target)
	{
		if(getTag() != null)
		{
			target.addTag(getTag(), getVersion());
		}
		target.addVersion(getVersion());
		target.setName(getName());
		target.setUUID(getUUID());
		target.setPrivate(getPrivate());
	}
	
	/**
	 * Load the default properties retrievable from the db object.
	 * @param target
	 */
	public void loadDefaultProperties(T target)
	{
		setName(target.getName());
		setUUID(target.getUUID());
		setPrivate(target.getPrivate());
	}
	
	public boolean hasAdditionalGitContent()
	{
		return false;
	}
	
	/**
	 * This is essentially a no--op on most objects.
	 */
	@Override
	public Future<String> storeAdditionalData(String currentVersion, GitManager gitManager, String targetRepository)
	{
		return Future.succeededFuture(currentVersion);
	}

	/**
	 * This is essentially a no--op on most objects.
	 */
	@Override
	public Future<Boolean> loadAdditionalData(GitManager gitManager, String targetRepository)
	{
		return Future.succeededFuture(true);
	}
	/**
	 * Set the element Properties for a DB element of the type specified for this APIElement.
	 * @param target
	 */
	public abstract void setElementProperties(T target);
	
	/**
	 * Load properties which are specific to this element.
	 * @param target
	 */
	//public abstract void loadElementProperties(T target);
	
	
	@Override
	public Future<T> getDBElement(MongoClient client, ElementFactory<T> ElementFactory) {
		Promise<T> elementPromise = Promise.<T>promise();
		// load the element with the given UUID.
		ElementFactory.loadElement(client, getUUID())
		.onSuccess(element -> 
		{			
			setDefaultProperties(element);
			setElementProperties(element);
			elementPromise.complete(element);		
		})
			.onFailure(fail -> elementPromise.fail(fail));
			return elementPromise.future();
	}
	
	@Override
	public void loadFromDBElement(T element) {
		// at least for now, all other properties come from the git Repo....
		loadDefaultProperties(element);
	}
	
	public JsonObject getJson() {
		return this.data;
	}
	
	@Override
	public void loadFromJson(JsonObject currentJson) {
		this.data = currentJson;
	}
	
}
