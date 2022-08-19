package fi.abo.kogni.soile2.project.items;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

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
	
	@Override
	public String nextTask(Participant user) {
		
		List<String> elements = new ArrayList<String>(elementIDs);
		//remove all elements already finished by the user.
		for(Object id : user.getFinishedTasks())
		{				
			elements.remove(id.toString());
		}
		// we still have elements, so we return the next element as specified in one random element of this task.
		if(elements.size() > 0)
		{
			if(getRandom())
			{
				//if this is random, return a random remaining element
				return sourceProject.getElement(elements.get(rand.nextInt(elements.size()))).nextTask(user);
			}
			else
			{
				// otherwise return the next element (most likely this is just generally the first, as otherwise this experiment would not be called back.
				return sourceProject.getElement(elements.get(0)).nextTask(user);	
			}
		}
		else
		{
			// everything in this Experiment is done. Update the user and return the next element.
			user.finishCurrentTask();
			return sourceProject.getElement(getNext()).nextTask(user);
		}

	}

}
