package fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonProperty;

import fi.abo.kogni.soile2.projecthandling.participant.Participant;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.ElementInstance;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.ElementInstanceBase;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.ProjectInstance;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * An {@link ElementInstance} represeting a Task
 * @author Thomas Pfau
 *
 */
public class TaskObjectInstance extends ElementInstanceBase {	
	    
	
	  static final Logger LOGGER = LogManager.getLogger(TaskObjectInstance.class);

	  public TaskObjectInstance(JsonObject data, ProjectInstance source)
	  {
		  super(data, source);
	  }
	    	  
	    
	  /**
	   * The Filter that needs to be passed to use this Task.
	   * @return
	   */
	  @JsonProperty("filter")
	  public String getFilter() {
	    return data.getString("filter");
	  }
	  /**
	   * Set the Filter that needs to be passed to use this Task.
	   * @param filter the filter formula
	   */	  
	  public void setFilter(String filter) {
		  data.put("filter",filter);
	  }

	  /**
	   * The type of code used in this task (elang, qmarkup, Javascript)
	   * @return
	   */
	  @JsonProperty("codeType")
	  public JsonObject getCodeType() {
	    return data.getJsonObject("codeType");
	  }
	  
	  /**
	   * Set the type of code used in this task (elang, qmarkup, Javascript)
	   * @param type The code type
	   */
	  public void setCodeType(JsonObject type) {
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

	    
	  /**
	   * The names of the outputs generated by this task
	   * @return
	   */
	  @JsonProperty("outputs")
	  public JsonArray getOutputs() {
	    return data.getJsonArray("outputs");
	  }
	  
	  /**
	   * Set the outputs generated by this task
	   * @return
	   */
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

	public static FieldSpecifications getFieldSpecs()
	{
		
		return new FieldSpecifications().put(new FieldSpecification("UUID", String.class, String::new, false))
										.put(new FieldSpecification("name", String.class, () -> "", true))
										.put(new FieldSpecification("tag", String.class, () -> "", true))
										.put(new FieldSpecification("version", String.class, () -> "", false))
										.put(new FieldSpecification("instanceID", String.class, String::new, false))										
										.put(new FieldSpecification("filter", String.class, () -> "", true))
										.put(new FieldSpecification("outputs", JsonArray.class, JsonArray::new, true))
										.put(new FieldSpecification("codeType", JsonObject.class, () -> new JsonObject().put("language", "").put("version", ""), false))
										.put(new FieldSpecification("next", String.class, () -> "end", false));
										
	}
}
