package edu.stanford.cs276;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Scanner;

import edu.stanford.cs276.util.Dictionary;
import edu.stanford.cs276.util.Pair;

public class EmpiricalCostModel implements EditCostModel{
	private static final double ZERO_EDIT_LOGP = Math.log(0.95);

	Character[] alphabet = CandidateGenerator.alphabet;
	private Dictionary[] counts;
	private static int DEL = 0, INS = 1, SUB = 2, TRANS = 3, COUNT = 4;;
	
	public EmpiricalCostModel(String editsFile) throws IOException {
		BufferedReader input = new BufferedReader(new FileReader(editsFile));
		System.out.println("Constructing edit distance map...");
		//Init dictionaries [delete, insert, sub, trans, COUNT]
		counts = new Dictionary[]{new Dictionary(),new Dictionary(),new Dictionary(),new Dictionary(),new Dictionary()};

		String line = null;
		while ((line = input.readLine()) != null) {
			Scanner lineSc = new Scanner(line);
			lineSc.useDelimiter("\t");
			String noisy = lineSc.next();
			String clean = lineSc.next();

			//Continue if they are the same
			if (noisy.equals(clean)) continue;

			//Determine the type of error and record its count
			Pair<Integer,String> edit = new Pair<Integer,String>(0,"");
			findEdits(clean,noisy,edit);
			counts[edit.getFirst()].add(edit.getSecond());
			//Add each unigram and bigram for the correct query
			counts[COUNT].add("#"+clean.substring(0,1));
			for (int i=0;i<clean.length()-1;i++){
				counts[COUNT].add(clean.substring(i,i+1));
				counts[COUNT].add(clean.substring(i,i+2));
			}
			counts[COUNT].add(clean.substring(clean.length()-1,clean.length()));
			lineSc.close();
		}
		input.close();
		System.out.println("Done.");
	}


	// You need to update this to calculate the proper empirical cost
	@Override
	public double editProbability(String original, String R, int distance) {
		if (R.equals(original)) return ZERO_EDIT_LOGP;
		Pair<Integer,String> edit = new Pair<Integer,String>(0,"");
		Pair<Integer,Integer> idx = findEdits(original,R,edit);
		//Determine the type of edit and get its counts
		double cost = editCost(edit);
		if (distance>1 && !original.substring(idx.getFirst()).equals(R.substring(idx.getSecond()))){
			findEdits(original.substring(idx.getFirst()),R.substring(idx.getSecond()),edit);
			cost+=editCost(edit);	
		}
		return cost;
	}
	
	private double editCost(Pair<Integer,String> edit){
		double denom;
		switch (edit.getFirst()){
			case 1:
			case 2:
				denom = counts[COUNT].count(edit.getSecond().substring(0,1));
				break;
			default:
				denom = counts[COUNT].count(edit.getSecond());
		}
		return Math.log(((double) counts[edit.getFirst()].count(edit.getSecond())+1) / (denom + alphabet.length));
	}

	//Find the first edit that turns original into R, returns a tuple with editType, originalstring position
	private Pair<Integer,Integer> findEdits(String _actual, String _typed, Pair<Integer,String> edit) {
		//Prepend string start char
		String actual = new String("#" + _actual);
		String typed = new String("#" + _typed);
		int i=0;
		while(i < Math.min(actual.length(), typed.length()) && actual.charAt(i) == typed.charAt(i)){
			i++;
		}
		if (actual.length() < typed.length()){
			//Insertion
			edit.setFirst(INS);
			edit.setSecond(actual.substring(i-1, i) + typed.substring(i,i+1));
			return new Pair<Integer,Integer>(i-1,i);
		} else if (actual.length() > typed.length()){
			//Deletion
			edit.setFirst(DEL);
			edit.setSecond(actual.substring(i-1, i) + actual.substring(i,i+1));
			return new Pair<Integer,Integer>(i,i-1);
		} else if (i+1 < actual.length() && typed.charAt(i) == actual.charAt(i+1) && typed.charAt(i+1) == actual.charAt(i) ) {
			//Transposition
			edit.setFirst(TRANS);
			edit.setSecond(actual.substring(i, i+1) + typed.substring(i+1,i+2));
			return new Pair<Integer,Integer>(i+1,i+1);
		} else {
			//Substitution
			edit.setFirst(SUB);
			edit.setSecond(typed.substring(i, i+1)+ actual.substring(i,i+1));
			return new Pair<Integer,Integer>(i,i);
		}
	}
}
