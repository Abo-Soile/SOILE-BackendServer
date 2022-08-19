package fi.abo.kogni.soile2.experiment;

import io.vertx.core.json.JsonObject;

public interface ExperimentElement {

	enum Type {
		Questionaire,
		Task,
		Experiment
	}

	public static ExperimentElement create(String UUID, Type type)
	{
		switch(type)
		{
		case Questionaire:
			return new Questionaire(UUID);
		case Task:
			return null;
		case Experiment:
			return null;
		}	
		return null;		
	}

	
	
	public String getID();
		
	public JsonObject toJson(); 
	
}
