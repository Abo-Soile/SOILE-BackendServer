package fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl;

import com.fasterxml.jackson.annotation.JsonProperty;

import fi.abo.kogni.soile2.projecthandling.participant.Participant;
import fi.abo.kogni.soile2.projecthandling.projectElements.Task;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.ElementInstanceBase;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.ProjectInstance;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class TaskObjectInstance extends ElementInstanceBase {	
	    
	  public TaskObjectInstance(JsonObject data, ProjectInstance source)
	  {
		  super(data, source);
	  }
	    	  
	    
	  @JsonProperty("filter")
	  public String getFilter() {
	    return data.getString("filter");
	  }
	  public void setFilter(String filter) {
		  data.put("filter",filter);
	  }

	    
	  @JsonProperty("outputs")
	  public JsonArray getOutputs() {
	    return data.getJsonArray("outputs");
	  }
	  public void setOutputs(JsonArray outputs) {
		  data.put("outputs",outputs);
	  }


	@Override
	public String nextTask(Participant user) {
		// this indicates, that the user currently was on this task, so lets ake the next.
		if(user.getProjectPosition().equals(getInstanceID()))
		{
			return sourceProject.getElement(this.getNext()).nextTask(user);
		}
		// If this has a filter and the user matches the filter, return the next task.
		// We will also set the current position to this.
		if(getFilter() != null && !Filter.userMatchesFilter(getFilter(), user))
		{			
			// We have done this, but it is still the position we are currently at.
			user.setProjectPosition(this.getInstanceID());			
			return sourceProject.getElement(getNext()).nextTask(user); 
		}
		// otherwise we are here at this task!
		return this.getInstanceID();
	}


}
