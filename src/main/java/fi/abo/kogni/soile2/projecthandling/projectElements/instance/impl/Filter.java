package fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl;

import java.util.HashMap;

import fi.aalto.scicomp.mathparser.MathHandler;
import fi.abo.kogni.soile2.projecthandling.participant.Participant;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;


/**
 * A Filter that allows checking whether a user matches or not. 
 * @author Thomas Pfau
 *
 */
public class Filter {
	
	/**
	 * Test if a Given {@link Participant} matches the filter. The filter is a Formula that has variables reflected by the outputs of the participant.
	 * @param Filter The Formula (needs to be parseable)
	 * @param user The {@link Participant} to check the filter for
	 * @return whether the user matches or not
	 */
	public static boolean userMatchesFilter(String Filter, Participant user)
	{
		// a positive evaluation will lead to a value of one. 
		double val = MathHandler.evaluate(Filter, user.getOutputs());
		// and we leave some tolerance due to numerical issues.
		return Math.abs(val - 1) < 0.0001;
	}	
	
	/**
	 * Test the filter expression with a given Set of values as JsonObject. 
	 * Returns success if valid (with the variables) or the message of the error that was thrown.
	 * @param FilterString the String to test
	 * @param Values example values.
	 * @return whether the expression is valid
	 */
	public static String isFilterExpressionValid(String FilterString , JsonObject Values)
	{
		
		try
		{			
			HashMap<String,Double> variables = new HashMap<String, Double>();
			for(String task : Values.fieldNames())
			{
				JsonObject taskOutputs = Values.getJsonObject(task); 
				for(String output : taskOutputs.fieldNames())
				{
					variables.put(task + "." + output, taskOutputs.getDouble(output));
				}
			}
			MathHandler.evaluate(FilterString, variables);
		}
		catch(Exception e)
		{
			return e.toString();
		}
		return "Success";
	}
	
	/**
	 * Get the Specifications for a Filter 
	 * @return the {@link FieldSpecifications} for a filter
	 */
	public static FieldSpecifications getFieldSpecs()
	{
		return new FieldSpecifications().put(new FieldSpecification("instanceID", String.class, String::new, false))
										.put(new FieldSpecification("options", JsonArray.class, JsonArray::new, true))
										.put(new FieldSpecification("defaultOption", String.class, () -> "end", false))
										.put(new FieldSpecification("position", JsonObject.class, () -> new JsonObject().put("x", 0).put("y", 0), true));

	}
	
}
