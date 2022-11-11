package fi.abo.kogni.soile2.project.elements;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class TaskObject extends ProjectDataBaseObject {

	public TaskObject(JsonObject data)
	{
		super(data);
	}

	@JsonProperty("codetype")
	public String getCodetype() {
		return data.getString("codetype");
	}
	public void setCodetype(String codetype) {
		data.put("codetype", codetype);
	}

	@JsonProperty("code")
	public String getCode() {
		return data.getString("code");
	}
	public void setCode(String code) {
		data.put("code", code);
	}


	@JsonProperty("resources")
	public JsonArray getResources() {
		return data.getJsonArray("resources");
	}
	public void setResources(JsonArray resources) {
		data.put("resources", resources);
	}

	public void addResource(String filename) {
		data.getJsonArray("resources").add(filename);
	}


}
