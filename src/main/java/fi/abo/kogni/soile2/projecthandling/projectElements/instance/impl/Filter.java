package fi.abo.kogni.soile2.projecthandling.projectElements.instance.impl;

import java.util.HashMap;

import fi.aalto.scicomp.mathparser.MathHandler;
import fi.abo.kogni.soile2.projecthandling.participant.Participant;
import io.vertx.core.json.JsonObject;


public class Filter {
	
	public static boolean userMatchesFilter(String Filter, Participant user)
	{
		// a positive evaluation will lead to a value of one. 
		double val = MathHandler.evaluate(Filter, user.getOutputs());
		// and we leave some tolerance due to numerical issues.
		return Math.abs(val - 1) < 0.0001;
	}	
	
	public static String testFilterExpression(String FilterString , JsonObject Values)
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
}
