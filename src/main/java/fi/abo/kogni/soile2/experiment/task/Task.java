package fi.abo.kogni.soile2.experiment.task;

import java.io.File;
import java.util.HashMap;

import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.json.JsonObject;

public class Task {
	private String UUID;
	private String version;
	private String Code;
	private HashMap<String,File> resources;	
	
	public Task(String uUID, String version) {
		super();
		UUID = uUID;
		this.version = version;
	}

	public String getID() {
		return UUID;
	}

	public JsonObject toJson() {		
		return new JsonObject().put(SoileConfigLoader.getTaskProperty("version"),version)
				 .put(SoileConfigLoader.getTaskProperty("id"),UUID);
	}		
	
	public void setCode(String code)
	{
		
	}
	
	public String getCode() {
		return Code;
	}	
	
	public File getRessource(String Path)
	{
		return resources.get(Path);
	}
				
}
