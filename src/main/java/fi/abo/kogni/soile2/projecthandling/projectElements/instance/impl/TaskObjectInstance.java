package fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonProperty;

import fi.abo.kogni.soile2.projecthandling.participant.Participant;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.ElementInstanceBase;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.ProjectInstance;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class TaskObjectInstance extends ElementInstanceBase {	
	    
	
	  static final Logger LOGGER = LogManager.getLogger(TaskObjectInstance.class);

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

	  @JsonProperty("codeType")
	  public String getCodeType() {
	    return data.getString("codeType");
	  }
	  
	  public void setCodeType(String type) {
		  data.put("codeType",type);
	  }

	  
	  private boolean hasFilter() {
		  String elementFilter = data.getString("filter", null);
		  if(elementFilter == null || elementFilter.equals(""))
		  {
			  return false;
		  }
		  return true;
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
		// this indicates, that the user currently was on this task, so lets take the next.
		if(user.getProjectPosition().equals(getInstanceID()))
		{			
			LOGGER.debug("Returning the next task, as this one is done.");
			return getNextIfThereIsOne(user, getNext());
		}
		// If this has a filter and the user matches the filter, return the next task.
		// We will also set the current position to this.
		if(this.hasFilter() && !Filter.userMatchesFilter(getFilter(), user))
		{	
			LOGGER.debug("User doesn't match the filter for this element, returning the next one.");
			// We have done this, but it is still the position we are currently at.
			// this is a bit awkward as we actually touch the task, but since the task itself filters the data, that is ok. 
			// this is after all different to an active Filter that splits the flow. 
			user.setProjectPosition(this.getInstanceID());			
			return getNextIfThereIsOne(user, getNext());
		}
		LOGGER.debug("User matches the filter OR there is no filter, doing this task");
		// otherwise we are here at this task!
		return this.getInstanceID();
	}


}
