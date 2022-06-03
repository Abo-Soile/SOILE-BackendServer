package fi.abo.kogni.soile2.experiment.filter.exp4j.functions;

import org.apache.commons.math3.stat.descriptive.rank.Max;

import net.objecthunter.exp4j.function.Function;

public class MaxFunction extends Function {

	/**
	 * A general purpose mean function.
	 */
	public MaxFunction() {
		super("max", Integer.MAX_VALUE);
	}

	@Override
	public double apply(double... args) {
			Max max = new Max();
			max.setData(args);
			return max.evaluate();
	}

}
