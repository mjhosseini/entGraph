package entailment.vector;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import entailment.Util;
import entailment.entityLinking.DistrTyping;
import entailment.entityLinking.SimpleSpot;

//This is to do multithreading over EntGrFactory
public class EntailGraphFactoryAggregator {

	ThreadPoolExecutor threadPool;

	EntailGraphFactory[] entGrFacts;
	public static Set<String> dsPreds = new HashSet<>();
	public static Set<String> dsRawPredPairs = new LinkedHashSet<>();
	public static Map<String, Map<String, Double>> dsPredToPredToScore = new ConcurrentHashMap<String, Map<String,Double>>();
	public static Map<String, double[]> relsToEmbed;
	public static Map<String, double[]> entsToEmbed;
	public static Set<String> anchorArgPairs;

	// All parameters:

	// public static boolean isCCG = false;
	// public static boolean isTyped = true;
	// static final int minArgPairForPred = 10;
	// static final int minPredForArgPair = 10;// min num of unique predicate
	// for
	// static final int minPredForArg = 30;// min num of unique predicates for
	// arg

	public static boolean onlyDSPreds = true;
	public static boolean rawExtractions = false;// gbooks original (or gbooks in general?)
	public static boolean GBooksCCG = false;
	public static boolean useTimeEx = false;
	public static boolean isCCG = true;
	public static boolean isTyped = false;
	public static TypeScheme typeScheme = TypeScheme.FIGER;
	public static boolean isForeign = false;
	public static boolean lemmatizePredWords = false;// whether we should lemmatize each word in the predicate.
	// if it has been already lemmatized in rel extraction, must be false.

	public static final boolean normalizePredicate = true;// if rawExtraction, wouldn't matter.
	public static final boolean backupToStanNER = false;// You can make this true, but it will take some good time to
														// run!
	public static boolean removeEventEventModifiers = false;
	public static boolean removeStopPreds = false;
	public static final int smoothParam = 0;// 0 means no smoothing
	static int minArgPairForPred = 0;// 100;
	static int minPredForArgPair = 0;// 20;// min num of unique predicates for
										// argpair
	public static int maxPredsTotal = -1;// 35000;
	public static HashSet<String> acceptablePreds;
	static final int minPredForArg = -1;// min num of unique predicates for

	// embeddinb parameters
	public static boolean embBasedScores = false;// use sigmoid(transE score) instead of counts
	public static boolean anchorBasedScores = false;// add anchor-based args to the prev args
	// public static boolean iterateAllArgPairs = true

	public static double sigmoidLocParameter = 10;
	public static double sigmoidScaleParameter = .25;
	// public static double sigmoidLocParameter = 0;
	// public static double sigmoidScaleParameter = 1.0;

	// public static double linkPredThreshold = 1e-12f;
	public static double linkPredThreshold = -1;

	static final int numThreads = 20;
	public static int dsPredNumThreads = 15;
	
	public static ProbModel probModel = ProbModel.Cos;

	static final boolean writePMIorCount = false;// false:count, true: PMI

	static final String relAddress;
	static final String simsFolder;

	static {
		// assert iterateAllArgPairs != anchorBasedScores;
		if (GBooksCCG) {
			relAddress = "gbooks_dir/gbooks_ccg.txt";
			simsFolder = "typedEntGrDir_gbooks_figer_30_30";
		} else if (isForeign) {
			// relAddress = "binary_relations.json";
			// simsFolder = "typedEntGrDir_German";
			relAddress = "binary_rels_chinese.txt";
			simsFolder = "typedEntGrDir_Chinese";
		} else {
			relAddress = "news_gen8_aida.json";
			// relAddress = "news_genC_aida.json";
			// relAddress = "gbooks_norm.txt";
			// simsFolder = "typedEntGrDir_aida_figer_5_5_a";
			// simsFolder = "typedEntGrDir_aida_figer_10_10";
			// simsFolder = "typedEntGrDirC_aida_figer_100_20_35K";
			// simsFolder = "typedEntGrDir_gbooks_onlyLevy";
			// simsFolder = "typedEntGrDir_NS_onlyLevy_san";
			// simsFolder = "typedEntGrDir_aida_untyped_40_40";
			// simsFolder = "typedEntGrDir_aida_untyped_transE_ol_NS_pel";
//			simsFolder = "typedEntGrDir_aida_untyped_transE_ol_NS_pel_10_.25_T1";
			simsFolder = "typedEntGrDir_aida_untyped_transE_ol_NS_cos";
			// simsFolder = "typedEntGrDir_aida_untyped_40_40_transE_Anchor2";
			// simsFolder = "typedEntGrDir_aida_untyped_40_40_anchor";
			// simsFolder = "typedEntGrDir_gbooks_all_20_20";
			// simsFolder = "untypedEntGrDirC_aida_50_50_20K";
		}

		if (maxPredsTotal != -1) {// we should just look at maxPT predicates, no other cutoff
			// minArgPairForPred = 0;
			// minPredForArgPair = 0;

			try {
				formAcceptablePreds();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		// if (embBasedScores) {
		relsToEmbed = new HashMap<>();
		entsToEmbed = new HashMap<>();
		try {
			relsToEmbed = loadEmbeddings("embs/rels2embed_NS_10_10_unique_transE.txt");
			entsToEmbed = loadEmbeddings("embs/ents2embed_NS_10_10_unique_transE.txt");
		} catch (IOException e) {
			e.printStackTrace();
		}
		// }

		if (anchorBasedScores) {
			try {
				anchorArgPairs = loadAnchors();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	static Set<String> loadAnchors() throws IOException {
		BufferedReader br = new BufferedReader(new FileReader("anchors/anchors_NS_untyped_40_40.txt"));
		Set<String> ret = new LinkedHashSet<>();
		String line = null;
		boolean shouldAdd = false;
		while ((line = br.readLine()) != null) {
			if (line.startsWith("hidden state ") || line.equals("")) {
				shouldAdd = true;
				continue;
			}
			if (shouldAdd) {
				int firstIdx = line.indexOf(" ") + 1;
				String anchor = line.substring(firstIdx);
				ret.add(anchor);
			}
			shouldAdd = false;
		}
		br.close();
		System.out.println("anchors:");
		for (String s : ret) {
			System.out.println(s);
		}
		System.out.println();
		return ret;
	}

	static Map<String, double[]> loadEmbeddings(String fname) throws IOException {
		Map<String, double[]> x2emb = new HashMap<>();
		BufferedReader br = new BufferedReader(new FileReader(new File(fname)));
		String line = null;
		while ((line = br.readLine()) != null) {
			String[] ss = line.split("\t");
			String x = ss[0];
			String embVec = ss[1].substring(1, ss[1].length() - 1);

			Scanner sc = new Scanner(embVec);
			List<Double> embs = new ArrayList<>();
			while (sc.hasNext()) {
				embs.add(sc.nextDouble());
			}
			sc.close();
			double[] embsArr = new double[embs.size()];
			for (int i = 0; i < embsArr.length; i++) {
				embsArr[i] = embs.get(i);
			}
			x2emb.put(x, embsArr);
			// System.out.println(x+" "+embsArr[0]);
			// System.out.println("emb size: "+embsArr.length);
		}
		br.close();
		return x2emb;
	}

	// a quick scan over the corpus and find the highest counts
	static void formAcceptablePreds() throws IOException {
		acceptablePreds = new HashSet<>();
		Map<String, Integer> relCounts = new HashMap<String, Integer>();
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(relAddress), "UTF-8"));

		int lineNumbers = 0;
		JsonParser jsonParser = new JsonParser();

		// long t0;
		// long sharedTime = 0;

		String line;
		while ((line = br.readLine()) != null) {
			lineNumbers++;
			// if (lineNumbers == 100000) {
			// break;
			// }
			if (lineNumbers % 100000 == 0) {
				System.out.println("quick scan: " + lineNumbers);
			}
			if (line.startsWith("exception for") || line.contains("nlp.pipeline")) {
				continue;
			}
			try {
				ArrayList<String> relStrs = new ArrayList<>();

				JsonObject jObj = jsonParser.parse(line).getAsJsonObject();

				// typedOp.println("line: " + newsLine);
				JsonArray jar = jObj.get("rels").getAsJsonArray();
				for (int i = 0; i < jar.size(); i++) {
					JsonObject relObj = jar.get(i).getAsJsonObject();
					String relStr = relObj.get("r").getAsString();
					relStrs.add(relStr);
					relStr = relStr.substring(1, relStr.length() - 1);
					String[] parts = relStr.split("::");
					String pred = parts[0];

					if (!Util.acceptablePredFormat(pred, EntailGraphFactoryAggregator.isCCG)) {
						continue;
					}

					String[] predicateLemma;
					if (!EntailGraphFactoryAggregator.isForeign) {
						predicateLemma = Util.getPredicateLemma(pred, EntailGraphFactoryAggregator.isCCG);
					} else {
						predicateLemma = new String[] { pred, "false" };
					}
					pred = predicateLemma[0];

					if (pred.equals("")) {
						continue;
					}

					if (!relCounts.containsKey(pred)) {
						relCounts.put(pred, 1);
					} else {
						relCounts.put(pred, relCounts.get(pred) + 1);
					}

				}

			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		List<SimpleSpot> ss = new ArrayList<>();
		for (String pred : relCounts.keySet()) {
			ss.add(new SimpleSpot(pred, relCounts.get(pred)));
		}

		Collections.sort(ss, Collections.reverseOrder());
		System.out.println("all acceptable preds:");
		for (int i = 0; i < maxPredsTotal; i++) {
			SimpleSpot s = ss.get(i);
			System.out.println(s.spot + " " + s.count);
			acceptablePreds.add(s.spot);
		}
		br.close();
	}

	static int allNonZero = 0;
	static int allEdgeCounts = 0;

	public EntailGraphFactoryAggregator() {
		try {
			dsPreds = new HashSet<>();
			String root = "../../python/gfiles/ent/";
			String[] dsPaths;
			if (isCCG) {
				// dsPaths = new String[] { root + "train1_rels.txt", root + "dev1_rels.txt",
				// root + "test1_rels.txt" };
				dsPaths = new String[] { root + "all_new_rels_l8.txt" };
			} else {
				// dsPaths = new String[] { root + "train1_rels_oie.txt", root +
				// "dev1_rels_oie.txt",
				// root + "test1_rels_oie.txt" };
				dsPaths = new String[] { root + "all_new_rels_oie.txt" };
			}

			for (String dsPath : dsPaths) {
				Util.fillDSPredsandPairs(dsPath, dsPreds, dsRawPredPairs);
			}

			System.err.println("num dspreds: " + dsPreds.size());

			if (onlyDSPreds) {
				System.err.println("all DS Rels" + dsPreds.size());
				for (String s : dsPreds) {
					System.err.println(s);
				}

				System.err.println("all DS pairs: " + dsRawPredPairs.size());
				for (String s : dsRawPredPairs) {
					System.err.println(s);
				}

			}

		} catch (IOException e) {
			e.printStackTrace();
		}

		renewThreadPool();
	}

	void renewThreadPool() {
		final BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(numThreads);
		threadPool = new ThreadPoolExecutor(numThreads, numThreads, 600, TimeUnit.SECONDS, queue);
		// to silently discard rejected tasks. :add new
		// ThreadPoolExecutor.DiscardPolicy()

		threadPool.setRejectedExecutionHandler(new RejectedExecutionHandler() {
			@Override
			public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
				// this will block if the queue is full
				try {
					executor.getQueue().put(r);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});
	}

	void runAllEntGrFacts(String fName, String entTypesFName, String genTypesFName, String typedEntGrDir)
			throws InterruptedException, FileNotFoundException {

		if (!(new File(typedEntGrDir)).exists()) {
			(new File(typedEntGrDir)).mkdirs();
		}

		// Util.loadEntGenTypes(entTypesFName, genTypesFName);

		entGrFacts = new EntailGraphFactory[numThreads];
		for (int i = 0; i < entGrFacts.length; i++) {
			entGrFacts[i] = new EntailGraphFactory(fName, entTypesFName, genTypesFName, typedEntGrDir);
			entGrFacts[i].threadNum = i;
		}

		assignTypesToEntGrFacts();

		for (EntailGraphFactory entGrFact : entGrFacts) {
			if (entGrFact.acceptableTypes.size() == 0) {
				continue;
			}
			System.out.println("num of types: " + entGrFact.acceptableTypes.size());
			// for (String s : entGrFact.acceptableTypes) {
			// System.out.println(s);
			// }
			Runnable extractor = entGrFact;
			entGrFact.runPart = 0;
			threadPool.execute(extractor);
			System.out.println("executing first part: " + entGrFact.threadNum);
			// entGrFact.run();
		}

		threadPool.shutdown();
		// Wait hopefully all threads are finished. If not, forget about it!
		threadPool.awaitTermination(200, TimeUnit.HOURS);
		System.gc();

		// renewThreadPool();
		//
		// for (EntailGraphFactory entGrFact : entGrFacts) {
		// Runnable extractor = entGrFact;
		// entGrFact.runPart= 1;
		// threadPool.execute(extractor);
		// System.out.println("executing second part: " + entGrFact.threadNum);
		// // entGrFact.run();
		// }
		//
		// threadPool.shutdown();
		// // Wait hopefully all threads are finished. If not, forget about it!
		// threadPool.awaitTermination(200, TimeUnit.HOURS);
		// System.gc();
		//
		// renewThreadPool();
		//
		// for (EntailGraphFactory entGrFact : entGrFacts) {
		// Runnable extractor = entGrFact;
		// entGrFact.runPart= 2;
		// threadPool.execute(extractor);
		// System.out.println("executing third part: " + entGrFact.threadNum);
		// // entGrFact.run();
		// }
		//
		// threadPool.shutdown();
		// // Wait hopefully all threads are finished. If not, forget about it!
		// threadPool.awaitTermination(200, TimeUnit.HOURS);

		// EntailGraphFactory aggEntGrFact = aggregate(typedEntGrDir);
		// aggEntGrFact.writeSimilaritiesAll();

		List<SimpleSpot> predCounts = new ArrayList<>();
		for (String pred : EntailGraphFactory.allPredCounts.keySet()) {
			predCounts.add(new SimpleSpot(pred, EntailGraphFactory.allPredCounts.get(pred)));
		}

		Collections.sort(predCounts, Collections.reverseOrder());
		PrintStream op = new PrintStream(new File("allPredCounts0.txt"));
		for (SimpleSpot ss : predCounts) {
			op.println(ss.spot + ss.count);
		}

		op.close();

		op = new PrintStream(new File("predDocs0.txt"));
		for (String pred : EntailGraphFactory.predToDocument.keySet()) {
			op.println(pred + "\tX\t" + EntailGraphFactory.predToDocument.get(pred).trim());
		}
		op.close();
	}

	void assignTypesToEntGrFacts() {
		System.out.println("assigning types");
		HashSet<String> allTypes = new HashSet<>();

		allTypes.add("thing");
		if (EntailGraphFactoryAggregator.isTyped && !EntailGraphFactoryAggregator.isForeign) {
			if (EntailGraphFactoryAggregator.typeScheme == TypeScheme.FIGER) {
				for (String s : Util.getEntToFigerType().values()) {
					allTypes.add(s);
				}
			}
			// else if (EntailGraphFactoryAggregator.typeScheme == TypeScheme.GKG) {
			// for (String s : Util.entToType.values()) {
			// allTypes.add(s);
			// }
			// for (String s : Util.genToType.values()) {
			// allTypes.add(s);
			// }
			// }
			else if (EntailGraphFactoryAggregator.typeScheme == TypeScheme.LDA) {
				for (int i = 0; i < DistrTyping.numTopics; i++) {
					allTypes.add("type" + i);
				}
			}
		}

		ArrayList<String> allTypesArr = new ArrayList<>();

		for (String s : allTypes) {

			allTypesArr.add(s);
		}

		Collections.sort(allTypesArr);

		System.out.println("alltypes size: " + allTypes.size());

		for (int i = 0; i < allTypesArr.size(); i++) {
			// System.out.println("type: " +allTypesArr.get(i) );
			int r = (int) (Math.random() * numThreads);
			// entGrFacts[r].acceptableTypes.add(allTypesArr.get(i));

			for (int j = i; j < allTypesArr.size(); j++) {
				String t1 = allTypesArr.get(i) + "#" + allTypesArr.get(j);
				String t2 = allTypesArr.get(j) + "#" + allTypesArr.get(i);
				r = (int) (Math.random() * numThreads);
				entGrFacts[r].acceptableTypes.add(t1);
				entGrFacts[r].acceptableTypes.add(t2);
			}
		}

		System.out.println("types assigned");

	}

	// EntailGraphFactory aggregate(String typedEntGrDir) {
	// EntailGraphFactory aggEntGrFact = new EntailGraphFactory(typedEntGrDir);
	//
	// for (EntailGraphFactory entGrFact : entGrFacts) {
	// addTypesToGraphs(aggEntGrFact.typesToSimpleGraph,
	// entGrFact.typesToSimpleGraph);
	// addTypesToGraphs(aggEntGrFact.typesToSimpleGraphX,
	// entGrFact.typesToSimpleGraphX);
	// addTypesToGraphs(aggEntGrFact.typesToSimpleGraphY,
	// entGrFact.typesToSimpleGraphY);
	// addTypesToGraphs(aggEntGrFact.typesToSimpleGraphUnaryX,
	// entGrFact.typesToSimpleGraphUnaryX);
	// addTypesToGraphs(aggEntGrFact.typesToSimpleGraphUnaryY,
	// entGrFact.typesToSimpleGraphUnaryY);
	//
	// addTypeToOrderedType(aggEntGrFact.typeToOrderedType,
	// entGrFact.typeToOrderedType);
	//
	// }
	// return aggEntGrFact;
	// }

	// 1 = 2
	// private void addTypesToGraphs(HashMap<String, SimpleEntailGraph>
	// typeToGr1,
	// HashMap<String, SimpleEntailGraph> typeToGr2) {
	// for (String types : typeToGr2.keySet()) {
	// typeToGr1.put(types, typeToGr2.get(types));
	// }
	// }

	// private void addTypeToOrderedType(HashMap<String, String> h1,
	// HashMap<String, String> h2) {
	// for (String types : h2.keySet()) {
	// h1.put(types, h2.get(types));
	// }
	// }

	public static void main(String[] args) throws InterruptedException, FileNotFoundException {
		System.out.println("sigmoid params: "+ sigmoidScaleParameter+ " "+ sigmoidLocParameter);
		String fileName;
		String typedEntGrDir;
		System.out.println("here");
		if (args.length == 5) {
			fileName = args[0];
//			entTypesFName = args[1];
//			genTypesFName = args[2];
			typedEntGrDir = args[3];
//			numThreads = Integer.parseInt(args[4]);
		} else {
			// fileName = "test.json";
			// fileName = "news_gen5_unlinked.txt";
			// fileName = "news_gen6_aida.txt";
			fileName = relAddress;
			// fileName = "OIE/oie_unlinked.json";
			// fileName = "gbooks_norm.txt";
			// fileName = "gnews.txt";
			// fileName = "news_NEs_NEL.json";
			// fileName = "test2.json";
			// fileName = "test_time.json";
			// entTypesFName = "entTypes.txt";
			// genTypesFName = "genTypes.txt";
			// typedEntGrDir = "typedEntGrDir_untyped_unlinked";
			// typedEntGrDir = "typedEntGrDir_aida_untyped_30_30";
			// typedEntGrDir = "typedEntGrDir_aida_untyped_20_20";
			typedEntGrDir = simsFolder;
			// typedEntGrDir = "typedEntGrDir_gbooks_all";
//			numThreads = EntailGraphFactoryAggregator.numThreads;
		}

		EntailGraphFactoryAggregator agg = new EntailGraphFactoryAggregator();
		if (EntailGraphFactoryAggregator.typeScheme == TypeScheme.LDA) {
			DistrTyping.loadLDATypes();
		}
		System.out.println("fileName: " + fileName);
		agg.runAllEntGrFacts(fileName, "", "", typedEntGrDir);

	}

	public enum TypeScheme {
		GKG, FIGER, LDA
	}
	
	public enum ProbModel {
		PE, PEL, PL, Cos;
	}
}
