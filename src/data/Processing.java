package data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import entailment.PredicateArgumentExtractor;
import entailment.Util;
import entailment.entityLinking.DistrTyping;
import entailment.vector.EntailGraphFactoryAggregator;
import entailment.vector.EntailGraphFactoryAggregator.TypeScheme;
import uk.co.flamingpenguin.jewel.cli.ArgumentValidationException;

public class Processing {

	static String root = "data/ent/";
	static Set<String> allPrevInstances;// instances in the levy set. Useful to
										// see if sth has been swapped!

	static {
		String fname = root + "all.txt";
		allPrevInstances = getInstances(fname);
	}

	static Set<String> getInstances(String fname) {
		Set<String> ret = new HashSet<>();
		String line = null;
		try {
			BufferedReader br = new BufferedReader(new FileReader(fname));
			while ((line = br.readLine()) != null) {
				String[] ss = line.split("\t");
				ret.add(ss[0] + "\t" + ss[1]);
			}
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ret;
	}

	// split the data randomly to train, development, test
	static void split() throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(root + "all.txt"));
		PrintWriter train = new PrintWriter(root + "train.txt");
		PrintWriter dev = new PrintWriter(root + "dev.txt");
		PrintWriter test = new PrintWriter(root + "test.txt");

		String line;
		while ((line = br.readLine()) != null) {
			double r = Math.random();
			if (r < .4) {
				train.println(line);
			} else if (r < .6) {
				dev.println(line);
			} else {
				test.println(line);
			}
		}
		br.close();
		train.close();
		dev.close();
		test.close();
	}

	// split the data based on chunks of [q_type, q_pred]
	static void split_chunks() throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(root + "all.txt"));
		PrintWriter train = new PrintWriter(root + "train1.txt");
		PrintWriter dev = new PrintWriter(root + "dev1.txt");
		PrintWriter test = new PrintWriter(root + "test1.txt");

		int ds = -1;
		String qt_qp = "";

		int[] counts = new int[] { 0, 0, 0 };
		double[] ratios = new double[] { 0, 0, 0 };
		double[] ratios_goal = new double[] { .4, .3, .3 };
		int allCount = 0;

		String line;
		while ((line = br.readLine()) != null) {

			String[] ss = StringUtils.split(line, ",");
			String this_qt_qp = ss[0].trim() + "#" + ss[1].trim();
			if (!this_qt_qp.equals(qt_qp)) {
				double r = -1;
				while (true) {
					r = Math.random();

					if (r < ratios_goal[0]) {
						ds = 0;
					} else if (r < ratios_goal[0] + ratios_goal[1]) {
						ds = 1;
					} else {
						ds = 2;
					}

					if (ratios[ds] <= ratios_goal[ds]) {
						break;
					}
				}

				qt_qp = this_qt_qp;
			}

			counts[ds]++;
			allCount++;

			for (int i = 0; i < ratios.length; i++) {
				ratios[i] = (counts[i] + 0.0) / allCount;
			}

			if (ds == 0) {
				train.println(line);
			} else if (ds == 1) {
				dev.println(line);
			} else {
				test.println(line);
			}

		}
		br.close();
		train.close();
		dev.close();
		test.close();
	}

	// split the data based on chunks of [q_type,q_pred]
	// static void split_chunks_unordered() throws IOException {
	// BufferedReader br = new BufferedReader(new FileReader(root +
	// "all_re.txt"));
	// PrintWriter train = new PrintWriter(root + "train_re.txt");
	// PrintWriter dev = new PrintWriter(root + "dev_re.txt");
	// PrintWriter test = new PrintWriter(root + "test_re.txt");
	//
	// int ds = -1;
	//
	// int[] counts = new int[] { 0, 0, 0 };
	// double[] ratios = new double[] { 0, 0, 0 };
	// double[] ratios_goal = new double[] { .4, .2, .4 };
	// int allCount = 0;
	//
	// HashMap<String, Integer> qTypeqPredToDS = new HashMap<>();
	//
	// String line;
	// while ((line = br.readLine()) != null) {
	// System.out.println(line);
	// String[] ss = StringUtils.split(line, "\t");
	// String[] qss = ss[0].split(",");
	// String[] pss = ss[1].split(",");
	// String pair1 = Util.getPredicateLemma(qss[1].trim(), false)[0] + "#" +
	// Util.normalizeArg(qss[0].trim());
	//
	// String pair2 = Util.getPredicateLemma(pss[1].trim(), false)[0] + "#" +
	// Util.normalizeArg(pss[0].trim());
	//
	// System.out.println(pair1);
	// System.out.println(pair2);
	//
	// if (qTypeqPredToDS.containsKey(pair1) &&
	// qTypeqPredToDS.containsKey(pair2)) {
	// if (qTypeqPredToDS.get(pair1) != qTypeqPredToDS.get(pair2)) {
	// System.out.println("inconsitency: " + line + " " + pair1 + " " + pair2);
	// }
	// }
	//
	// if (qTypeqPredToDS.containsKey(pair1) &&
	// !qTypeqPredToDS.containsKey(pair2)) {
	// qTypeqPredToDS.put(pair2, qTypeqPredToDS.get(pair1));
	// }
	//
	// if (qTypeqPredToDS.containsKey(pair2) &&
	// !qTypeqPredToDS.containsKey(pair1)) {
	// qTypeqPredToDS.put(pair1, qTypeqPredToDS.get(pair2));
	// }
	//
	// if (!qTypeqPredToDS.containsKey(pair1)) {// so, it won't have pair2
	// // as well
	// double r = -1;
	// while (true) {
	// r = Math.random();
	//
	// if (r < .4) {
	// ds = 0;
	// } else if (r < .6) {
	// ds = 1;
	// } else {
	// ds = 2;
	// }
	//
	// if (ratios[ds] <= ratios_goal[ds]) {
	// break;
	// }
	// }
	//
	// qTypeqPredToDS.put(pair1, ds);
	// qTypeqPredToDS.put(pair2, ds);
	// System.out.println("assign " + pair1 + " to " + ds);
	// System.out.println("assign " + pair2 + " to " + ds);
	// } else {
	// ds = qTypeqPredToDS.get(pair1);
	// System.out.println("was assigned to: " + ds + " " + line);
	// }
	//
	// counts[ds]++;
	// allCount++;
	//
	// for (int i = 0; i < ratios.length; i++) {
	// ratios[i] = (counts[i] + 0.0) / allCount;
	// }
	//
	// if (ds == 0) {
	// train.println(line);
	// } else if (ds == 1) {
	// dev.println(line);
	// } else {
	// test.println(line);
	// }
	//
	// }
	// br.close();
	// train.close();
	// dev.close();
	// test.close();
	// }

	// split the data based on chunks of [q_pred,p_pred]
	static void split_preds() throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(root + "all.txt"));
		PrintWriter train = new PrintWriter(root + "train2.txt");
		PrintWriter dev = new PrintWriter(root + "dev2.txt");
		PrintWriter test = new PrintWriter(root + "test2.txt");

		int ds = -1;

		int[] counts = new int[] { 0, 0, 0 };
		double[] ratios = new double[] { 0, 0, 0 };
		double[] ratios_goal = new double[] { .4, .2, .4 };
		int allCount = 0;

		HashMap<String, Integer> predPairToDS = new HashMap<>();

		String line;
		while ((line = br.readLine()) != null) {

			String[] ss = StringUtils.split(line, "\t");
			String[] qss = ss[0].split(",");
			String[] pss = ss[1].split(",");
			String predPair = Util.removeModals(Util.getLemma(qss[1].trim())) + "#"
					+ Util.removeModals(Util.getLemma(pss[1].trim()));
			System.out.println(predPair);
			if (!predPairToDS.containsKey(predPair)) {
				double r = -1;
				while (true) {
					r = Math.random();

					if (r < .4) {
						ds = 0;
					} else if (r < .6) {
						ds = 1;
					} else {
						ds = 2;
					}

					if (ratios[ds] <= ratios_goal[ds]) {
						break;
					}
				}

				predPairToDS.put(predPair, ds);
				System.out.println("assign " + predPair + " to " + ds);
			} else {
				ds = predPairToDS.get(predPair);
				System.out.println("was assigned to: " + ds + " " + line);
			}

			counts[ds]++;
			allCount++;

			for (int i = 0; i < ratios.length; i++) {
				ratios[i] = (counts[i] + 0.0) / allCount;
			}

			if (ds == 0) {
				train.println(line);
			} else if (ds == 1) {
				dev.println(line);
			} else {
				test.println(line);
			}

		}
		br.close();
		train.close();
		dev.close();
		test.close();
	}

	static void makeCandEnts() throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(root + "all.txt"));
		PrintWriter opCand = new PrintWriter(root + "candEnts.txt");
		String line;
		HashSet<String> allEnts = loadEntities();
		HashSet<String> dsEnts = new HashSet<>();

		while ((line = br.readLine()) != null) {
			String[] ss = line.split("\t");
			String[] rel1 = ss[0].split(",");
			String[] rel2 = ss[1].split(",");
			for (int i = 0; i < rel1.length; i++) {
				rel1[i] = rel1[i].trim();
				rel2[i] = rel2[i].trim();
			}

			if (allEnts.contains(rel1[0])) {
				dsEnts.add(rel1[0]);
			}

			if (allEnts.contains(rel1[2])) {
				dsEnts.add(rel1[2]);
			}

			if (allEnts.contains(rel2[0])) {
				dsEnts.add(rel2[0]);
			}

			if (allEnts.contains(rel2[2])) {
				dsEnts.add(rel2[2]);
			}

		}

		for (String s : dsEnts) {
			opCand.println(s);
		}

		br.close();
		opCand.close();

	}

	static void fixNEs(String fname) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(root + fname + ".txt"));
		String line;
		HashSet<String> candEnts = new HashSet<>();
		BufferedReader candBr = new BufferedReader(new FileReader(root + "candEnts.txt"));
		PrintWriter op = new PrintWriter(root + fname + "_s.txt");
		PrintWriter op2 = new PrintWriter(root + fname + "_s2.txt");

		while ((line = candBr.readLine()) != null) {
			candEnts.add(line);
		}

		while ((line = br.readLine()) != null) {
			String[] ss = line.split("\t");
			boolean swapped = isSwapped(line);
			String[] rel1;
			String[] rel2;
			if (!swapped) {
				rel1 = ss[0].split(",");
				rel2 = ss[1].split(",");
			} else {
				rel1 = ss[1].split(",");
				rel2 = ss[0].split(",");
			}

			for (int i = 0; i < rel1.length; i++) {
				rel1[i] = rel1[i].trim();
				rel2[i] = rel2[i].trim();
			}

			replaceTypeWithEnt(line, rel1, rel2);
			String[] ab = makeSentence(rel1, rel2, candEnts, " ");
			String[] ab2 = makeSentence(rel1, rel2, candEnts, ",");
			if (!swapped) {
				op.println(ab[0] + "\t" + ab[1] + "\t" + ss[2]);
				op2.println(ab2[0] + "\t" + ab2[1] + "\t" + ss[2]);
			} else {
				op.println(ab[1] + "\t" + ab[0] + "\t" + ss[2]);
				op2.println(ab2[1] + "\t" + ab2[0] + "\t" + ss[2]);
			}
		}

		candBr.close();
		br.close();
		op.close();
		op2.close();

	}

	// african country, exports, coffee coffee, is a native of, abyssinia
	// ==>
	// Abyssinia exports coffee Coffee is a native of Abyssinia
	static String[] makeSentence(String[] rel1, String[] rel2, HashSet<String> candEnts, String delim) {
		rel1[0] = capFirst(rel1[0]);
		rel2[0] = capFirst(rel2[0]);

		if (candEnts.contains(rel1[0].toLowerCase())) {
			rel1[0] = capFirstAll(rel1[0]);
		}
		if (candEnts.contains(rel2[0].toLowerCase())) {
			rel2[0] = capFirstAll(rel2[0]);
		}

		if (candEnts.contains(rel1[2].toLowerCase())) {
			rel1[2] = capFirstAll(rel1[2]);
		}
		if (candEnts.contains(rel2[2].toLowerCase())) {
			rel2[2] = capFirstAll(rel2[2]);
		}
		String s1 = rel1[0] + delim + rel1[1] + delim + rel1[2];
		String s2 = rel2[0] + delim + rel2[1] + delim + rel2[2];

		return new String[] { s1, s2 };
	}

	// america: America
	static String capFirst(String s) {
		if (s.length() == 0) {
			return s;
		}
		return s.substring(0, 1).toUpperCase() + s.substring(1);
	}

	// united states: United States
	static String capFirstAll(String s) {
		if (s.length() == 0) {
			return s;
		}
		String ret = "";
		String[] ss = s.split(" ");
		for (String t : ss) {
			ret += capFirst(t) + " ";
		}
		return ret.trim();
	}

	static HashSet<String> loadEntities() throws IOException {
		BufferedReader br = new BufferedReader(new FileReader("entToWiki.txt"));
		String line;
		HashSet<String> ret = new HashSet<>();
		while ((line = br.readLine()) != null) {
			ret.add(line.split("::")[0].toLowerCase());
		}
		br.close();
		return ret;
	}

	static void replaceTypeWithEnt(String line, String[] relp1, String[] relp2) {
		int typeIdx = -1;
		String[] rel1 = new String[3];
		String[] rel2 = new String[3];
		for (int i = 0; i < rel1.length; i++) {
			rel1[i] = removeDet(relp1[i]);
		}
		for (int i = 0; i < rel2.length; i++) {
			rel2[i] = removeDet(relp2[i]);
		}

		// for (int i = 0; i < 3; i += 2) {
		// for (int j = 0; j < 3; j += 2) {
		//
		// }
		// }

		typeIdx = 0;// typeIdx is always 0!!!

		// if (!rel1[0].equals(rel2[0]) && !rel1[0].equals(rel2[2])) {
		// typeIdx = 0;
		// } else if (!rel1[2].equals(rel2[0]) && !rel1[2].equals(rel2[2])) {
		// typeIdx = 2;
		// // Now, maybe with stem, they're equal:
		// } else if (!relp1[0].equals(relp2[0]) && !relp1[0].equals(relp2[2]))
		// {
		// typeIdx = 0;
		// } else if (!relp1[2].equals(relp2[0]) && !relp1[2].equals(relp2[2]))
		// {
		// typeIdx = 2;
		// } else {
		// System.err.println("no typeIdx: " + line);
		// }

		int fixIdx = typeIdx == 0 ? 2 : 0;

		int entIdx = -1;

		// animal, has, claws the lion, will have, no claws
		for (int i = 0; i < rel1.length; i++) {
			rel1[i] = removeNeg(rel1[i]);
		}
		for (int i = 0; i < rel2.length; i++) {
			rel2[i] = removeNeg(rel2[i]);
		}

		if (rel1[fixIdx].equals(rel2[0])) {
			entIdx = 2;
		} else if (rel1[fixIdx].equals(rel2[2])) {
			entIdx = 0;
		} else {
			System.err.println("no entIdx: " + line + " typeIdx: " + typeIdx);
			System.err.println(rel1[fixIdx] + " " + rel2[0] + " " + rel2[2]);
		}
		if (typeIdx != -1 && entIdx != -1) {
			relp1[typeIdx] = relp2[entIdx];
		}
		// System.out.println("lineId: "+fixIdx+" "+typeIdx);

	}

	static void extractRelationsCCG(String fname) throws IOException {

		BufferedReader br = new BufferedReader(new FileReader(root + fname + "_s.txt"));
		BufferedReader brOrig = new BufferedReader(new FileReader(root + fname + "_s2.txt"));

		String line, line2;
		PredicateArgumentExtractor prExt = new PredicateArgumentExtractor(null);
		PrintWriter op = new PrintWriter(new File(root + fname + "_rels.txt"));
		PrintWriter opLDA = new PrintWriter(new File(root + fname + "_LDArels.txt"));

		while ((line = br.readLine()) != null) {
			line2 = brOrig.readLine();
			String[] ss = line.split("\t");
			String rel1 = "", rel2 = "";
			String[] ss2 = line2.split("\t");
			String[] rel1Args = new String[] { ss2[0].split(",")[0].trim().toLowerCase(),
					ss2[0].split(",")[2].trim().toLowerCase() };
			String[] rel2Args = new String[] { ss2[1].split(",")[0].trim().toLowerCase(),
					ss2[1].split(",")[2].trim().toLowerCase() };
			try {
				rel1 = prExt.extractPredArgsStrsForceFinding(ss[0] + ".", rel1Args[0], rel1Args[1]);
				rel2 = prExt.extractPredArgsStrsForceFinding(ss[1] + ".", rel2Args[0], rel2Args[1]);
			} catch (ArgumentValidationException | InterruptedException e) {
				e.printStackTrace();
			}
			if (rel1.equals("")) {
				System.out.println("nothing: " + ss[0]);
			}
			if (rel2.equals("")) {
				System.out.println("nothing: " + ss[1]);
			}

			System.out.println(line);
			System.out.println("rel2: " + rel2);
			System.out.println("rel1: " + rel1);

			String LDArel1 = "", LDArel2 = "";
			String LDAtypes1 = "";//, LDAtypes2 = "";

			if (!rel1.equals("")) {
				String[] rel1ss = rel1.split(" ");
				String[] lemmas = Util.getPredicateLemma(rel1ss[0], true);
				rel1ss[0] = lemmas[0];

				// no backup for figerTypes
				String lt1 = Util.linkAndType(rel1ss[1], rel1ss[4].charAt(0) == 'E',
						!EntailGraphFactoryAggregator.figerTypes);
				String lt2 = Util.linkAndType(rel1ss[2], rel1ss[4].charAt(1) == 'E',
						!EntailGraphFactoryAggregator.figerTypes);

				System.out.println(line + " " + lt1 + " " + lt2);

				if (lemmas[1].equals("false")) {
					LDArel1 = rel1ss[0] + " " + rel1ss[1] + " " + rel1ss[2];// no change. e.g.: (write.1,write.2)
									// dramatist hamlet
					LDAtypes1 = getLDATypesStr(rel1ss[0], rel1ss[1], rel1ss[2]);
					rel1 = rel1ss[0] + " " + lt1 + " " + lt2;
				} else {
					LDArel1 = rel1ss[0] + " " + rel1ss[2] + " " + rel1ss[1];
					LDAtypes1 = getLDATypesStr(rel1ss[0], rel1ss[2], rel1ss[1]);
					rel1 = rel1ss[0] + " " + lt2 + " " + lt1;
				}
			}

			if (!rel2.equals("")) {
				String[] rel2ss = rel2.split(" ");
				String[] lemmas = Util.getPredicateLemma(rel2ss[0], true);
				rel2ss[0] = lemmas[0];

				String lt1 = Util.linkAndType(rel2ss[1], rel2ss[4].charAt(0) == 'E',
						!EntailGraphFactoryAggregator.figerTypes);
				String lt2 = Util.linkAndType(rel2ss[2], rel2ss[4].charAt(1) == 'E',
						!EntailGraphFactoryAggregator.figerTypes);

				System.out.println(line + " " + lt1 + " " + lt2);

				if (lemmas[1].equals("false")) {
					LDArel2 = rel2ss[0] + " " + rel2ss[1] + " " + rel2ss[2];// no change. e.g.: (write.1,write.2)
									// dramatist hamlet
//					LDAtypes2 = getLDATypesStr(rel2ss[0], rel2ss[1], rel2ss[2]);
					rel2 = rel2ss[0] + " " + lt1 + " " + lt2;
				} else {
					LDArel2 = rel2ss[0] + " " + rel2ss[2] + " " + rel2ss[1];
//					LDAtypes2 = getLDATypesStr(rel2ss[0], rel2ss[2], rel2ss[1]);
					rel2 = rel2ss[0] + " " + lt2 + " " + lt1;
				}
			}

			op.println(rel1 + "\t" + rel2 + "\t" + ss[2]);
			
			//We'll assume that the LDAtypes are inherited only from the LHS of Levy (q)
			opLDA.println(LDArel1 + "\t" + LDArel2 + "\t" + ss[2]+"\t"+LDAtypes1);
		}
		br.close();
		op.close();
		brOrig.close();
		opLDA.close();

	}

	static String getLDATypesStr(String pred, String arg1, String arg2) {
		String ret = "";
		List<float[]> types = DistrTyping.getType(pred, arg1, arg2);
		// System.err.println(pred + "," + arg1 + "," + arg2);
		// System.err.println("types:");
		ArrayList<Integer> types1 = new ArrayList<>();// only the non-zero
														// ones
		ArrayList<Integer> types2 = new ArrayList<>();// only the non-zero
														// ones
		for (int k = 0; k < types.get(0).length; k++) {
			if (types.get(0)[k] != 0) {
				types1.add(k);
				// System.err.print(k + ":" + types.get(0)[k] + " ");
			}
		}
		// System.err.println();
		for (int k = 0; k < types.get(1).length; k++) {
			if (types.get(1)[k] != 0) {
				types2.add(k);
				// System.err.print(k + ":" + types.get(1)[k] + " ");
			}
		}
		// System.err.println();
		String type1, type2;
		// Now, find all the likely type-pairs
		double sum = 0;
		for (int t1 : types1) {
			for (int t2 : types2) {
				float prob = types.get(0)[t1] * types.get(1)[t2];
				if (prob < .1) {
					continue;
				}

				// System.err.println("adding");
				sum += prob;
			}
		}
		for (int t1 : types1) {
			for (int t2 : types2) {
				float prob = types.get(0)[t1] * types.get(1)[t2];
				if (prob < .1) {
					continue;
				}
				type1 = "type" + t1;
				type2 = "type" + t2;
				ret += type1 + "#" + type2 + " " + (prob / sum)+" ";// sum up to 1!
			}
		}
		return ret.trim();
	}

	// the oil : oil
	// + lemma
	private static String removeDet(String s) {

		s = Util.getLemma(s);
		// hack for an error in lemma (finally, the output wouldn't change, just
		// a matching which is used to transform the dataset
		s = s.replace("united nation", "unite nation");
		s = s.replace("alamos", "alamo");
		s = s.replace("papers", "paper");
		s = s.replace("vitamin a", "vitamin");
		String[] starts = { "the very ", "all such ", "the most ", "the other ", "this same ", "no other ",
				"any other ", "other ", "the same ", "some other ", "each ", "any ", "the only ", "all other ",
				"such an ", "such a ", "a few ", "the ", "this ", "a ", "these ", "that ", "those ", "an ", "all ",
				"some ", "more ", "such ", "most ", "both ", "which ", "on ", "your ", "my ", "his ", "her ", "you ",
				"only ", "she ", "he " };
		for (String st : starts) {
			if (s.startsWith(st)) {
				s = s.substring(st.length());
				break;
			}
		}

		return s;
	}

	private static String removeNeg(String s) {

		String[] starts = { "no " };
		for (String st : starts) {
			if (s.startsWith(st)) {
				s = s.substring(st.length());
				break;
			}
		}
		s = Util.getLemma(s);
		return s;
	}

	static void formOIEDSAll() {
		// String[] fileNames = new String[] { "train1", "dev1", "test1" };//
		// String[] fileNames = new String[] { "all", "train1", "dev1", "test1",
		// "all_new", "train_new", "dev_new",
		// "test_new", "all_new_dir", "dev_new_dir", "train_new_dir",
		// "test_new_dir" };//

		String[] fileNames = new String[] { "train_new", "dev_new", "test_new", "dev_new_dir", "train_new_dir",
				"test_new_dir" };//
		// String[] fileNames = new String[] { "all_new", "all_new_dir" };//
		for (String s : fileNames) {
			try {
				fixNEs(s);
				formOIEDS(root + s + "_rels.txt", root + s + "_s2.txt");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	static void formOIEDS(String relPath, String origPath) throws FileNotFoundException {
		// Scanner scCCG = new Scanner(new File(CCGRelPath));
		Scanner scOrig = new Scanner(new File(origPath));
		String oPath = relPath.split("\\.")[0] + "_oie.txt";
		PrintStream op = new PrintStream(new File(oPath));

		while (scOrig.hasNextLine()) {
			// String line = scCCG.nextLine();
			String lineOrig = scOrig.nextLine();
			System.out.println(lineOrig);
			String[] parts = lineOrig.split("\t");
			String[] rel1Parts = parts[0].split(",");
			String[] rel2Parts = parts[1].split(",");

			String rel1Str;
			if (rel1Parts.length > 1) {
				String rel1 = rel1Parts[1].trim();
				rel1 = Util.getPredicateLemma(rel1, false)[0];

				String arg1 = rel1Parts[0];
				boolean isEnt1 = !Util.isGeneric(arg1, Util.getAllPOSTags(arg1));
				String arg2 = rel1Parts[2];
				boolean isEnt2 = !Util.isGeneric(arg2, Util.getAllPOSTags(arg2));

				String lt1 = Util.linkAndType(arg1, isEnt1, true);
				if (lt1.endsWith("thing")) {
					lt1 = Util.linkAndType(arg1.split(" ")[arg1.split(" ").length - 1], isEnt1, true);
				}
				String lt2 = Util.linkAndType(arg2, isEnt2, true);
				if (lt2.endsWith("thing")) {
					lt2 = Util.linkAndType(arg2.split(" ")[arg2.split(" ").length - 1], isEnt2, true);
				}

				rel1Str = rel1 + " " + lt1 + " " + lt2;
			} else {
				rel1Str = "";
			}

			String rel2Str;
			if (rel2Parts.length > 1) {
				String rel2 = rel2Parts[1].trim();
				rel2 = Util.getPredicateLemma(rel2, false)[0];

				String arg1 = rel2Parts[0];
				boolean isEnt1 = !Util.isGeneric(arg1, Util.getAllPOSTags(arg1));
				String arg2 = rel2Parts[2];
				boolean isEnt2 = !Util.isGeneric(arg2, Util.getAllPOSTags(arg2));

				String lt1 = Util.linkAndType(arg1, isEnt1, true);
				if (lt1.endsWith("thing")) {
					lt1 = Util.linkAndType(arg1.split(" ")[arg1.split(" ").length - 1], isEnt1, true);
				}
				String lt2 = Util.linkAndType(arg2, isEnt2, true);
				if (lt2.endsWith("thing")) {
					lt2 = Util.linkAndType(arg2.split(" ")[arg2.split(" ").length - 1], isEnt2, true);
				}

				rel2Str = rel2 + " " + lt1 + " " + lt2;
			} else {
				rel2Str = "";
			}

			String oieLine = rel1Str + "\t" + rel2Str + "\t" + parts[2];

			op.println(oieLine);

		}
	}

	// no typing, no arg checking
	static void normalizeOIESimple(String fpath) throws IOException {
		EntailGraphFactoryAggregator.isTyped = true;
		EntailGraphFactoryAggregator.figerTypes = true;
		BufferedReader br = new BufferedReader(new FileReader(fpath));
		int dotIdx = fpath.lastIndexOf('.');
		String opath = fpath.substring(0, dotIdx) + "_norm" + fpath.substring(dotIdx);
		PrintStream op = new PrintStream(new File(opath));
		String line = null;
		while ((line = br.readLine()) != null) {
			String[] ss = line.split("\t");
			String[] relParts0 = ss[0].split(",");
			String[] relParts1 = ss[1].split(",");
			ss[0] = relParts0[0].trim() + ", " + Util.getPredicateLemma(relParts0[1].trim(), false)[0] + ", "
					+ relParts0[2].trim();
			ss[1] = relParts1[0].trim() + ", " + Util.getPredicateLemma(relParts1[1].trim(), false)[0] + ", "
					+ relParts1[2].trim();
			op.println(ss[0] + "\t" + ss[1] + "\t" + ss[2]);
		}
		br.close();
		op.close();

	}

	// Whether it's the swap of q_arg, ...
	static boolean isSwapped(String line) {
		String[] ss = line.split("\t");
		String cand = ss[0] + "\t" + ss[1];
		String cand2 = ss[1] + "\t" + ss[0];
		if (allPrevInstances.contains(cand)) {
			return false;
		} else if (allPrevInstances.contains(cand2)) {
			return true;
		}
		throw new RuntimeException("horrible bug");
	}

	static void convertDSToRelsCCG() throws IOException {
		// String[] fileNames = new String[] { "dummy"};//
		// String[] fileNames = new String[] { "dev1", "train1", "test1" };//
		// String[] fileNames = new String[] { "all_new" };//
		EntailGraphFactoryAggregator.isTyped = true;
		EntailGraphFactoryAggregator.figerTypes = true;
		// String[] fileNames = new String[] { "all_new", "all_new_dir" };//
		String[] fileNames = new String[] { "all", "train1", "dev1", "test1", "all_new", "train_new", "dev_new",
				"test_new", "all_new_dir", "train_new_dir", "dev_new_dir", "test_new_dir" };//

		for (String fname : fileNames) {
			fixNEs(fname);
			extractRelationsCCG(fname);
		}
	}

	static void testTime1() {
		long t0 = System.currentTimeMillis();
		int m = 10000000;
		HashMap<Integer, Integer> h = new HashMap<>();
		for (int i = 0; i < m; i++) {
			h.put(i, 2 * i);
		}

		System.out.println(System.currentTimeMillis() - t0);

		int s = 0;
		for (int i = 0; i < m; i++) {
			int x = h.get(i);
			if (i % 2 == 0) {
				s += x;
			} else {
				s -= x;
			}
		}
		System.out.println(s);
		System.out.println(System.currentTimeMillis() - t0);
	}

	// split new annotated DS based on the previous one!
	static void splitBasedOnPrevDS(String fname) throws IOException {
		Set<String> trainIns = getInstances(root + "train1.txt");
		Set<String> devIns = getInstances(root + "dev1.txt");
		Set<String> testIns = getInstances(root + "test1.txt");
		String line = null;
		BufferedReader br = new BufferedReader(new FileReader(fname));

		String trainName = fname.substring(0, fname.lastIndexOf('.')) + "_train.txt";
		String devName = fname.substring(0, fname.lastIndexOf('.')) + "_dev.txt";
		String testName = fname.substring(0, fname.lastIndexOf('.')) + "_test.txt";
		PrintStream opTrain = new PrintStream(new File(trainName));
		PrintStream opDev = new PrintStream(new File(devName));
		PrintStream opTest = new PrintStream(new File(testName));

		while ((line = br.readLine()) != null) {

			boolean swapped = isSwapped(line);
			String[] ss = line.split("\t");
			String ins = ss[0] + "\t" + ss[1];
			if (swapped) {
				ins = ss[1] + "\t" + ss[0];
			}
			if (trainIns.contains(ins)) {
				opTrain.println(line);
			} else if (devIns.contains(ins)) {
				opDev.println(line);
			} else if (testIns.contains(ins)) {
				opTest.println(line);
			} else {
				throw new RuntimeException("horrible bug");
			}

		}

		opTrain.close();
		opDev.close();
		opTest.close();
		br.close();

	}

	static void countUniques(String f) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(f));
		String line = null;
		Set<String> allLines = new HashSet<>();
		while ((line = br.readLine()) != null) {
			allLines.add(line);
		}
		System.out.println(allLines.size());
	}

	public static void main(String[] args) throws IOException {
		// countUniques(root + "re-annotated-full.tsv");
		// makeCandEnts();
		// split_chunks();
		// split_preds();
		// split_chunks_unordered();

		convertDSToRelsCCG();
		// formOIEDSAll();
		// testTime1();
		// splitBasedOnPrevDS(root + "all_new.txt");
		// splitBasedOnPrevDS(root + "all_new_dir.txt");

		// normalizeOIESimple(root+"re-annotated-directional.tsv");
		// normalizeOIESimple(root+"re-annotated-full.tsv");

		// formOIEDSAll();
	}
}
