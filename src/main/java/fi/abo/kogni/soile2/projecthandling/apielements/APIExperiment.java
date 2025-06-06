package fi.abo.kogni.soile2.projecthandling.apielements;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.abo.kogni.soile2.projecthandling.projectElements.impl.ElementManager;
import fi.abo.kogni.soile2.projecthandling.projectElements.impl.Experiment;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl.ExperimentObjectInstance;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl.FieldSpecifications;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl.Filter;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl.TaskObjectInstance;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
/**
 * An API Experiment instance
 * @author Thomas Pfau
 *
 */
public class APIExperiment extends APIElementBase<Experiment> {

	private String[] gitFields = new String[] {"name", "elements"};
	private Object[] gitDefaults = new Object[] {"", new JsonArray()};
	private Function<Object, Object>[] elementCheckers; 
	/**
	 * The Logger for API Experiments
	 */
	public static final Logger LOGGER = LogManager.getLogger(ElementManager.class);

	/**
	 * Default empty constructor
	 */
	public APIExperiment() {
		this(new JsonObject());
	}
	
	/**
	 * Default constructor using Json data
	 * @param data the Json Representation of the experiment
	 */
	public APIExperiment(JsonObject data) {
		super(data);
		createFunctionCheckers();
		loadGitJson(data);
	}
	
	
	@SuppressWarnings("unchecked")
	private void createFunctionCheckers()
	{
		this.elementCheckers = new Function[] { (x) -> {return x;}, 												 
												(x) -> reduceElementsToSpec((JsonArray) x)}; 
	}
	
	
	private static JsonArray reduceElementsToSpec(JsonArray sourceArray)
	{		
		for(int i = 0; i < sourceArray.size(); i++)
		{
			JsonObject currentElement = sourceArray.getJsonObject(i);
			String currentElementType = currentElement.getString("elementType");
			switch(currentElementType)
			{
			case "task":
				currentElement.put("data",FieldSpecifications.filterFieldBySpec(currentElement.getJsonObject("data"), TaskObjectInstance.getFieldSpecs()));
				break;
			case "filter":
				currentElement.put("data",FieldSpecifications.filterFieldBySpec(currentElement.getJsonObject("data"), Filter.getFieldSpecs()));
				break;
			case "experiment":
				currentElement.put("data",FieldSpecifications.filterFieldBySpec(currentElement.getJsonObject("data"), ExperimentObjectInstance.getFieldSpecs()));
				break;
			}
		}
		return sourceArray;
	}
	
	
	/**
	 * Get all elements that are part of this experiment
	 * @return A JsonArray of element IDs
	 */
	public JsonArray getElements() {
		if(!data.containsKey("elements"))
		{
			data.put("elements", new JsonArray());
		}
		return data.getJsonArray("elements",new JsonArray());
	}
	/**
	 * Set the code
	 * TODO: Check whether this function is necessary
	 * @param elements odd... 
	 */
	public void setCode(JsonArray elements) {
		data.put("elements", elements);
	}
	
	@Override
	public void setElementProperties(Experiment experiment) {
		JsonArray currentElements = data.getJsonArray("elements",new JsonArray()); 
		for(int i = 0; i < currentElements.size(); i++)
		{
			experiment.addElement(currentElements.getJsonObject(i).getJsonObject("data").getString("UUID"));
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
			
			this.data.put(gitFields[i],elementCheckers[i].apply(json.getValue(gitFields[i], gitDefaults[i])));	
		}
	}
	
	public Function<Object,Object> getFieldFilter(String fieldName)
	{
		if(fieldName == "elements")
		{
			return (x) -> {return this.reduceElementsToSpec((JsonArray)x);};
		}
		else
		{
			return (x) -> {return x;};
		}
	}

	@Override
	public JsonObject calcDependencies() {
		HashSet<String> taskDependencies = new HashSet<>();
		HashSet<String> experimentDependencies = new HashSet<>();
		for(int i = 0 ; i < this.getElements().size(); i++)
		{
			JsonObject element = this.getElements().getJsonObject(i);
			switch(element.getString("elementType"))
			{
				case "task" : taskDependencies.add(element.getJsonObject("data").getString("UUID")); break;
				case "experiment" : experimentDependencies.add(element.getJsonObject("data").getString("UUID")); break;
				default: continue;
			}			
		}
		return new JsonObject().put("tasks", new JsonArray(new LinkedList<String>(taskDependencies)))
				.put("experiments", new JsonArray(new LinkedList<String>(experimentDependencies)));
		
	}
	
}

