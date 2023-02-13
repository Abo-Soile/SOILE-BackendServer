package fi.abo.kogni.soile2.projecthandling.apielements;

import com.fasterxml.jackson.annotation.JsonProperty;

import fi.abo.kogni.soile2.projecthandling.projectElements.impl.Project;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl.ExperimentObjectInstance;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl.FieldSpecifications;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl.Filter;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl.TaskObjectInstance;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * An API Project
 * @author Thomas Pfau
 *
 */
public class APIProject extends APIElementBase<Project>{

	
	private String[] gitFields = new String[] {"name","tasks","experiments","filters","start"};
	private Object[] gitDefaults = new Object[] {"",new JsonArray(),new JsonArray(),new JsonArray(),null};		
	private Function 
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
		JsonArray newTasks =  new JsonArray();		
		for(int i = 0; i< tasks.size(); i++)
		{
			newTasks.add(FieldSpecifications.filterFieldBySpec(tasks.getJsonObject(i), TaskObjectInstance.getFieldSpecs()));
		}
		this.data.put("tasks", newTasks);
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
		JsonArray newExperiments =  new JsonArray();		
		for(int i = 0; i< experiments.size(); i++)
		{
			newExperiments.add(FieldSpecifications.filterFieldBySpec(experiments.getJsonObject(i), ExperimentObjectInstance.getFieldSpecs()));
		}
		this.data.put("experiments", newExperiments);
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
		JsonArray newFilters =  new JsonArray();		
		for(int i = 0; i< filters.size(); i++)
		{
			newFilters.add(FieldSpecifications.filterFieldBySpec(filters.getJsonObject(i), Filter.getFieldSpecs()));
		}
		this.data.put("filters",newFilters);
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
