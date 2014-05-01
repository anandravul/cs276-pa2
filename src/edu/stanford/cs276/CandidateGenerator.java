package edu.stanford.cs276;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import edu.stanford.cs276.util.Pair;

public class CandidateGenerator implements Serializable {
	private LanguageModel _lm;

	private static CandidateGenerator cg_;

	// Don't use the constructor since this is a Singleton instance
	private CandidateGenerator() {}

	public static CandidateGenerator get() throws Exception{
		if (cg_ == null ){
			cg_ = new CandidateGenerator();
		}
		return cg_;
	}

	public void setLanguageModel(LanguageModel lm) {
		_lm = lm;
	}

	public static final Character[] alphabet = {
					'a','b','c','d','e','f','g','h','i','j','k','l','m','n',
					'o','p','q','r','s','t','u','v','w','x','y','z',
					'0','1','2','3','4','5','6','7','8','9',
					' ',',', ' '};

	/**
	 * Generates a set of candidates within 1 edit of the input query. Candidates also must have
	 * no more than 2 - `edits` words that are not in the language model dictionary. This is
	 * because we assume no more than 2 edits per query. There is no need to consider queries with
	 * more than 1 invalid word when generating the set of candidates within 1 of the original query;
	 * likewise there's no need to consider candidates with more than 0 invalid words when consider
	 * candidates within 2 edits of the original.
	 * @param query
	 * @param edits - edit distance between query and the *original* query + 1
	 * @return
	 */
	public Set<Pair<String, Integer>> getSingleEditCandidates(String query, int edits) {
		Set<Pair<String, Integer>> candidates = new HashSet<Pair<String, Integer>>();	

		for (int i = 0; i < query.length(); i++) {
			StringBuilder cand = new StringBuilder(query);
			for (char c : alphabet) {
				if (query.charAt(i) != c) {
					// Insertion
					cand.insert(i, c);
					if (_lm.hasNOrFewerInvalidWords(cand.toString(), 0)) {
						candidates.add(new Pair<String, Integer>(cand.toString().trim().replace("  ", " "), edits));
					}
					cand.deleteCharAt(i);	

					// Substitution
					char origChar = cand.charAt(i);
					cand.setCharAt(i, c);
					if (_lm.hasNOrFewerInvalidWords(cand.toString(), 0)) {
						candidates.add(new Pair<String, Integer>(cand.toString().trim().replace("  ", " "), edits));
					}
					cand.setCharAt(i, origChar);
				}
			}
			// Deletion
			cand.deleteCharAt(i);
			if (_lm.hasNOrFewerInvalidWords(cand.toString(), 0)) {
				candidates.add(new Pair<String, Integer>(cand.toString().trim().replace("  ", " "), edits));
			}
		}

		// Transposition
		for (int i = 0; i < query.length() - 1; i++) {
			StringBuilder cand = new StringBuilder(query);
			// Substitutions
			char c1 = cand.charAt(i);
			char c2 = cand.charAt(i + 1);
			cand.setCharAt(i, c2);
			cand.setCharAt(i + 1, c1);
			if (_lm.hasNOrFewerInvalidWords(cand.toString(), 0)) {
				candidates.add(new Pair<String, Integer>(cand.toString().trim().replace("  ", " "), edits));
			}
		}

		return candidates;
	}

	// Generate all candidates w/in edit distance 2 of the target query
	public Set<Pair<String, Integer>> getCandidates(String query) throws Exception {
		Set<Pair<String, Integer>> candidates = new HashSet<Pair<String, Integer>>();
		if (_lm.isValidQuery(query)) {
			candidates.add(new Pair<String, Integer>(query, 0));
		}

		Set<Pair<String, Integer>> singleEdits = getSingleEditCandidates(query, 1);
		candidates.addAll(singleEdits);


		// Edit distance of 2
		for (Pair<String, Integer> cand : singleEdits) {
			candidates.addAll(getSingleEditCandidates(cand.getFirst(), 2));
		}

		return candidates;
	}

}