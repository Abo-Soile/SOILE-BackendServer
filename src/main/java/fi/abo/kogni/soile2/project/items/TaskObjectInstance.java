package fi.abo.kogni.soile2.project.items;

import com.fasterxml.jackson.annotation.JsonProperty;

import fi.abo.kogni.soile2.project.Filter;
import fi.abo.kogni.soile2.project.instance.ProjectInstance;
import fi.abo.kogni.soile2.project.participant.impl.DBParticipant;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class TaskObjectInstance extends ProjectDataBaseObjectInstance {	
	    
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
	public String nextTask(DBParticipant user) {
		// If the user either already finished this task OR there is a filter and the user does not fulfil it, we skip to the next task.
		if(user.finished(getInstanceID()) || getFilter() != null && !Filter.userMatchesFilter(getFilter(), user))
		{
			user.finishCurrentTask();
			return sourceProject.getElement(getNext()).nextTask(user); 
		}
		// otherwise we are here at this task!
		return this.getInstanceID();
	}


}
