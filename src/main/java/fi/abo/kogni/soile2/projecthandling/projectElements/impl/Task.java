package fi.abo.kogni.soile2.projecthandling.projectElements.impl;

import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.TargetElementType;
import fi.abo.kogni.soile2.projecthandling.projectElements.ElementBase;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Class representing a Task in the Database.
 * @author Thomas Pfau
 *
 */
public class Task extends ElementBase {

	/**
	 * Type ID for type specification
	 */
	public static String typeID = "T";
	private static JsonObject listFields = new JsonObject().put("author", 1).put("language", 1).put("keywords", 1).put("created", 1).put("type", 1);
	/**
	 * Default empty Constructor
	 */
	public Task()
	{
		this(new JsonObject());
	}
	/**
	 * Copy constructor from Json Data
	 * @param data the data to load into the task
	 */
	public Task(JsonObject data)
	{		
		super(data, SoileConfigLoader.getdbProperty("taskCollection"));
		if(data.getJsonArray("resources") == null)
		{
			this.data.put("resources", new JsonArray());
		}
		initRequiredFields();
	}

	/**
	 * Add a specific resource
	 * @param filename the filename of the resource to add
	 */
	public void addResource(String filename) {		
		data.getJsonArray("resources").add(filename);
	}
	@Override
	public void loadfromJson(JsonObject json)
	{		
		super.loadfromJson(json);
		this.data.put("author", json.getValue("author","UNKNOWN"));
		this.data.put("language", json.getValue("language","UNKNOWN"));
		this.data.put("type", json.getValue("type","UNKNOWN"));
		this.data.put("description", json.getValue("description",this.getName()));
		this.data.put("keywords", json.getValue("keywords",new JsonArray()));
		this.data.put("created", json.getValue("created",System.currentTimeMillis()));
	}
	
	private JsonObject getDatafields()
	{				
		JsonObject res = new JsonObject();
		res.put("author", this.data.getValue("author","UNKNOWN"));
		res.put("language", this.data.getValue("language","UNKNOWN"));
		res.put("type", this.data.getValue("type","UNKNOWN"));
		res.put("description", this.data.getValue("description",this.getName()));
		res.put("keywords", this.data.getValue("keywords",new JsonArray()));
		res.put("created", this.data.getValue("created",System.currentTimeMillis()));
		return res;
	}
	
	@Override
	public JsonObject toJson(boolean provideUUID)
	{			
		JsonObject res = super.toJson(provideUUID);
		res.mergeIn(getDatafields());		
		return res;		
	}
	@Override
	public JsonObject getUpdates()
	{
			JsonObject updateVersions = new JsonObject().put("versions", new JsonObject().put("$each", getVersions()));
			JsonObject updateTags = new JsonObject().put("tags", new JsonObject().put("$each", getTags()));
			JsonObject updates = new JsonObject().put("$addToSet", new JsonObject().mergeIn(updateVersions).mergeIn(updateTags));
			updates.put("$set", getDatafields());
			return updates;
	}

	@Override
	public String getTypeID() {
		return typeID;
	}	
	@Override
	public TargetElementType getElementType() {
		return TargetElementType.TASK;
	}
	
	/**
	 * This function creates all required fields for a task, if they don't exist in the current data of the task.
	 * These fields include:
	 * author: The original author of the task 
	 * language: The language the task content is in
	 * type: The type of the task
	 * description: A textual description of the task
	 * keywords: a list of keywords for the task (for searchability)
	 * created: A time stamp of the creation time of this task
	 */
	private void initRequiredFields() {
		this.data.mergeIn(this.getDatafields());					
	}
	
	/**
	 * Get the author of the task
	 * @return the author of this task
	 */
	public String getAuthor()
	{
		return this.data.getString("author");
	}
	/**
	 * Set the author of the task
	 * @param author the author of the task
	 */
	public void setAuthor(String author)
	{
		this.data.put("author", author);
	}
	
	/**
	 * Get the language of the task
	 * @return the language (non coding)  the task is in
	 */
	public String getLanguage()
	{
		return this.data.getString("language");
	}
	/**
	 * Set the language of the task
	 * @param language the language the task is in
	 */
	public void setLanguage(String language)
	{
		this.data.put("language", language);
	}
	
	/**
	 * Get the type category of the task
	 * @return the type of the task
	 */
	public String getType()
	{
		return this.data.getString("type");
	}
	/**
	 * Set the type catregory of the task
	 * @param type the type of the task
	 */
	public void setType(String type)
	{
		this.data.put("type", type);
	}
	
	/**
	 * Get the description of the task
	 * @return the description of the task
	 */
	public String getDescription()
	{
		return this.data.getString("description");
	}
	/**
	 * Set the description of the task
	 * @param description the description of the task
	 */
	public void setDescription(String description)
	{
		this.data.put("description", description);
	}
	
	/**
	 * Get the keywords for the task
	 * @return a {@link JsonArray} with keywords
	 */
	public JsonArray getKeywords()
	{
		return this.data.getJsonArray("keywords");
	}
	/**
	 * Set the keywords for the task
	 * @param keywords the keywords to assign to the task
	 */
	public void setKeywords(JsonArray keywords)
	{
		this.data.put("keywords", keywords);
	}
	
	/**
	 * Get the created timestamp of the task
	 * @return the timestamp of creation
	 */
	public Long getCreated()
	{
		return this.data.getLong("created");
	}
	@Override
	public JsonObject getListFields()
	{
		return listFields;
	}
	
}
