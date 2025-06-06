package fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import fi.aalto.scicomp.mathparser.MathHandler;
import fi.abo.kogni.soile2.projecthandling.participant.Participant;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.ElementInstanceBase;
import fi.abo.kogni.soile2.projecthandling.projectElements.instance.Study;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * A Filter Object Instance
 * @author Thomas Pfau
 *
 */
public class FilterObjectInstance extends ElementInstanceBase {

	List<String> options = new LinkedList<>();
	HashMap<String,String> optionsNext = new HashMap<String,String>();
	String defaultOption; 
	/**
	 * Defautl constructor
	 * @param data the Json Data for this Filter
	 * @param source The study this filter is in
	 */
	public FilterObjectInstance(JsonObject data, Study source) {
		super(data, source);
		parseOptions(data.getJsonArray("options"));
		defaultOption = data.getString("defaultOption");
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
	public Future<String> nextTask(Participant user) {
		for(String exp : options)
		{			
			double val = MathHandler.evaluate(exp, user.getOutputs());
			if(Math.abs(val - 1) < 0.0001)
			{
				return getNextIfThereIsOne(user, optionsNext.get(exp));
			}
		}
		return getNextIfThereIsOne(user, defaultOption);		
	}

	/**
	 * Field Specifications for a Filter Instance
	 * @return The {@link FieldSpecifications}
	 */
	public static FieldSpecifications getFieldSpecs()
	{
		return new FieldSpecifications().put(new FieldSpecification("instanceID", String.class, String::new, false))																																								
										.put(new FieldSpecification("options", JsonArray.class, JsonArray::new, false))
										.put(new FieldSpecification("defaultOption", String.class, () -> "end", false))
										.put(new FieldSpecification("position", JsonObject.class, () -> new JsonObject().put("x", 0).put("y", 0), true));
	}
	
	@Override
	public FieldSpecifications getElementSpecifications() {
		// TODO Auto-generated method stub
		return getFieldSpecs();
	}

}
