package edu.stanford.cs276;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import edu.stanford.cs276.util.Pair;


public class LanguageModel implements Serializable {

	private static LanguageModel lm_;
	// Dictionary of word to wordID
	private HashMap<String, Integer> _dictionary;
	private HashMap<Integer, Double> _unigramProbs;
	private HashMap<Pair<Integer, Integer>, Double> _bigramProbs;
	private static final double LAMBDA = 0.1;
	
	
	// Do not call constructor directly since this is a Singleton
	private LanguageModel(String corpusFilePath) throws Exception {
		_dictionary = new HashMap<String, Integer>();
		_unigramProbs = new HashMap<Integer, Double>();
		_bigramProbs = new HashMap<Pair<Integer, Integer>, Double>();
		constructDictionaries(corpusFilePath);
		save();
	}


	public void constructDictionaries(String corpusFilePath)
			throws Exception {
		HashMap<Integer, Integer> unigramCounts = new HashMap<Integer, Integer>();
		HashMap<Pair<Integer, Integer>, Integer> bigramCounts = new HashMap<Pair<Integer, Integer>, Integer>();
		int _numTokens = 0;
		int _wordID = 0;

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
				String[] words = line.split(" ");
				for (int i = 0; i < words.length; i++) {
					_numTokens++;
					// Put word in dictionary
					if (!_dictionary.containsKey(words[i])) {
						_dictionary.put(words[i], _wordID++);
					}
					
					int wordID = _dictionary.get(words[i]);
					
					// Update unigram counts
					if (unigramCounts.containsKey(words[i])) {
						unigramCounts.put(wordID, unigramCounts.get(words[i]) + 1);
					} else {
						unigramCounts.put(wordID, 1);
					}
					if (i + 1 < words.length) {
						if (!_dictionary.containsKey(words[i + 1])) {
							_dictionary.put(words[i + 1], _wordID++);
						}
						int secondWordID = _dictionary.get(words[i + 1]);
						Pair<Integer, Integer> bigram = new Pair<Integer, Integer>(wordID, secondWordID);
						if (bigramCounts.containsKey(bigram)) {
							bigramCounts.put(bigram, bigramCounts.get(bigram) + 1);
						} else {
							bigramCounts.put(bigram, 1);
						}
					}
				}
			}
			input.close();
			
			// Compute probabilities from counts
			// Unigrams
			for (Entry<Integer, Integer> entry : unigramCounts.entrySet()) {
				_unigramProbs.put(entry.getKey(), (double) entry.getValue() / _numTokens);
			}
			// Bigrams
			for (Entry<Pair<Integer, Integer>, Integer> entry : bigramCounts.entrySet()) {
				Pair<Integer, Integer> bigram = entry.getKey();
				int unigramCount = unigramCounts.get(bigram.getFirst());
				_bigramProbs.put(bigram, (double) entry.getValue() / unigramCount);
			}
		}
		System.out.println("Done.");
	}
	
	/**
	 * Returns the log interpolated conditional probability of bigram.getSecond | bigram.getFirst
	 * @param bigram
	 * @return
	 */
	private double bigramProbability(String w1, String w2) {
		int w1ID = _dictionary.get(w1);
		int w2ID = _dictionary.get(w2);
		Pair<Integer, Integer> bigram = new Pair<Integer, Integer>(w1ID, w2ID);

		double unigramProbability = _unigramProbs.get(bigram.getSecond()); // P(w2)
		double bigramProbability = _bigramProbs.get(bigram); 			   // P(w2|w1)

		return Math.log(LAMBDA * unigramProbability + (1 - LAMBDA) * bigramProbability);
	}
	
	/**
	 * Returns the probability of a sequence of words (a query) given the language model.
	 * @param words
	 * @return
	 */
	public double queryProbability(String[] words) {
		double p = _unigramProbs.get(words[0]);
		for (int i = 0; i < words.length - 1; i++) {
			p += bigramProbability(words[i], words[i+1]);
		}
		
		return p;
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
