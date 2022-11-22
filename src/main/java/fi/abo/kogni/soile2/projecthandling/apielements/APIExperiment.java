package fi.abo.kogni.soile2.projecthandling.apielements;

import com.fasterxml.jackson.annotation.JsonProperty;

import fi.abo.kogni.soile2.projecthandling.projectElements.Experiment;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

public class APIExperiment extends APIElementBase<Experiment> {

	private static String[] gitFields = new String[] {"name", "elements"};
	private static Object[] gitDefaults = new Object[] {"", new JsonArray()};

	
	public APIExperiment(JsonObject data) {
		super(data);
		// TODO Auto-generated constructor stub
	}

	@JsonProperty("elements")
	public JsonArray getElements() {
		return data.getJsonArray("elements",new JsonArray());
	}
	@JsonProperty("elements")
	public void setCode(JsonArray elements) {
		data.put("elements", elements);
	}
	
	@Override
	public void setElementProperties(Experiment experiment) {
		JsonArray currentElements = data.getJsonArray("elements",new JsonArray()); 
		for(int i = 0; i < currentElements.size(); i++)
		{
			experiment.addElement(currentElements.getJsonObject(i).getJsonObject("data").getString("UUID"));
		}			
	}

	@Override
	public JsonObject getGitJson() {
		JsonObject gitData = new JsonObject();
		for(int i = 0; i < gitFields.length ; ++i)
		{
			gitData.put(gitFields[i], data.getValue(gitFields[i], gitDefaults[i]));	
		}
		return gitData;
	}
}
