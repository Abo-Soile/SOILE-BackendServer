package fi.abo.kogni.soile2.experiment.filter.exp4j.functions;

import org.apache.commons.math3.stat.descriptive.rank.Min;

import net.objecthunter.exp4j.function.Function;

public class MinFunction extends Function {

	/**
	 * A general purpose mean function.
	 */
	public MinFunction() {
		super("min", Integer.MAX_VALUE);
	}

	@Override
	public double apply(double... args) {
			Min min = new Min();
			min.setData(args);
			return min.evaluate();
	}

}
