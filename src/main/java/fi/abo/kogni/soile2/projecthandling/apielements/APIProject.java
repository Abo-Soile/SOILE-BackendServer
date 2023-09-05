package fi.abo.kogni.soile2.projecthandling.apielements;

import java.util.function.Function;

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
	private Function<Object, Object>[] elementCheckers;   
	public APIProject() {
		this(new JsonObject());
		
	}

	
	public APIProject(JsonObject data) {
		super(data);
		createFunctionCheckers();
		loadGitJson(data);		
	}
	
	
	private void createFunctionCheckers()
	{
		this.elementCheckers = new Function[] { (x) -> {return x;}, 
												(x) -> FieldSpecifications.applySpecToArray((JsonArray)x, TaskObjectInstance.getFieldSpecs()), 
												(x) -> FieldSpecifications.applySpecToArray((JsonArray)x, ExperimentObjectInstance.getFieldSpecs()), 
												(x) -> FieldSpecifications.applySpecToArray((JsonArray)x, Filter.getFieldSpecs()), 
												(x) ->  {return x;}}; 
	}
	/**
	 * Set the start property
	 * @param start
	 */
	public void setStart(String start)
	{
		this.data.put("start", start);
	}
	
	/**
	 * Get the start property
	 * @return
	 */
	public String getStart()
	{
		return this.data.getString("start");
	}
	
	/**
	 * Set the tasks
	 * @param tasks
	 */
	public void setTasks(JsonArray tasks)
	{
		JsonArray newTasks =  new JsonArray();		
		for(int i = 0; i< tasks.size(); i++)
		{
			newTasks.add(FieldSpecifications.filterFieldBySpec(tasks.getJsonObject(i), TaskObjectInstance.getFieldSpecs()));
		}
		this.data.put("tasks", newTasks);
	}
	/**
	 * Get the tasks
	 * @return
	 */
	public JsonArray getTasks()
	{		
		return this.data.getJsonArray("tasks");
	}
		
	public void addTask(JsonObject task)
	{
		this.data.getJsonArray("tasks").add(task);
	}
	
	
	public void setExperiments(JsonArray experiments)
	{
		JsonArray newExperiments =  new JsonArray();		
		for(int i = 0; i< experiments.size(); i++)
		{
			newExperiments.add(FieldSpecifications.filterFieldBySpec(experiments.getJsonObject(i), ExperimentObjectInstance.getFieldSpecs()));
		}
		this.data.put("experiments", newExperiments);
	}

	public JsonArray getExperiments()
	{		
		return this.data.getJsonArray("experiments");
	}
		
	public void addExperiment(JsonObject experiment)
	{
		this.data.getJsonArray("experiments").add(experiment);
	}
	
	public void setFilters(JsonArray filters)
	{
		JsonArray newFilters =  new JsonArray();		
		for(int i = 0; i< filters.size(); i++)
		{
			newFilters.add(FieldSpecifications.filterFieldBySpec(filters.getJsonObject(i), Filter.getFieldSpecs()));
		}
		this.data.put("filters",newFilters);
	}

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
			this.data.put(gitFields[i], elementCheckers[i].apply(json.getValue(gitFields[i], gitDefaults[i])));
		}
	}
	
	
	public Function<Object,Object> getFieldFilter(String fieldName)
	{		
		switch(fieldName)			
		{
		case "tasks" :
			return (x) -> {return FieldSpecifications.applySpecToArray((JsonArray)x, TaskObjectInstance.getFieldSpecs());};			
		case "experiments" :
			return (x) -> {return FieldSpecifications.applySpecToArray((JsonArray)x, ExperimentObjectInstance.getFieldSpecs());};
		case "filters" :
			return (x) -> {return FieldSpecifications.applySpecToArray((JsonArray)x, Filter.getFieldSpecs());};		
		default:
			return (x) -> {return x;};
		}
	}
}
