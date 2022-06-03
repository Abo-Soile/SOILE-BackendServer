package fi.abo.kogni.soile2.experiment.filter.exp4j.functions;

import org.apache.commons.math3.stat.descriptive.moment.Mean;

import net.objecthunter.exp4j.function.Function;

public class MeanFunction extends Function {

	/**
	 * A general purpose mean function.
	 */
	public MeanFunction() {
		super("mean", Integer.MAX_VALUE);
	}

	@Override
	public double apply(double... args) {
			Mean mean = new Mean();
			mean.setData(args);
			return mean.evaluate();
	}

}
