package fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonProperty;

import fi.abo.kogni.soile2.projecthandling.participant.Participant;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.ElementInstanceBase;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.ProjectInstance;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * {@link ElementInstance} representing an Experiment in a Project.
 * @author Thomas Pfau
 */
public class ExperimentObjectInstance extends ElementInstanceBase{

	Random rand = new Random();
	List<String> elementIDs;
	static final Logger LOGGER = LogManager.getLogger(ExperimentObjectInstance.class);

	public ExperimentObjectInstance(JsonObject data, ProjectInstance source) {
		super(data, source);
		defineElements();		
	}

	/**
	 * Get the instanceID of the Element (Task or Experiment) within this Experiment that is the first element (if this is not random)
	 * @return The InstanceID of the start element
	 */
	@JsonProperty("start")
	public String getStart() {
		return data.getString("start", elementIDs.get(0));
	}
	
	/**
	 * Set the instance ID of the Element (Task or Experiment) within this Experiment that is the first element (if this is not random)
	 * @param start The InstanceID of the start element
	 */
	public void setStart(String start) {
		data.put("start", start);
	}
	

	/**
	 * Is this experiment randomized, default is false. 
	 * @return whether this experiments elements are randomized or not
	 */
	@JsonProperty("random")
	public Boolean getRandom() {
		return data.getBoolean("random",false);
	}
	/**
	 * Set the randomization state of this element
	 * @param random
	 */
	public void setRandom(Boolean random) {
		data.put("random",random);
	}

	/**
	 * Get the JsonArray of Elements in this experiment. 
	 * Each element is a JsonObject with individual properties depending on its type. 
	 * @return
	 */
	@JsonProperty("elements")
	public JsonArray getElements() {
		return data.getJsonArray("elements");
	}
	/**
	 * Set the JsonArray of Elements in this experiment. 
	 * Each element is a JsonObject with individual properties depending on its type. 
	 */
	public void setElements(JsonArray elements) {
		data.put("elements",elements);	
		defineElements();
	}
	
	/**
	 * Define the element instanceIDs in this experiment. 
	 */
	private void defineElements()
	{
		elementIDs = new LinkedList<String>();
		
		for(Object cElementData : data.getJsonArray("elements", new JsonArray()))
		{			
			JsonObject element = (JsonObject)cElementData;
			elementIDs.add(element.getJsonObject("data").getString("instanceID"));
		}
	}
	
	
	/**
	 * Logic here:
	 * In a random Experiment, we have callbacks for every step. So we need to mark every finished task.
	 * In a non random experiment, the final element needs to refer back to this, and if we get a callback
	 * in a non-random we proceed to the next element of this experiment. 
	 */
	@Override
	public String nextTask(Participant user) {
		List<String> elements = new ArrayList<String>(elementIDs);				
		if(user.isActiveExperiment(getInstanceID()) && !getRandom())
		{
			// we got a callback while we were in the experiment.
			// Return the next element
			LOGGER.debug("Callback while in experiment, getting next element from project");
			return getNext(user);
		}
		else
		{
			// if we haven't started yet start it.
			if(!user.isActiveExperiment(getInstanceID()))
			{
				user.addActiveExperiment(getInstanceID()); 
			}
			if(!getRandom())
			{
				// we are non random, so give the first task.
				LOGGER.debug("Not random, and just started, getting next element");
				return getNextIfThereIsOne(user, getStart());	
			}
			else
			{
				// remove option that are already done
				elements.removeAll(user.getFinishedExpTasks(this.getInstanceID()));
				// this indicates, that we got a callback from a task in our list.
				if(elements.remove(user.getProjectPosition()))
				{
					// we add the task to the finished tasks for this experiment.				
					user.addFinishedExpTask(getInstanceID(), user.getProjectPosition());
				}
				// we still have elements, so we return the next element as specified in one random element of this task.
				if(elements.size() > 0)
				{
					LOGGER.debug("Random Experiment and still options left.");
					return sourceProject.getNextTask(elements.get(rand.nextInt(elements.size())),user);
				}
				else
				{
					// get the the next element in the project.
					LOGGER.debug("Experiment finished, getting next element");
					return getNext(user);
				}			
			}				
		}
	}
	
	/**
	 * Utility function to get the next object based on the user.
	 * @param user
	 * @return
	 */
	private String getNext(Participant user)
	{
		user.endActiveExperiment(this.getInstanceID());
		return sourceProject.getNextTask(getNext(),user);
	}
	
	
	
	public static FieldSpecifications getFieldSpecs()
	{
		return new FieldSpecifications().put(new FieldSpecification("instanceID", String.class, String::new, false))
										.put(new FieldSpecification("UUID", String.class, String::new, false))
										.put(new FieldSpecification("name", String.class, () -> "", true))
										.put(new FieldSpecification("tag", String.class, () -> "", true))
										.put(new FieldSpecification("version", String.class, () -> "", false))
										.put(new FieldSpecification("elements", JsonArray.class, JsonArray::new, false))
										.put(new FieldSpecification("next", String.class, () -> "end", false))
										.put(new FieldSpecification("randomize", Boolean.class, () -> false, true));
	}
	

}
