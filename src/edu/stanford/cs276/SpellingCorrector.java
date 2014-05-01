package edu.stanford.cs276;

import java.util.Map;
import java.util.Set;

import edu.stanford.cs276.util.Pair;

public class SpellingCorrector {
	private LanguageModel _lm;
	private NoisyChannelModel _ncm;
	private CandidateGenerator _cg;
	private static final double MU = 1;
	
	public SpellingCorrector(LanguageModel lm, NoisyChannelModel ncm) {
		_lm = lm;
		_ncm = ncm;
		try {
			_cg = CandidateGenerator.get();
			_cg.setLanguageModel(_lm);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public double scoreCandidate(String candidate, String origQuery, int distance) {
		return _lm.queryProbability(candidate) * MU + _ncm.getEditProbability(candidate, origQuery, distance);
	}
	
	public String bestCorrection(String query) {
		Map<String, Integer> candidates = null;
		try {
			candidates = _cg.getCandidates(query);
		} catch (Exception e) {
			e.printStackTrace();
		}
		double maxScore = Double.NEGATIVE_INFINITY;
		String bestMatch = null;
		for (String candidate : candidates.keySet()) {
			double score = scoreCandidate(candidate, query, candidates.get(candidate));
			if (score > maxScore) {
				maxScore = score;
				bestMatch = candidate;
			}
		}
//		System.out.println("best match: " + bestMatch + " score: " + maxScore);
		
		return bestMatch;
	}

}
