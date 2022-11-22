package fi.abo.kogni.soile2.projecthandling.projectElements;

import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

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
	
	
	/**
	 * Load a project as specified by the UUID. This UUID is the mongoDB id. if no project could be loaded, the promise fails.
	 * @param client
	 * @param UUID
	 * @return
	 */
	public static Future<Experiment> loadExperiment(MongoClient client, String UUID)
	{
		Promise<Experiment> expPromise = Promise.<Experiment>promise();

		client.findOne(SoileConfigLoader.getdbProperty("experimentCollection"), new JsonObject().put("_id", UUID), null)
		.onSuccess(currentExp ->
				{					
					if(currentExp != null)
					{
						System.out.println("Found an existing project");
						
						Experiment p = new Experiment();
						p.loadfromJson(currentExp);
						expPromise.complete(p);
					}
					else
					{
							expPromise.fail(new ObjectDoesNotExist(UUID));
					}
				})
		.onFailure(err -> {
			expPromise.fail(err);
		});		
		return expPromise.future();

	}
}
