package fi.abo.kogni.soile2.experiment.filter;

import java.util.HashMap;

import fi.abo.kogni.soile2.experiment.filter.exp4j.SoileExpression;
import fi.abo.kogni.soile2.experiment.filter.exp4j.SoileExpressionBuilder;
import net.objecthunter.exp4j.operator.Operator;

public class MathHandler {
	
	public double evaluate(String expression, HashMap<String,Double> values)
	{
		SoileExpressionBuilder eb = new SoileExpressionBuilder(expression);
		Operator gt = new Operator(">", 2, true, Operator.PRECEDENCE_ADDITION - 1) {

	        @Override
	        public double apply(double[] values) {
	            if (values[0] > values[1]) {
	                return 1d;
	            } else {
	                return 0d;
	            }
	        }
	    };
	    eb.operator(gt);
		for(String var  : values.keySet())
		{
			eb.variable(var);
		}
		SoileExpression exp = eb.build();
		for(String var  : values.keySet())
		{
			exp.setVariable(var,values.get(var));
		}
		return exp.evaluate();
	}	

}
