package data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import constants.ConstantsAgg;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TrueCaseAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import entailment.Util;
import entailment.entityLinking.DistrTyping;
import entailment.entityLinking.SimpleSpot;
import entailment.vector.EntailGraphFactoryAggregator;
import entailment.vector.EntailGraphFactoryAggregator.TypeScheme;
import uk.co.flamingpenguin.jewel.cli.ArgumentValidationException;

public class Scripts {
	static void swapDS(String path) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(path));
		String line;
		while ((line = br.readLine()) != null) {
			String[] ss = line.split("\t");
			System.out.println(ss[1] + "\t" + ss[0] + "\t" + ss[2]);
		}
		br.close();
	}

	static void makeEntTypes() throws IOException {
		String root = "freebase_types/";
		BufferedReader br1 = new BufferedReader(new FileReader(root + "freebase2wikiTitle.txt"));
		BufferedReader br2 = new BufferedReader(new FileReader(root + "mid2name.tsv"));
		BufferedReader br3 = new BufferedReader(new FileReader(root + "entity2type_names.txt"));
		BufferedReader br4 = new BufferedReader(new FileReader(root + "types.map"));

		HashMap<String, String> mid2FreebaseName = new HashMap<>();
		HashMap<String, String> mid2FreebaseType = new HashMap<>();
		HashMap<String, String> freebaseToFigerTypes = new HashMap<>();
		HashMap<String, List<String>> mid2WikiTitle = new HashMap<>();
		HashSet<String> matchedWikis = new HashSet<>();// the wiki titles that
														// have matched freebase
														// completely

		PrintStream op = new PrintStream(new File(root + "entity2Types.txt"));

		String line = null;
		int lineNumber = 0;

		// form mapping from mid 2 freebase types and names
		while ((line = br3.readLine()) != null) {
			String[] ss = line.split("\t");
			String mid = ss[0];
			String freebaseName = ss[2];
			String types = ss[3];

			mid2FreebaseType.put(mid, types);
			mid2FreebaseName.put(mid, Util.simpleNormalize(freebaseName));
			// if (lineNumber++==10){
			// break;
			// }
		}

		// read types.map
		while ((line = br4.readLine()) != null) {
			System.out.println(line);
			String[] ss = line.split("\t");
			freebaseToFigerTypes.put(ss[0], ss[1]);
		}

		lineNumber = 0;

		// read first mid 2 wiki
		while ((line = br1.readLine()) != null) {
			if (!line.startsWith("<Entity")) {
				continue;
			}
			try {
				int midIdx = line.indexOf("mid=\"");
				String mid = line.substring(midIdx + 5, line.length() - 2);

				line = br1.readLine();
				String wiki = line.substring(11, line.length() - 12);

				System.out.println(mid);
				System.out.println(wiki);

				String wikiNorm = Util.simpleNormalize(wiki);
				if (matchedWikis.contains(wikiNorm)) {
					continue;
				}
				if (wikiNorm.equals(mid2FreebaseName.get(mid))) {
					matchedWikis.add(wikiNorm);
				}

				// one mid to many wikis (in mid2name.tsv)
				// if (!mid2WikiTitle.containsKey(mid) ||
				// matchedWikis.contains(wikiNorm)){
				// mid2WikiTitle.put(mid, wiki);
				// }
				if (!mid2WikiTitle.containsKey(mid)) {
					mid2WikiTitle.put(mid, new ArrayList<>());
				}

				mid2WikiTitle.get(mid).add(wiki);

				// if (lineNumber++%100==0){
				// break;
				// }
				br1.readLine();
			} catch (Exception e) {
				continue;
			}

			// if (lineNumber++==10){
			// break;
			// }

		}

		lineNumber = 0;

		// read 2nd mid 2 wiki
		while ((line = br2.readLine()) != null) {
			String[] ss = line.split("\t");
			String wiki = ss[1];
			String mid = ss[0];

			System.out.println(wiki);
			System.out.println(mid);

			String wikiNorm = Util.simpleNormalize(wiki);
			if (matchedWikis.contains(wikiNorm)) {
				continue;
			}
			if (wikiNorm.equals(mid2FreebaseName.get(mid))) {
				matchedWikis.add(wikiNorm);
			}

			// one mid to many wikis (in mid2name.tsv)
			// if (!mid2WikiTitle.containsKey(mid) ||
			// matchedWikis.contains(wikiNorm)){
			// mid2WikiTitle.put(mid, wiki);
			// }
			if (!mid2WikiTitle.containsKey(mid)) {
				mid2WikiTitle.put(mid, new ArrayList<>());
			}
			mid2WikiTitle.get(mid).add(wiki);

			// if (lineNumber++==10){
			// break;
			// }

		}

		lineNumber = 0;

		for (String mid : mid2WikiTitle.keySet()) {

			if (!mid2FreebaseType.containsKey(mid)) {
				System.out.println("type not found for " + " :" + mid);
				continue;
			}

			String freebaseTypes = mid2FreebaseType.get(mid);
			String[] frTypes = freebaseTypes.split("\\s");
			if (!freebaseToFigerTypes.containsKey(frTypes[0])) {
				System.out.println("no mapping" + frTypes[0]);
			}

			String figerTypes = "";
			for (String s : frTypes) {
				if (freebaseToFigerTypes.containsKey(s)) {
					figerTypes += freebaseToFigerTypes.get(s) + " ";
				}
			}

			figerTypes = figerTypes.trim();

			for (String wiki : mid2WikiTitle.get(mid)) {
				if (figerTypes.equals("")) {
					System.out.println("no type for: " + wiki + " " + mid);
				}

				String entry = mid + "\t" + wiki + "\t";

				entry += figerTypes + "\t#\t" + freebaseTypes;
				op.println(entry);
			}

		}

		br1.close();
		br2.close();
		br3.close();
		br4.close();

	}

	static void testEntTypes() throws IOException {
		PrintStream op = new PrintStream("entToFigerTypes.txt");
		EntailGraphFactoryAggregator.typeScheme = TypeScheme.FIGER;
		ConstantsAgg.isTyped = true;
		BufferedReader br = new BufferedReader(new FileReader("entTypes.txt"));
		String line;
		while ((line = br.readLine()) != null) {
			String s = line.split("::")[0];
			s = Util.simpleNormalize(s);
			op.println(s + "::" + Util.getType(s, true, null));
		}
		br.close();
		op.close();
	}

	static void trueCase() {
		String test = "The team Barelona lost to real madrid";
		Properties props = new Properties();
		// props.put("annotators",
		// "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
		props.put("annotators", "tokenize,ssplit,pos,lemma,ner, truecase");
		// logger = Logger.getLogger(StanfordCoreNLP.class);
		// logger.setLevel(Level.OFF);
		// System.out.println("here111");
		StanfordCoreNLP parser = new StanfordCoreNLP(props);
		Annotation document = new Annotation(test);
		// run all Annotators on this text
		parser.annotate(document);

		// Iterate over all of the sentences found
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);

		for (CoreMap sentence : sentences) {
			// Iterate over all tokens in a sentence
			for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
				// Retrieve and add the lemma for each word into the list of
				// lemmas
				String word = token.get(TextAnnotation.class);
				System.out.println(word);
				System.out.println(token.get(TrueCaseAnnotation.class));
				if (word.equals("")) {
					continue;
				}
			}
		}

	}

	static void makeUniqueDS(String f) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(f));
		String line = null;
		Set<String> allLines = new HashSet<>();
		while ((line = br.readLine()) != null) {
			if (allLines.contains(line)) {
				continue;
			}
			allLines.add(line);
			System.out.println(line);

		}
	}
	
	static void convertDecomposableAttentionProbsToFlat(String fname) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(fname));
		String line = null;
		JsonParser jsonParser = new JsonParser();
		boolean ignore = false;
		while ((line=br.readLine())!=null) {
			if (line.startsWith("input:")) {
				line = line.replace("input: ", "");
//				System.out.println(line);
				JsonObject jObj = jsonParser.parse(line).getAsJsonObject();
				String label = jObj.get("gold_label").getAsString();
//				System.out.println(label);
				if (label.equals("-")) {
					ignore = true;
				}
				continue;
			}
			else if(line.equals("")) {
				continue;
			}
			if (ignore) {
				ignore = false;
				continue;
			}
			line = line.replace("prediction:  ","");
//			System.out.println(line);
			JsonObject jObj = jsonParser.parse(line).getAsJsonObject();
			String probs = jObj.get("label_probs")+"";
//			System.out.println(probs);
			probs = probs.replace("[", "").replace("]","").replace(","," ");
			String[] ss = probs.split(" ");
			System.out.println(ss[0]+" "+ss[2]+" "+ss[1]);
		}
	}

	// static void formLDAInput(String fpath) throws IOException {
	// File folder = new File(fpath);
	// File[] files = folder.listFiles();
	//
	// HashMap<String, Integer> allPredsNeighCounts = new HashMap<>();
	// List<SimpleSpot> allPredsList = new ArrayList<>();
	// int numAllPreds = 0;
	//
	// for (File f : files) {
	// String p = f.getPath();
	// // if (p.contains("_sim") || p.contains("_graph")){
	// // continue;
	// // }
	//
	// if (p.endsWith("_sim.txt") || p.contains("_emb") || p.contains("_graph"))
	// {
	// continue;
	// }
	//
	// BufferedReader br = new BufferedReader(new FileReader(p));
	//
	// HashMap<String, String> predToDocument = new HashMap<>();
	//
	// String line;
	// br.readLine();//skip the first line
	// String doc = "";
	// while ((line = br.readLine()) != null) {
	// if (line.startsWith("predicate:")) {
	// String pred = line.substring(11);
	// pred = pred.substring(0, pred.indexOf("#"));
	// numAllPreds++;
	// line = br.readLine();
	// int numNeighs = Integer.parseInt(line.substring(line.lastIndexOf(':') +
	// 2));
	// if (!allPredsNeighCounts.containsKey(pred)) {
	// allPredsNeighCounts.put(pred, numNeighs);
	// } else {
	// allPredsNeighCounts.put(pred, allPredsNeighCounts.get(pred) + numNeighs);
	// }
	// }
	// }
	// br.close();
	// }
	//
	// for (String s : allPredsNeighCounts.keySet()) {
	// allPredsList.add(new SimpleSpot(s, allPredsNeighCounts.get(s)));
	// }
	//
	// Collections.sort(allPredsList, Collections.reverseOrder());
	//
	// System.out.println(numAllPreds);
	//
	// for (SimpleSpot ss : allPredsList) {
	// System.out.println(ss.spot + " " + ss.count);
	// }
	// }

	static void getAllRemainedPredicates(String fpath) throws IOException {
		File folder = new File(fpath);
		File[] files = folder.listFiles();

		// HashMap<String, Integer> allPredsNeighCounts = new HashMap<>();
		// List<SimpleSpot> allPredsList = new ArrayList<>();

		int numAllPreds = 0;
		int numAllLocalEdges = 0;

		Map<String, Integer> fnameToPredCount = new HashMap<>();
		Set<String> uniqueUntypedPreds = new HashSet<>();

		for (File f : files) {
			String p = f.getPath();
			// if (p.contains("_sim") || p.contains("_graph")){
			// continue;
			// }

			if (!p.endsWith("_sim.txt")) {
				continue;
			}
			int thisNumPreds = 0;

			BufferedReader br = new BufferedReader(new FileReader(p));

			String line;
			while ((line = br.readLine()) != null) {
				if (line.startsWith("predicate:")) {
					thisNumPreds++;
					numAllPreds++;
					String pred = line.substring(11);
					pred = pred.substring(0, pred.indexOf("#"));
					uniqueUntypedPreds.add(pred);
					line = br.readLine();
					int numNeighs = Integer.parseInt(line.substring(line.lastIndexOf(':') + 2));
					numAllLocalEdges += numNeighs;
					// if (!allPredsNeighCounts.containsKey(pred)) {
					// allPredsNeighCounts.put(pred, numNeighs);
					// } else {
					// allPredsNeighCounts.put(pred, allPredsNeighCounts.get(pred) + numNeighs);
					// }
				}

			}
			fnameToPredCount.put(p, thisNumPreds);
			br.close();
		}

		// for (String s : allPredsNeighCounts.keySet()) {
		// allPredsList.add(new SimpleSpot(s, allPredsNeighCounts.get(s)));
		// }

		// Collections.sort(allPredsList, Collections.reverseOrder());

		System.out.println("num all preds: " + numAllPreds);
		System.out.println("num all untyped preds: " + uniqueUntypedPreds.size());
		System.out.println("num all local edges: " + numAllLocalEdges);

		// System.out.println("neighbor counts: ");
		// for (SimpleSpot ss : allPredsList) {
		// System.out.println(ss.spot + " " + ss.count);
		// }

		System.out.println("num preds in files");
		for (String s : fnameToPredCount.keySet()) {
			System.out.println(s + "\t" + fnameToPredCount.get(s));
		}

	}

	static void getSubCCG() throws IOException {
		Scanner sc = new Scanner(new File("all_target_sub.txt"));
		BufferedReader br = new BufferedReader(new FileReader("../../python/gfiles/ent/ccg2.sim"));
		PrintStream op = new PrintStream(new File("../../python/gfiles/ent/ccg3.sim"));
		HashSet<String> goodPreds = new HashSet<>();
		while (sc.hasNextLine()) {
			String pred = sc.nextLine().split(" ")[0];
			// System.out.println(pred);
			goodPreds.add(pred);
		}
		String line = null;

		while ((line = br.readLine()) != null) {
			String pred = line.split("\t")[0];
			pred = pred.substring(0, pred.length() - 1);
			// System.out.println(pred+" "+goodPreds.contains(pred));
			if (goodPreds.contains(pred)) {
				op.println(line);
			}
		}
		op.close();
	}

	static void getThresholdsCCGEmbs() throws IOException {
		String fpath1 = "../../python/gfiles/ent/ccg.sim";
		String fpath2 = "../../python/gfiles/ent/ccg2.sim";

		String line = null;
		BufferedReader br1 = new BufferedReader(new FileReader(fpath1));
		BufferedReader br2 = new BufferedReader(new FileReader(fpath2));

		HashMap<String, HashSet<String>> targetPreds = new HashMap<>();

		while ((line = br1.readLine()) != null) {
			String[] ss = line.split("\t");
			String pred = ss[0].trim();

			HashSet<String> currentPreds = new HashSet<>();
			int idx = 1;

			while (idx < ss.length) {
				String q = ss[idx];
				idx += 2;
				currentPreds.add(q);
			}

			targetPreds.put(pred, currentPreds);

			// System.out.println(line);
		}

		while ((line = br2.readLine()) != null) {
			String[] ss = line.split("\t");
			String pred = ss[0].trim();

			if (!targetPreds.containsKey(pred)) {
				continue;
			}

			HashSet<String> rels = targetPreds.get(pred);

			int idx = 1;

			while (idx < ss.length) {
				String q = ss[idx];
				idx += 1;
				float sim = Float.parseFloat(ss[idx].trim());
				idx += 1;
				if (!rels.contains(q) && !q.equals(pred)) {
					System.out.println("last Threshold: " + sim + " " + pred + " " + q);
					break;
				}
			}

		}

		br1.close();
		br2.close();
	}

	static void combineCCG3WTargetRels() throws IOException {
		String fpath1 = "../../python/gfiles/ent/ccg.sim";
		String fpath2 = "../../python/gfiles/ent/ccg3.sim";

		String line = null;
		BufferedReader br1 = new BufferedReader(new FileReader(fpath1));
		BufferedReader br2 = new BufferedReader(new FileReader(fpath2));

		HashSet<String> targetPreds = new HashSet<>();
		while ((line = br1.readLine()) != null) {
			String[] ss = line.split("\t");
			String pred = ss[0].trim();
			targetPreds.add(pred);
			// System.out.println(line);
		}

		while ((line = br2.readLine()) != null) {
			String[] ss = line.split("\t");
			String pred = ss[0].trim();
			if (targetPreds.contains(pred)) {
				continue;
			}
			// System.out.println(line);
			System.out.println(pred);
		}
		br1.close();
		br2.close();
	}

	static void getUnion(String s1, String s2) throws FileNotFoundException {

		Scanner sc = new Scanner(new File(s1));
		Scanner sc2 = new Scanner(new File(s2));
		Set<String> allStrs = new HashSet<>();

		while (sc.hasNext()) {// write all the target rels
			String line = sc.nextLine();
			allStrs.add(line.split(" ")[0]);
			System.out.println(line);

		}

		int lNum = 0;
		while (sc2.hasNext()) {// write everything from graphs that are not already printed
			String line = sc2.nextLine();
			line = line.split(" ")[0];
			if (!allStrs.contains(line)) {
				System.out.println(line);
			}

			lNum++;
			if (lNum == 20000) {
				break;
			}

			allStrs.add(line);
		}

	}

	static void normalizeLDAWeights() throws IOException {
		int numTopics = DistrTyping.numTopics;
		BufferedReader br = new BufferedReader(new FileReader("topic-weights" + numTopics));
		String line = null;
		// int lineNumber = 0;
		HashMap<String, double[]> wordToWeights = new HashMap<>();

		// read weights
		while ((line = br.readLine()) != null) {
			// lineNumber++;
			String[] ss = line.split("\t");
			int topicNumber = Integer.parseInt(ss[0]);
			String word = ss[1];
			double w = Double.parseDouble(ss[2]);
			if (!wordToWeights.containsKey(word)) {
				wordToWeights.put(word, new double[numTopics]);
			}
			wordToWeights.get(word)[topicNumber] = w;
		}

		// normalize
		for (String word : wordToWeights.keySet()) {
			double[] ws = wordToWeights.get(word);
			double sum = 0;
			for (double w : ws) {
				sum += w;
			}
			for (int i = 0; i < ws.length; i++) {

				ws[i] /= sum;
			}
		}

		// write results
		for (int i = 0; i < numTopics; i++) {
			for (String word : wordToWeights.keySet()) {
				double w = wordToWeights.get(word)[i];
				if (w > DistrTyping.typePropThresh) {
					System.out.println(i + "\t" + word + "\t" + w);
				}
			}
		}
		br.close();
	}

	static void postProcess() throws IOException {
		BufferedReader br = new BufferedReader(new FileReader("predDocs.txt"));
		String line = null;
		int lineNumber = 0;
		while ((line = br.readLine()) != null) {
			lineNumber++;
			if (lineNumber % 10000 == 0) {
				System.err.println(lineNumber);
			}
			if (line.startsWith("(")) {
				line = line.substring(1);
			}

			String[] ss = line.split("\t");
			if (ss[0].contains("__") || ss.length < 3) {
				continue;
			}
			String[] ss2 = ss[2].split(" ");
			if (ss2.length < 3) {
				continue;
			}

			System.out.println(line);
		}
		br.close();
	}

	static void makeSNLIFormatJsonAll() {
		String root = "/Users/hosseini/Documents/python/gfiles/ent/";
		String[] paths = new String[] { "train_new_s", "dev_new_s", "test_new_s" };
		String[] opaths = new String[] { "train_snli.json", "dev_snli.json", "test_snli.json" };
		for (int i = 0; i < paths.length; i++) {
			try {
				makeSNLIFormatJson(root + paths[i] + ".txt", root + opaths[i] + ".txt");
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	static void makeSNLIFormatJson(String path, String opath) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(path));
		PrintStream op = new PrintStream(new File(opath));
		JsonObject jo = new JsonObject();
		String line = null;
		while ((line = br.readLine()) != null) {
			String[] ss = line.split("\t");
			jo.addProperty("sentence1", ss[1]);
			jo.addProperty("sentence2", ss[0]);
			String label = ss[2].equals("True") ? "entailment" : "contradiction";
			jo.addProperty("gold_label", label);
			op.println(jo);
		}
		op.close();
		br.close();
	}

	static void countKeys() throws NumberFormatException, IOException {
		BufferedReader br = new BufferedReader(new FileReader("MN_keys.txt"));
		String line = null;

		int lineNumber = 0;
		int numOne = 0;

		while ((line = br.readLine()) != null) {
			lineNumber++;
			if (lineNumber < 7500000) {
				continue;
			}
			String[] ss = line.split(" ");
			double key = Float.parseFloat(ss[ss.length - 1]);
			if (key > .999) {
				numOne++;
			}
		}
		System.out.println(numOne + " " + (lineNumber - 7500000));
	}

	static void dumpGoodLines() throws IOException {
		BufferedReader br = new BufferedReader(new FileReader("predArgsC_gen.txt"));
		String line;

		boolean shouldWrite = true;
		String prevMainLine = "";
		while ((line = br.readLine()) != null) {
			if (line.startsWith("#line: ")) {
				prevMainLine = line;
			} else if (line.startsWith("#lineId")) {
				try {
					int lineId = Integer.parseInt(line.split(" ")[1]);
					if (lineId < 110000000) {
						System.out.println(prevMainLine);
						System.out.println(line);
						shouldWrite = true;
					} else {
						shouldWrite = false;
					}
				} catch (Exception e) {
					shouldWrite = false;
				}
			} else {
				if (shouldWrite) {
					System.out.println(line);
				}
			}
		}
		br.close();
	}

	static void findLastRead() throws IOException {// because of some memory overload, I needed this function. Not
													// important, though
		BufferedReader br = new BufferedReader(new FileReader("predArgsC_gen.txt"));
		String line;
		boolean[] seenIdxes = new boolean[130000000];
		int idx = 0;
		while ((line = br.readLine()) != null) {
			if (line.startsWith("#lineId:")) {
				try {
					int lineId = Integer.parseInt(line.split(" ")[1]);
					seenIdxes[lineId] = true;
					idx++;
					if (idx % 100000 == 0) {
						System.out.println(idx);
					}
				} catch (Exception e) {
					System.err.println("exception for: " + line);
				}

			}
		}

		int n = 0;
		for (int i = 0; i < seenIdxes.length - 1; i++) {
			if (!seenIdxes[i + 1]) {
				n = i;
				System.out.println("N: " + n);
			}
		}

		br.close();
	}
	
	public static void main(String[] args) throws IOException, ArgumentValidationException, InterruptedException {

		// re-annotated-full.tsv was first swapped (to be the same as Levy),
		// also repeated ones were removed
		// makeUniqueDS("data/ent/all_new_dir.txt");
		// makeUniqueDS("data/ent/all_new.txt");
		// swapDS("data/ent/all_new_dir.txt");
		// swapDS("data/ent/all_new.txt");
		// makeEntTypes();
		// testEntTypes();
		// trueCase();
		
		convertDecomposableAttentionProbsToFlat(args[0]);

//		extractRelationsSNLIAll();

		// getAllRemainedPredicates("../../python/gfiles/typedEntGrDir_aida_figer_3_3_f/");
		// formLDAInput("../../python/gfiles/typedEntGrDir_aida_figer_3_3_b/");
		// String s1 = "target_rels_CCG.txt";
		// String s2 = "all_CCG_rem.txt";
		// getUnion(s1, s2);

		// getSubCCG();
		// combineCCG3WTargetRels();
		// getThresholdsCCGEmbs();

		// postProcess();
		// normalizeLDAWeights();
		// makeSNLIFormatJsonAll();

		// countKeys();

		// findLastRead();
		// dumpGoodLines();

	}

}
