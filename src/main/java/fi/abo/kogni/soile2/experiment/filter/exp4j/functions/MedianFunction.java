package fi.abo.kogni.soile2.experiment.filter.exp4j.functions;

import org.apache.commons.math3.stat.descriptive.rank.Median;

import net.objecthunter.exp4j.function.Function;

public class MedianFunction extends Function {

	/**
	 * A general purpose median function.
	 */
	public MedianFunction() {
		super("median", Integer.MAX_VALUE);
	}

	@Override
	public double apply(double... args) {
			Median median = new Median();
			median.setData(args);
			return median.evaluate();
	}

}
