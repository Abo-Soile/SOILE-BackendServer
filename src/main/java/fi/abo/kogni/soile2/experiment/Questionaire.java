package fi.abo.kogni.soile2.experiment;

import io.vertx.core.json.JsonObject;

public class Questionaire implements ExperimentElement{

	private String UUID;
	public Questionaire(String UUID)
	{
		this.UUID = UUID;
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
