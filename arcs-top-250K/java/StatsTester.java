import java.io.*;
import java.util.*;

public class StatsTester{

	static String list[] = new String[500000]; // list of tokens (words+tag), actual # is 404944
	static String relsFlip[] = new String[38]; 	// list of relations, just a way to flip
												// hashing done in rels
	static Hashtable<String, Integer> words = new Hashtable<String, Integer>();
	// Hashtable from a word to its first occurrence in values
	static Hashtable<Integer, String> wordsFlip = new Hashtable<Integer, String>();
	// Hashtable from a first occurrence to an actual word, just a way to flip hashing in words
	static Hashtable<String, Integer> revWords = new Hashtable<String, Integer>();
	// same as words, but for the reversed files
	static Hashtable<String, Integer> rels = new Hashtable<String, Integer>();
	// Hashtable from a relation to the value representing it
	static Hashtable<String, Integer> wordVals = new Hashtable<String, Integer>();
	// Hashtable from a word to the value representing it
	static String[] wordValsFlip = new String[310000];
	// A way to reverse hashing done in wordVals
	static Hashtable<String, Long> relCounts = new Hashtable<String, Long>();
	// Hashtable from a relation to the total number of occurrences of that relation
	static long values[] = new long[300000000]; // list of long values
	static long revValues[] = new long[300000000]; // same as values, but for the reversed files
	//static long entropies[] = new long[300000000]; 	// list of long values, with count replaced by 
													// entropy

	// these are the sizes of each value in the long, by bits
	static final int RELSIZE = 6;
	static final int WORDSIZE = 29;
	static final int COUNTSIZE = 28;
	static final int SHIFTTOREL = WORDSIZE+COUNTSIZE;
	static final int RELONES = (int) Math.pow(2,RELSIZE)-1;
	static final int WORDONES = (int) Math.pow(2,WORDSIZE)-1;
	static final int COUNTONES = (int) Math.pow(2,COUNTSIZE)-1;
	static double log2 = Math.log(2);

	public static long count(String w, String r, String w2) { 	//given two words and a relation,
																//returns # of occurrences of the
																//given pattern
		long relVal, wIndex;

		if (w.equals("*")) {
			if (w2.equals("*")) { // this is || * r * ||
				try {
					long val = relCounts.get(r);
					return val;
				} catch (NullPointerException e) {
					e.printStackTrace();
					return 0;
				}
			} else { // this is || * r w' ||
				long sum = 0;
				try {
					relVal = rels.get(r);
					wIndex = revWords.get(w2);//FIX ERRORS? (NOT NECESSARY, MIGHT SPEED IT UP A BIT)
				} catch (NullPointerException e) {
					return 0;
				}
				boolean reached = false;
				for (int i=(int)wIndex;;i++) {
					long num = revValues[i];
					if (num == 0L) continue;
					if (num == 5332341007679750154L) { // last line of long-arcs reversed 98
						sum += 10; // corresponding count in p-arcs reversed 98
						return sum;
					}
					long test = (num >> SHIFTTOREL);
					if (test == relVal) {
						sum += ((int)num & COUNTONES);
						reached = true;
					} else if ((test > relVal) ||
								(test < relVal && reached)) return sum;
				}
			}
		} else {
			if (w2.equals("*")) { // this is || w r * ||
				long sum = 0;
				try {
					relVal = rels.get(r);
					wIndex = words.get(w);
				} catch (NullPointerException e) {
					e.printStackTrace();
					return 0;
				}
				boolean reached = false;
				for (int i=(int)wIndex;;i++) {
					long num = values[i];
					if (num == 0L) continue;
					long test = (num >> SHIFTTOREL);
					if (test == relVal) {
						sum += ((int)num & COUNTONES);
						reached = true;
					} else if ((test > relVal) ||
								(test < relVal && reached)) return sum;
				}
			} else { // this is || w r w' ||
				int wi;
				try {
					relVal = rels.get(r);
					wi = words.get(w);
				} catch (NullPointerException e) {
					e.printStackTrace();
					return 0L;
				}
				for (int i=wi;;i++) {
					long num = values[i];
					if (num == 0L) continue;
					long test = relVal * (long)(Math.pow(2,WORDSIZE)) + wordVals.get(w2);
					if ((num >> COUNTSIZE) == test) return (long)((int)num & COUNTONES);
					else if ((num >> SHIFTTOREL) > relVal) return 0L;
				}
			}
		}
	}

	public static void main(String args[]) {

		try {
			String s;
			int count = 0;

			BufferedReader indices = new BufferedReader(new FileReader("../p-arcs/final/"+
																	"final-indices.txt"));

			while ((s = indices.readLine()) != null) {
				int n = s.indexOf('\t');
				String word = s.substring(0,n);
				list[count] = word;
				count++;

				int temp = Integer.parseInt(s.substring(n+1));
				words.put(word,temp);
				wordsFlip.put(temp,word);
			}
			indices.close();

			// creates list of tokens
			System.out.println("\nList of tokens created. Length: " + count);

			indices = new BufferedReader(new FileReader("../word-values.txt"));

			count = 0;
			while ((s = indices.readLine()) != null) {
				wordVals.put(s,count);
				wordValsFlip[count] = s;
				count++;
			}
			indices.close();

			indices = new BufferedReader(new FileReader("../p-arcs/final/final-indices"+
																	"-r.txt"));
			count = 0;

			while ((s = indices.readLine()) != null) {
				int n = s.indexOf('\t');
				String word = s.substring(0,n);
				revWords.put(word,Integer.parseInt(s.substring(n+1)));
				count++;
			}
			indices.close();

			BufferedReader relations = new BufferedReader(new FileReader("../p-arcs/relcounts."+
																	"txt"));

			count = 0;

			while ((s = relations.readLine()) != null) {
				String rel = s.substring(0,s.indexOf('\t'));
				rels.put(rel,count);
				relsFlip[count] = rel;
				System.out.println("rels["+rel+"] = "+count);
				relCounts.put(rel,Long.parseLong(s.substring(s.indexOf('\t')+1)));
				count++;
			}
			relations.close();
			// creates hashtable of relations and their index (their order of appearance in
			//														rel-counts-total.txt)
			System.out.println("Hashtable of relations created. Length: " + rels.size());

			count = 0;
			int index = 0;

			System.out.print("\nopening files:");
			for (int i=0; i<99; i++) {

				String file = String.format("../long-arcs/final/long-arcs.purged.%02d.txt",i);
				if (i%10==0) System.out.println();
				System.out.print(String.format(" %02d /", i));
				BufferedReader vals = new BufferedReader(new FileReader(file));

				while ((s = vals.readLine()) != null) {
					if (s.equals("")) { //Empty line, we've reached end of this token
						continue;
					} else if (s.equals("X")) {//Disregarded, second word isn't in top 250K
						values[count] = 0L;
					} else {
						values[count] = Long.parseLong(s);
					}
					count++;
				}
				vals.close();
				//creates arrays of long values, where each value is (rels.get(r)*(2^40)+
				//												words.get(w)*(2^8) + c)
			}

			count = 0;
			index = 0;

			System.out.print("\nopening reversed files:");
			for (int i=0; i<99; i++) {

				String file = String.format("../long-arcs/final/reversed/long-arcs.reversed."+
																		"%02d.txt",i);
				if (i%10==0) System.out.println();
				System.out.print(String.format(" %02d /", i));
				BufferedReader vals = new BufferedReader(new FileReader(file));

				while ((s = vals.readLine()) != null) {
					if (s.equals("")) { //Empty line, we've reached end of this token
						continue;
					} else if (s.equals("X")) {//Disregarded, second word isn't in top 250K
						revValues[count] = 0L;
					} else {
						revValues[count] = Long.parseLong(s);
					}
					count++;
				}
				vals.close();
				//creates arrays of long values, where each value is (rels.get(r)*(2^40)+
				//												words.get(w)*(2^8) + c)
			}

			//precompute();

			System.out.println("\n\nReplacing counts with mutual information");

		} catch (Exception e) {
			e.printStackTrace();
		}

		Scanner s = new Scanner(System.in);
		String input = "";
		while (true) {
			System.out.print("\n\nEnter \"[w1] [rel] [w2]\", or \'quit\' to quit: ");
			input = s.nextLine();
			if (input.equalsIgnoreCase("quit")) break;
			list = input.split(" ");
			try {
				System.out.print(count(list[0],list[1],list[2]));
			} catch (Exception e) {}
		}
	}
}