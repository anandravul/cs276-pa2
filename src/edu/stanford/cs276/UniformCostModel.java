package edu.stanford.cs276;

public class UniformCostModel implements EditCostModel {
	private static final double UNIFORM_LOGP = Math.log(0.01);
	private static final double ZERO_EDIT_LOGP = Math.log(0.95);
	
	
	@Override
	public double editProbability(String original, String R, int distance) {
		return distance > 0 ? distance * UNIFORM_LOGP : ZERO_EDIT_LOGP;
	}
}
