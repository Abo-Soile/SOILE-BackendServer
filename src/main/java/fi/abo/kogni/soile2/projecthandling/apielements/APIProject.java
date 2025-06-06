package fi.abo.kogni.soile2.projecthandling.apielements;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.abo.kogni.soile2.projecthandling.projectElements.impl.Project;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl.ExperimentObjectInstance;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl.FieldSpecifications;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl.Filter;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl.TaskObjectInstance;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.randomizers.Randomizer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * An API Project
 * @author Thomas Pfau
 *
 */
public class APIProject extends APIElementBase<Project>{

	private static final Logger LOGGER = LogManager.getLogger(APIProject.class);

	private String[] gitFields = new String[] {"name","tasks","experiments","filters","randomizers","start"};
	private Object[] gitDefaults = new Object[] {"",new JsonArray(),new JsonArray(),new JsonArray(),new JsonArray(),null};		
	private Function<Object, Object>[] elementCheckers;
	/**
	 * Constructor for an empty element
	 */
	public APIProject() {
		this(new JsonObject());
		
	}

	/**
	 * Element Constructor using Json Data 
	 * @param data the Json representation of the {@link APIProject}
	 */
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
												(x) -> FieldSpecifications.applySpecToArray((JsonArray)x, Randomizer.getFieldSpecifications()),
												(x) ->  {return x;}}; 
	}
	/**
	 * Set the start property
	 * @param start the start element of the project
	 */
	public void setStart(String start)
	{
		this.data.put("start", start);
	}
	
	/**
	 * Get the start property
	 * @return the id of the start object of the Project
	 */
	public String getStart()
	{
		return this.data.getString("start");
	}
	
	/**
	 * Set the tasks
	 * @param tasks the list of tasks
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
	 * @return A JsonArray of all Task (JsonObjects)
	 */
	public JsonArray getTasks()
	{		
		return this.data.getJsonArray("tasks");
	}
		
	/**
	 * Add a task
	 * @param task A {@link JsonObject} describing the task
	 */
	public void addTask(JsonObject task)
	{
		this.data.getJsonArray("tasks").add(task);
	}
	
	/**
	 * Get and set the randomizers
	 * @return A {@link JsonArray} of Randomizer {@link JsonObject}s
	 */
	public JsonArray getRandomizers()
	{		
		return this.data.getJsonArray("randomizers");
	}
		
	/**
	 * Add a JsonObject representing a Randomizer to this API Project
	 * @param randomizer the Randomizer to add.
	 */
	public void addRandomizer(JsonObject randomizer)
	{
		this.data.getJsonArray("randomizers").add(randomizer);
	}
	
	/**
	 * Set the Experiments {@link JsonArray}.
	 * @param experiments A {@link JsonArray} with Objects representing Experiments 
	 */
	public void setExperiments(JsonArray experiments)
	{
		JsonArray newExperiments =  new JsonArray();		
		for(int i = 0; i< experiments.size(); i++)
		{
			newExperiments.add(FieldSpecifications.filterFieldBySpec(experiments.getJsonObject(i), ExperimentObjectInstance.getFieldSpecs()));
		}
		this.data.put("experiments", newExperiments);
	}

	/**
	 * Get the Experiments contained in this Project
	 * @return A {@link JsonArray} of {@link JsonObject}s contaiing the data for the experiments in this Project
	 */
	public JsonArray getExperiments()
	{		
		return this.data.getJsonArray("experiments");
	}
		
	/**
	 * Add a Experiment to this Project
	 * @param experiment The JsonObject with the experiment data
	 */
	public void addExperiment(JsonObject experiment)
	{
		this.data.getJsonArray("experiments").add(experiment);
	}
	
	/**
	 * Set the Filters for this Project
	 * @param filters A {@link JsonArray} of {@link JsonObject} representing the filters.
	 */
	public void setFilters(JsonArray filters)
	{
		JsonArray newFilters =  new JsonArray();		
		for(int i = 0; i< filters.size(); i++)
		{
			newFilters.add(FieldSpecifications.filterFieldBySpec(filters.getJsonObject(i), Filter.getFieldSpecs()));
		}
		this.data.put("filters",newFilters);
	}

	/**
	 * Get the Filter definitions for this Project
	 * @return a {@link JsonArray} of {@link JsonObject}s representing the filters
	 */
	public JsonArray getFilters()
	{		
		return this.data.getJsonArray("filters");
	}
		
	/**
	 * Add a Filter by description
	 * @param filter The JsonObject describing the filter
	 */
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
		case "randomizers" :
			return (x) -> {return FieldSpecifications.applySpecToArray((JsonArray)x, Randomizer.getFieldSpecifications());};					
		default:
			return (x) -> {return x;};
		}
	}
	
	@Override
	public JsonObject calcDependencies() {
		HashSet<String> taskDependencies = new HashSet<>();
		HashSet<String> experimentDependencies = new HashSet<>();		
		for(int i = 0 ; i < this.getTasks().size(); i++)
		{
			taskDependencies.add(this.getTasks().getJsonObject(i).getString("UUID"));			
		}
		for(int i = 0 ; i < this.getExperiments().size(); i++)
		{
			experimentDependencies.add(this.getExperiments().getJsonObject(i).getString("UUID"));			
		}
		return new JsonObject().put("tasks", new JsonArray(new LinkedList<String>(taskDependencies)))
				.put("experiments", new JsonArray(new LinkedList<String>(experimentDependencies)));
		
	}
}
