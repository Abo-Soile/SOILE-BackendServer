package fi.abo.kogni.soile2.projecthandling.projectElements.impl;

import java.util.LinkedList;
import java.util.List;

import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.TargetElementType;
import fi.abo.kogni.soile2.projecthandling.projectElements.ElementBase;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Class representing an Experiment on the Database.
 * @author Thomas Pfau
 *
 */
public class Experiment extends ElementBase {

	private List<String> elements;

	/**
	 * Default Constructor
	 */
	public Experiment()
	{
		this(new JsonObject());
		
	}
	/**
	 * Default constructor using json data
	 * @param data Json data to use for the Experiment
	 */
	public Experiment(JsonObject data) {
		super(data, SoileConfigLoader.getdbProperty("experimentCollection"));
	}
	
	/**
	 * Get the Element ids in this experiments
	 * @return a List of IDs of objects in this experiment
	 */
	public JsonArray getElements() {
		return new JsonArray(elements);
	}
	/**
	 * Set the elements in this object
	 * @param elements the element (IDs) to set
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
	 * Add an element (ID) to the elements in this Experiment
	 * @param elementID the id to add
	 */
	public void addElement(String elementID)
	{
		elements.add(elementID);
	}
	
	
	@Override
	public void loadfromJson(JsonObject o)
	{
		super.loadfromJson(o);
		setElements(o.getJsonArray("elements",new JsonArray()));
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
		return "E";
	}
	
	@Override
	public TargetElementType getElementType() {
		return TargetElementType.EXPERIMENT;
	}
}
