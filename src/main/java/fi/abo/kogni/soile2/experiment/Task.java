package fi.abo.kogni.soile2.experiment;

import io.vertx.core.json.JsonObject;

public class Task implements ExperimentElement{
	private String UUID;
	
	
	public Task(String uUID) {
		super();
		UUID = uUID;
	}

	@Override
	public String getID() {
		// TODO Auto-generated method stub
		return UUID;
	}

	@Override
	public JsonObject toJson() {
		// TODO Auto-generated method stub
		return null;
	}

}
