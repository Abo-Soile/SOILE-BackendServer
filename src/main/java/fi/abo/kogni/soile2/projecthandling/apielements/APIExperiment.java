package fi.abo.kogni.soile2.projecthandling.apielements;

import com.fasterxml.jackson.annotation.JsonProperty;

import fi.abo.kogni.soile2.projecthandling.projectElements.impl.Experiment;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class APIExperiment extends APIElementBase<Experiment> {

	private String[] gitFields = new String[] {"name", "elements"};
	private Object[] gitDefaults = new Object[] {"", new JsonArray()};

	public APIExperiment() {
		this(new JsonObject());
	}
	
	public APIExperiment(JsonObject data) {
		super(data);
		loadGitJson(data);
	}
			
	@JsonProperty("elements")
	public JsonArray getElements() {
		if(!data.containsKey("elements"))
		{
			data.put("elements", new JsonArray());
		}
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


	@Override
	public void loadGitJson(JsonObject json) {
		for(int i = 0; i < gitFields.length ; ++i)
		{
			this.data.put(gitFields[i], json.getValue(gitFields[i], gitDefaults[i]));	
		}
	}
}
