package fi.abo.kogni.soile2.projecthandling.projectElements;

import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import fi.abo.kogni.soile2.projecthandling.apielements.APIElement;
import fi.abo.kogni.soile2.projecthandling.apielements.APIExperiment;
import fi.abo.kogni.soile2.projecthandling.exceptions.ObjectDoesNotExist;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

public class Experiment extends ElementBase {

	private List<String> elements;

	public Experiment()
	{
		this(new JsonObject());
	}
	
	public Experiment(JsonObject data) {
		super(data, SoileConfigLoader.getdbProperty("experimentCollection"));
		// TODO Auto-generated constructor stub
	}

	@JsonProperty("elements")
	public JsonArray getElements() {
		return new JsonArray(elements);
	}
	@JsonProperty("elements")
	public void setElements(JsonArray elements) {
		List<String> newElements = new LinkedList<String>();
		for(int i = 0; i < elements.size(); i++)
		{
			newElements.add(elements.getString(i));
		}
		this.elements = newElements;
	}
	
	public void addElement(String elementID)
	{
		elements.add(elementID);
	}
	
	
	@Override
	public JsonObject getUpdates()
	{
			JsonObject updateVersions = new JsonObject().put("versions", new JsonObject().put("$each", getVersions()));
			JsonObject updateTags = new JsonObject().put("tags", new JsonObject().put("$each", getTags()));
			JsonObject updateElements = new JsonObject().put("elements", new JsonObject().put("$each", getElements()));
			JsonObject updates = new JsonObject().put("$addToSet", new JsonObject().mergeIn(updateVersions).mergeIn(updateElements).mergeIn(updateTags))
												 .put("$set", new JsonObject().put("private", getPrivate()).put("name", getName()));
			return updates;
	}
	
	@Override
	public String getTargetCollection() {
		// TODO Auto-generated method stub
		return SoileConfigLoader.getdbProperty("experimentCollection");
	}
}
