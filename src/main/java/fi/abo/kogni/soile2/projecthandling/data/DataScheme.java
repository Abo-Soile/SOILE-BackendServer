package fi.abo.kogni.soile2.projecthandling.data;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.vertx.core.json.JsonArray;

public class DataScheme {
	
	@JsonProperty("tasks")
	JsonArray tasks;
	
	@JsonProperty("tasks")
	public JsonArray getTasks()
	{
		return tasks;
	}
	
	@JsonProperty("tasks")
	public void setTasks(JsonArray tasks)
	{
		this.tasks = tasks;
	}
	
	
}
