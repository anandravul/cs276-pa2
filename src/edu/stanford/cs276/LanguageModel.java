package edu.stanford.cs276;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import edu.stanford.cs276.util.Dictionary;


public class LanguageModel implements Serializable {

	private static LanguageModel lm_;
	private Dictionary _unigramCounts;
	private Dictionary _bigramCounts;
	private static final double LAMBDA = 0.01;
	
	
	// Do not call constructor directly since this is a Singleton
	private LanguageModel(String corpusFilePath) throws Exception {
		_bigramCounts = new Dictionary();
		_unigramCounts = new Dictionary();
		constructDictionaries(corpusFilePath);
		save();
	}


	/**
	 * Reads in corpus and constructs dictionaries of unigram and bigram counts.
	 * @param corpusFilePath
	 * @throws Exception
	 */
	public void constructDictionaries(String corpusFilePath)
			throws Exception {

		System.out.println("Constructing dictionaries...");
		File dir = new File(corpusFilePath);
		for (File file : dir.listFiles()) {
			if (".".equals(file.getName()) || "..".equals(file.getName())) {
				continue; // Ignore the self and parent aliases.
			}
			System.out.printf("Reading data file %s ...\n", file.getName());
			BufferedReader input = new BufferedReader(new FileReader(file));
			String line = null;
			while ((line = input.readLine()) != null) {
				String[] words = line.trim().split(" ");
				for (int i = 0; i < words.length; i++) {
					// Put word in dictionary
					_unigramCounts.add(words[i]);
					
					if (i + 1 < words.length) {
						_bigramCounts.add(words[i] + " " + words[i + 1]);
					}
				}
			}
			input.close();
		}
		System.out.println("Done.");
	}
	
	/**
	 * Computes the probability of a term as count(term)/termCount
	 * @param w
	 * @return
	 */
	private double unigramProbability(String w) {
		return (double) _unigramCounts.count(w) / _unigramCounts.termCount();
	}
	
	/**
	 * Returns the log interpolated conditional probability of w2 | w1
	 * @param bigram
	 * @return
	 */
	private double bigramProbability(String w1, String w2) {
		double unigramProbability = unigramProbability(w1); // P(w2)
		double bigramProbability =  (double) _bigramCounts.count(w1 + " " + w2) / _unigramCounts.count(w1); 			   // P(w2|w1)

		return Math.log(LAMBDA * unigramProbability + (1 - LAMBDA) * bigramProbability);
	}
	
	/**
	 * Returns the probability of a sequence of words (a query) given the language model.
	 * @param words
	 * @return
	 */
	public double queryProbability(String query) {
		String[] words = query.trim().split(" ");
		double p = Math.log(unigramProbability(words[0]));
		for (int i = 0; i < words.length - 1; i++) {
			// Assign 0 probability to queries with words not in dictionary.
			if (_unigramCounts.count(words[i]) == 0 || _unigramCounts.count(words[i + 1]) == 0) {
				return Double.NEGATIVE_INFINITY;
			}
			p += bigramProbability(words[i], words[i+1]);
		}
		
		return p;
	}
	
	/**
	 * Returns true if all words in the query are in the dictionary.
	 * @param query
	 * @return
	 */
	public boolean isValidQuery(String query) {
		String[] words = query.trim().split(" ");
		for (String word : words) {
			if (_unigramCounts.count(word) == 0) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Returns true if the candidate has n or fewer invalid words.
	 * @param query
	 * @return
	 */
	public boolean hasNOrFewerInvalidWords(String query, int n) {
		int errorCount = 0;
		String[] words = query.trim().split(" ");
		for (String word : words) {
			if (_unigramCounts.count(word) == 0) {
				errorCount++;
			}
			if (errorCount > n) {
				return false;
			}
		}
		
		return true;
	}
	
	// Loads the object (and all associated data) from disk
	public static LanguageModel load() throws Exception {
		try {
			if (lm_==null){
				FileInputStream fiA = new FileInputStream(Config.languageModelFile);
				ObjectInputStream oisA = new ObjectInputStream(fiA);
				lm_ = (LanguageModel) oisA.readObject();
			}
		} catch (Exception e){
			throw new Exception("Unable to load language model.  You may have not run build corrector");
		}
		return lm_;
	}
	
	// Saves the object (and all associated data) to disk
	public void save() throws Exception{
		FileOutputStream saveFile = new FileOutputStream(Config.languageModelFile);
		ObjectOutputStream save = new ObjectOutputStream(saveFile);
		save.writeObject(this);
		save.close();
	}
	
	// Creates a new lm object from a corpus
	public static LanguageModel create(String corpusFilePath) throws Exception {
		if(lm_ == null ){
			lm_ = new LanguageModel(corpusFilePath);
		}
		return lm_;
	}
	
}
