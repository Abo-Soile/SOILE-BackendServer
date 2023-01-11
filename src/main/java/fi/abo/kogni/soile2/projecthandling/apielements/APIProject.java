package fi.abo.kogni.soile2.projecthandling.apielements;

import com.fasterxml.jackson.annotation.JsonProperty;

import fi.abo.kogni.soile2.projecthandling.projectElements.Project;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class APIProject extends APIElementBase<Project>{

	
	private String[] gitFields = new String[] {"name","tasks","experiments","filters","start"};
	private Object[] gitDefaults = new Object[] {"",new JsonArray(),new JsonArray(),new JsonArray(),null};

	public APIProject() {
		this(new JsonObject());
	}

	
	public APIProject(JsonObject data) {
		super(data);
		loadGitJson(data);		
	}
	
	@JsonProperty("start")
	public void setStart(String start)
	{
		this.data.put("start", start);
	}

	@JsonProperty("start")
	public String getStart()
	{
		return this.data.getString("start");
	}
	
	
	@JsonProperty("tasks")
	public void setTasks(JsonArray tasks)
	{
		this.data.put("tasks", tasks);
	}

	@JsonProperty("tasks")
	public JsonArray getTasks()
	{		
		return this.data.getJsonArray("tasks");
	}
		
	public void addTask(JsonObject task)
	{
		this.data.getJsonArray("tasks").add(task);
	}
	
	
	@JsonProperty("experiments")
	public void setExperiments(JsonArray experiments)
	{
		this.data.put("experiments", experiments);
	}

	@JsonProperty("experiments")
	public JsonArray getExperiments()
	{		
		return this.data.getJsonArray("experiments");
	}
		
	public void addExperiment(JsonObject experiment)
	{
		this.data.getJsonArray("experiments").add(experiment);
	}
	
	@JsonProperty("filters")
	public void setFilters(JsonArray filters)
	{
		this.data.put("filters", filters);
	}

	@JsonProperty("filters")
	public JsonArray getFilters()
	{		
		return this.data.getJsonArray("filters");
	}
		
	public void addFilter(JsonObject filter)
	{
		this.data.getJsonArray("filters").add(filter);
	}
	
	
	@Override
	public void setElementProperties(Project project) {
		JsonArray currentTasks = data.getJsonArray("tasks",new JsonArray()); 
		for(int i = 0; i < currentTasks.size(); i++)
		{
			project.addElement(currentTasks.getJsonObject(i).getString("UUID"));
		}
		JsonArray currentExperiments = data.getJsonArray("experiments",new JsonArray()); 
		for(int i = 0; i < currentExperiments.size(); i++)
		{
			project.addElement(currentExperiments.getJsonObject(i).getString("UUID"));
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
