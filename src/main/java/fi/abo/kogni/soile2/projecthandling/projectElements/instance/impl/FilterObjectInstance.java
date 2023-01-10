package fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import fi.aalto.scicomp.mathparser.MathHandler;
import fi.abo.kogni.soile2.projecthandling.participant.Participant;
import fi.abo.kogni.soile2.projecthandling.participant.DataParticipant;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.ElementInstance;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.ElementInstanceBase;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.ProjectInstance;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * 
 * @author thomas
 *
 */
public class FilterObjectInstance extends ElementInstanceBase {

	List<String> options = new LinkedList<>();
	HashMap<String,String> optionsNext = new HashMap<String,String>();
	String defaultNext; 
	public FilterObjectInstance(JsonObject data, ProjectInstance source) {
		super(data, source);
		parseOptions(data.getJsonArray("options"));
		defaultNext = data.getString("defaultNext");
	}
	
	private void parseOptions(JsonArray options)
	{
		// TODO: This can be done a lot more efficient by building the expression in the beginning, extracting only the relevant variables from the user etc pp. 
		// currently all data is extracted and computed.
		for(Object o : options)
		{
			JsonObject jo = (JsonObject) o;	
			this.options.add(jo.getString("filter"));
			optionsNext.put(jo.getString("filter"),jo.getString("next"));			
		}
	}
	
	@Override
	public String nextTask(Participant user) {
		for(String exp : options)
		{			
			double val = MathHandler.evaluate(exp, user.getOutputs());
			if(Math.abs(val - 1) < 0.0001)
			{
				return getNextIfThereIsOne(user, optionsNext.get(exp));
			}
		}
		return getNextIfThereIsOne(user, defaultNext);		
	}

}
