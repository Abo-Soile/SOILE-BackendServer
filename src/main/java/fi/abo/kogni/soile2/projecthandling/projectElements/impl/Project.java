package fi.abo.kogni.soile2.projecthandling.projectElements.impl;

import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.TargetElementType;
import fi.abo.kogni.soile2.projecthandling.projectElements.ElementBase;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * This class represents a Project as stored in the mongoDB Project Database.
 * It essentially represents the Json object represented in the Database.
 * @author Thomas Pfau
 *
 */
public class Project extends ElementBase{
	static final Logger LOGGER = LogManager.getLogger(Project.class);
	private List<String> elements;
	/**
	 * Default constructor
	 */
	public Project()
	{
		super(new JsonObject(), SoileConfigLoader.getdbProperty("projectCollection"));
		elements = new LinkedList<String>();
	}
			
	/**
	 * Get Elements in this project
	 * @return a JsonArray with Strings containing the IDs of all elements
	 */
	public JsonArray getElements() {
		return new JsonArray(elements);
	}
	/**
	 * Set the elements in this project
	 * @param elements the new elements in this project
	 */
	public void setElements(JsonArray elements) {
		List<String> newElements = new LinkedList<String>();
		for(int i = 0; i < elements.size(); i++)
		{
			newElements.add(elements.getString(i));
		}
		this.elements = newElements;
	}
	
	/**
	 * Add an element to the list of elements used by this Project
	 * @param elementID the id of the element to add
	 */
	public void addElement(String elementID)
	{
		elements.add(elementID);
	}
	
	@Override
	public JsonObject toJson(boolean provideUUID)
	{		
		return super.toJson(provideUUID).put("elements", getElements());
	}	
	
	@Override
	public JsonObject getUpdates()
	{
			JsonObject updateVersions = new JsonObject().put("versions", new JsonObject().put("$each", getVersions()));
			JsonObject updateTags = new JsonObject().put("tags", new JsonObject().put("$each", getTags()));
			JsonObject updateElements = new JsonObject().put("elements", new JsonObject().put("$each", getElements()));
			JsonObject updates = new JsonObject().put("$addToSet", new JsonObject().mergeIn(updateVersions).mergeIn(updateElements).mergeIn(updateTags));
			return updates;
	}
	
	@Override
	public String getTypeID() {
		return "P";
	}
	
	@Override
	public TargetElementType getElementType() {
		return TargetElementType.PROJECT;
	}
}
