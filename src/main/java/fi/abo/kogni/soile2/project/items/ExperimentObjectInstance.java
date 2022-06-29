package fi.abo.kogni.soile2.project.items;

import java.util.Random;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

import fi.abo.kogni.soile2.http_server.userManagement.Participant;
import fi.abo.kogni.soile2.project.Project;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class ExperimentObjectInstance extends ProjectDataBaseObjectInstance {

	Random rand = new Random();
	public ExperimentObjectInstance(JsonObject data, Project source) {
		super(data, source);
		// TODO Auto-generated constructor stub		
	}

	@JsonProperty("random")
	public Boolean getRandom() {
		return data.getBoolean("random");
	}
	public void setRandom(Boolean random) {
		data.put("random",random);
	}

	@JsonProperty("tasks")
	public JsonArray getTasks() {
		return data.getJsonArray("tasks");
	}
	public void setTasks(JsonArray tasks) {
		data.put("tasks",tasks);
	}
	
	@JsonProperty("experiments")
	public JsonArray getExperiments() {
		return data.getJsonArray("experiments");
	}
	public void setExperiments(JsonArray experiments) {
		data.put("experiments",experiments);
	}

	private JsonArray getElements()
	{
		JsonArray elements = new JsonArray();
		if(data.getJsonArray("experiments") != null)
		{
			elements.addAll(data.getJsonArray("experiments"));
		}
		if(data.getJsonArray("tasks") != null)
		{
			elements.addAll(data.getJsonArray("experiments"));
		}
		return elements;
	}
	
	private int getElementCount()
	{
		int count = 0;
		if(getExperiments() != null)
		{
			count+= getExperiments().size();
		}
		if(getTasks() != null)
		{
			count+= getTasks().size();
		}
		return count;
	}
	
	private UUID getElementID(int position)
	{
		if(getExperiments() != null)
		{
			if(getExperiments().size() < position)
			{
				return UUID.fromString(getExperiments().getString(position));
			}
			else
			{
				position -= getExperiments().size(); 
			}
		}
		return UUID.fromString(getTasks().getString(position));
	}
	
	@Override
	public UUID nextTask(Participant user) {

		JsonArray elements = getElements();
		//remove all elements already finished by the user.
		for(UUID id : user.getFinisheTasks(getProject()))
		{				
			elements.remove(id.toString());
		}
		// we still have elements, so we return the next element as specified in one random element of this task.
		if(elements.size() > 0)
		{
			if(getRandom())
			{
				//if this is random, return a random remaining element
				return sourceProject.getElement(UUID.fromString(elements.getString(rand.nextInt(elements.size())))).nextTask(user);
			}
			else
			{
				// otherwise return the next element (most likely this is just generally the first, as otherwise this experiment would not be called back.
				return sourceProject.getElement(UUID.fromString(elements.getString(0))).nextTask(user);	
			}
		}
		else
		{
			// everything in this Experiment is done. Update the user and return the next element.
			user.finishElement(getUUID(), getProject());
			return sourceProject.getElement(getNext()).nextTask(user);
		}

	}

}
