package data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import entailment.Util;
import entailment.linkingTyping.SimpleSpot;

public class Stats {

	public static StanfordCoreNLP stanPipeline;

	static {
		// creates a StanfordCoreNLP object, with POS tagging, lemmatization,
		// NER, parsing, and coreference resolution
		Properties props = new Properties();
		// props.put("annotators",
		// "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
		props.put("annotators", "tokenize,ssplit");
		// logger = Logger.getLogger(StanfordCoreNLP.class);
		// logger.setLevel(Level.OFF);
		// System.out.println("here111");
		stanPipeline = new StanfordCoreNLP(props);
	}

	public static int countSentences(String text) {

		// create an empty Annotation just with the given text
		Annotation document = new Annotation(text);

		// run all Annotators on this text
		stanPipeline.annotate(document);

		// Iterate over all of the sentences found
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		return sentences.size();

	}

	public static void countRelsArgs(String[] args) throws JsonSyntaxException, IOException, ParseException {
		String fName = args[0];
		HashMap<String, Integer> argCount = new HashMap<>();
		HashMap<String, Integer> relCount = new HashMap<>();
		BufferedReader br = new BufferedReader(new FileReader(fName));

		int numAllRels = 0;

		int lineNumbers = 0;
		JsonParser jsonParser = new JsonParser();

		Map<String, Integer> year2relCounts = new HashMap<>();

		// long t0;
		// long sharedTime = 0;

		String line;

		int lineIdx = 0;

		while ((line = br.readLine()) != null) {

			try {
				if (line.startsWith("exception for") || line.contains("nlp.pipeline")) {
					continue;
				}

				if (lineIdx++ % 10000 == 0) {
					System.out.println("lineIdx: " + lineIdx);
				}

				JsonObject jObj = jsonParser.parse(line).getAsJsonObject();

				JsonArray jar = jObj.get("rels").getAsJsonArray();

				String datestamp = jObj.get("date").getAsString();
				String[] ds_ss = datestamp.split(" ");
				// datestamp = ds_ss[0] + "_" + ds_ss[1] + "_" + ds_ss[2];
				String year = Util.getYear(ds_ss[0] + " " + ds_ss[1] + " " + ds_ss[2]);

				// t0 = 0;

				for (int i = 0; i < jar.size(); i++) {
					JsonObject relObj = jar.get(i).getAsJsonObject();
					String relStr = relObj.get("r").getAsString();
					relStr = relStr.substring(1, relStr.length() - 1);
					String[] parts = relStr.split("::");
					String pred = parts[0];

					if (++lineNumbers % 10000 == 0) {
						System.err.println(lineNumbers);
					}

					if (!Util.acceptablePredFormat(pred, true)) {
						continue;
					}

					numAllRels++;

					if (!year2relCounts.containsKey(year)) {
						year2relCounts.put(year, 1);
					} else {
						year2relCounts.put(year, year2relCounts.get(year) + 1);
					}

					pred = Util.getPredicateNormalized(pred, true)[0];
					if (!relCount.containsKey(pred)) {
						relCount.put(pred, 1);
					} else {
						relCount.put(pred, relCount.get(pred) + 1);
					}

					// We also remove "-" here, because sometimes, we have the type
					// without "-". But we didn't remove
					// "-" when we're looking in g kg, because it might help!

					for (int j = 1; j < 3; j++) {
						parts[j] = Util.simpleNormalize(parts[j]);

						if (!argCount.containsKey(parts[j])) {
							argCount.put(parts[j], 1);
						} else {
							argCount.put(parts[j], argCount.get(parts[j]) + 1);
						}

					}
				}
			} catch (Exception e) {
				System.err.println("could not parse: " + line);
				continue;
			}

		}

		System.out.println("rel counts:");

		ArrayList<SimpleSpot> sspots = new ArrayList<>();

		int relIdx = 0;

		for (String s : relCount.keySet()) {
			sspots.add(new SimpleSpot(s, relCount.get(s)));
		}

		Collections.sort(sspots, Collections.reverseOrder());

		for (SimpleSpot ss : sspots) {
			if (relIdx++ == 1000) {
				break;
			}
			System.out.println(ss.spot + " " + ss.count);
		}

		System.out.println("arg counts:");

		int argIdx = 0;

		sspots = new ArrayList<>();
		for (String s : argCount.keySet()) {
			sspots.add(new SimpleSpot(s, argCount.get(s)));
		}

		Collections.sort(sspots, Collections.reverseOrder());

		for (SimpleSpot ss : sspots) {
			if (argIdx++ == 1000) {
				break;
			}
			System.out.println(ss.spot + " " + ss.count);
		}

		System.out.println("num All rels: " + numAllRels);
		System.out.println("num unique rels: " + relCount.size());

		System.out.println("num relations per year");
		for (String year : year2relCounts.keySet()) {
			System.out.println("year " + year + ": " + year2relCounts.get(year));
		}

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

	static void countNumSentences(String fileName) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(fileName));
		String line;
		int countAllSentences = 0;

		int lineIdx = 0;

		while ((line = br.readLine()) != null) {

			if (line.startsWith("exception for") || line.contains("nlp.pipeline")) {
				continue;
			}

			if (lineIdx++ % 10000 == 0) {
				System.out.println("lineIdx: " + lineIdx);
			}

			countAllSentences += countSentences(line);
			// System.out.println("num All sents: "+countAllSentences);
		}
		System.out.println("num All sents: " + countAllSentences);
	}

	// static void countNodesAllGraphs(String folderName) throws IOException {
	// File folder = new File(folderName);
	// File[] files = folder.listFiles();
	// Arrays.sort(files);
	//
	// Map<String,String> fnameToPredCount = new HashMap<>();
	// HashSet<String> uniquePreds = new HashSet<>();
	//
	// int gc = 0;
	// for (File f : files) {
	// String fname = f.getName();
	// if (!fname.endsWith("_sim.txt")) {
	// continue;
	// }
	//
	// int numPreds = 0;
	//
	// String line = null;
	// BufferedReader br = new BufferedReader(new FileReader(fname));
	// while ((line=br.readLine())!=null) {
	// if (!line.startsWith("predicate:")) {
	// continue;
	// }
	// String pred = line.substring(11);
	// }
	// }
	// }

	public static void main(String[] args) throws IOException, JsonSyntaxException, ParseException {
		// if (args.length==0){
		// args = new String[]{"particles.txt"};
		// }
		countNumSentences("news_genC_GG_CN_nbee.json");

		// countRelsArgs(new String[] { "news_genC_GG_CN_nbee.json" });
		// countRelsArgs(new String[] { "news_gen8_p.json" });

	}
}
