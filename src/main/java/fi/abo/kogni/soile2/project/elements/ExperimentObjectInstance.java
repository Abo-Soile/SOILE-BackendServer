package fi.abo.kogni.soile2.project.elements;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import com.fasterxml.jackson.annotation.JsonProperty;

import fi.abo.kogni.soile2.project.instance.ProjectInstance;
import fi.abo.kogni.soile2.project.participant.Participant;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class ExperimentObjectInstance extends ProjectDataBaseObjectInstance{

	Random rand = new Random();
	List<String> elementIDs;
	
	public ExperimentObjectInstance(JsonObject data, ProjectInstance source) {
		super(data, source);
		// TODO Auto-generated constructor stub		
		defineElements();		
	}

	@JsonProperty("start")
	public String getStart() {
		return data.getString("start");
	}
	
	public void setStart(String start) {
		data.put("start", start);
	}
	
	@JsonProperty("random")
	public Boolean getRandom() {
		return data.getBoolean("random");
	}
	public void setRandom(Boolean random) {
		data.put("random",random);
	}

	@JsonProperty("elements")
	public JsonArray getElements() {
		return data.getJsonArray("elements");
	}
	public void setElements(JsonArray elements) {
		data.put("elements",elements);	
		defineElements();
	}
	
	/**
	 * 
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
			System.out.println("Already in Experiment, finishing up, due to non-random");
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
				System.out.println("Already in Experiment, finishing up, due to non-random");
				// we are non random, so give the first task.
				return sourceProject.getElement(getStart()).nextTask(user);	
			}
			else
			{
				// remove option that are already done
				elements.removeAll(user.getFinishedExpTasks(this.getInstanceID()));
				// this indicates, that we got a callback from a task in our list.
				System.out.println(elements);
				if(elements.remove(user.getProjectPosition()))
				{
					System.out.println("Removing element from options");
					// we add the task to the finished tasks for this experiment.				
					user.addFinishedExpTask(getInstanceID(), user.getProjectPosition());
				}
				// we still have elements, so we return the next element as specified in one random element of this task.
				if(elements.size() > 0)
				{
					System.out.println("Returning a random element");
					return sourceProject.getNextTask(elements.get(rand.nextInt(elements.size())),user);
				}
				else
				{
					System.out.println("Everything done in random experiment, returning next");
					// get the the next element in the project.
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

}
