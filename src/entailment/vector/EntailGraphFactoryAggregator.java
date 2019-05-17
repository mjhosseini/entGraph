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

import constants.ConstantsAgg;
import entailment.Util;
import entailment.linkingTyping.DistrTyping;
import entailment.linkingTyping.SimpleSpot;
import entailment.linkingTyping.StanfordNERHandler;
import entailment.randWalk.RandWalkMatrix;

//This is to do multithreading over EntGrFactory
public class EntailGraphFactoryAggregator {

	ThreadPoolExecutor threadPool;

	EntailGraphFactory[] entGrFacts;
	public static Set<String> dsPreds = new HashSet<>();
	public static Set<String> dsRawPredPairs = new LinkedHashSet<>();
	public static Map<String, Map<String, Double>> dsPredToPredToScore = new ConcurrentHashMap<String, Map<String, Double>>();
	public static Set<String> anchorArgPairs;
	public static Set<String> acceptablePreds;
	public static Map<String, Set<String>> typesToAcceptablePreds;
	public static Map<String, Set<String>> typesToAcceptableArgPairs;

	// These parameters are intended to be fixed, or not parameters anymore!

	public static TypeScheme typeScheme = TypeScheme.FIGER;
	public static final int smoothParam = 0;// 0 means no smoothing
	public static boolean anchorBasedScores = false;// add anchor-based args to the prev args

	// public static double linkPredThreshold = 1e-12f;
	public static double linkPredThreshold = -1;

	static final boolean writePMIorCount = false;// false:count, true: PMI

	public static List<Double> allPosLinkPredProbs = Collections.synchronizedList(new ArrayList<>());// just used to
																										// tune scale
																										// and shape
																										// parameters
	public static Map<String, int[]> cutOffsNS;
	public static Map<String, Integer> predNumArgPairsNS;
	public static Map<String, Integer> type2RankNS;

	static int allNonZero = 0;
	static int allEdgeCounts = 0;
	static int numAllTuples = 0;
	static int numAllTuplesPlusReverse = 0;

	static {
		if (ConstantsAgg.maxPredsTotal != -1) {// we should just look at maxPT predicates, no other cutoff
			// minArgPairForPred = 0;
			// minPredForArgPair = 0;

			try {
				formAcceptablePreds();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		if (ConstantsAgg.cutoffBasedonNSGraphs || ConstantsAgg.removeGGFromTopPairs) {
			try {
				cutOffsNS = getAllCutoffs();
				predNumArgPairsNS = getAllPredArgPairSizes();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

//		if (ConstantsAgg.maxPredsTotalTypeBased != -1) {
//			try {
//				formAcceptablePredsAndArgPairsTypeBased();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		}

		// if (embBasedScores) {
		RandWalkMatrix.relsToEmbed = new HashMap<>();
		RandWalkMatrix.entsToEmbed = new HashMap<>();
		RandWalkMatrix.tripleToScore = new HashMap<>();
		try {
			if (ConstantsAgg.linkPredModel == LinkPredModel.TransE) {
				RandWalkMatrix.relsToEmbed = loadEmbeddings("embs/rels2embed_NS_10_10_unique_transE.txt");
				RandWalkMatrix.entsToEmbed = loadEmbeddings("embs/ents2embed_NS_10_10_unique_transE.txt");
			}
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

	public static Map<String, int[]> getAllCutoffs() throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(ConstantsAgg.NSSizesAddress));
		String line;
		Map<String, int[]> ret = new HashMap<>();
		List<SimpleSpot> typeSizes = new ArrayList<>();
		while ((line = br.readLine()) != null) {
			String[] ss = line.split("\t");
			String types = ss[0];
			String[] t_ss = types.split("#");
			String type_reverse = t_ss[1] + "#" + t_ss[0];
			int[] sizes = new int[2];
			sizes[0] = Integer.parseInt(ss[1]);
			sizes[1] = Integer.parseInt(ss[2]);
			ret.put(types, sizes);
			ret.put(type_reverse, sizes);
			typeSizes.add(new SimpleSpot(types, sizes[0]));
		}

		Collections.sort(typeSizes, Collections.reverseOrder());
		int i = 0;
		type2RankNS = new HashMap<>();
		for (SimpleSpot ss : typeSizes) {
			type2RankNS.put(ss.spot, i);
			String[] ps = ss.spot.split("#");
			type2RankNS.put(ps[1] + "#" + ps[0], i);
			i++;
		}

		br.close();
		return ret;
	}

	public static Map<String, Integer> getAllPredArgPairSizes() throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(ConstantsAgg.NSPredSizesAddress));
		String line;
		Map<String, Integer> ret = new HashMap<>();
		while ((line = br.readLine()) != null) {
			String[] ss = line.split("\t");
			String pred = ss[0];
			int size = Integer.parseInt(ss[1]);
			ret.put(pred, size);
		}
		br.close();
		return ret;
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

	public static Set<String> loadAllTriples(String fname) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(new File(fname)));
		String line = null;
		Set<String> ret = new HashSet<>();

		while ((line = br.readLine()) != null) {
			String[] ss = line.split("\t");
			try {
				ret.add(ss[0] + "#" + ss[1] + "#" + ss[2]);
			} catch (Exception e) {
			}
		}
		return ret;
	}

	public static Map<String, double[]> loadEmbeddings(String fname) throws IOException {
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
		}
		br.close();
		return x2emb;
	}

	// a quick scan over the corpus and find the highest counts
	static void formAcceptablePreds() throws IOException {
		acceptablePreds = new HashSet<>();
		Map<String, Integer> relCounts = new HashMap<String, Integer>();
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(ConstantsAgg.relAddress), "UTF-8"));

		int lineNumbers = 0;
		JsonParser jsonParser = new JsonParser();

		// long t0;
		// long sharedTime = 0;

		String line;
		while ((line = br.readLine()) != null) {
			lineNumbers++;
			
			if (lineNumbers % 100000 == 0) {
				System.out.println("quick scan: " + lineNumbers);
			}
			if (line.startsWith("exception for") || line.contains("nlp.pipeline")) {
				continue;
			}
			try {

				JsonObject jObj = jsonParser.parse(line).getAsJsonObject();

				// typedOp.println("line: " + newsLine);
				JsonArray jar = jObj.get("rels").getAsJsonArray();
				for (int i = 0; i < jar.size(); i++) {
					JsonObject relObj = jar.get(i).getAsJsonObject();
					String relStr = relObj.get("r").getAsString();
					
					relStr = relStr.substring(1, relStr.length() - 1);
					String[] parts = relStr.split("::");
					String pred = parts[0];

					if (!Util.acceptablePredFormat(pred, ConstantsAgg.isCCG)) {
						continue;
					}

					String[] predicateLemma;
					if (!ConstantsAgg.isForeign) {
						predicateLemma = Util.getPredicateNormalized(pred, ConstantsAgg.isCCG);
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
		for (int i = 0; i < ConstantsAgg.maxPredsTotal; i++) {
			SimpleSpot s = ss.get(i);
			System.out.println(s.spot + " " + s.count);
			acceptablePreds.add(s.spot);
		}
		br.close();
	}

//	// a quick scan over the corpus and find the highest counts
//	static void formAcceptablePredsAndArgPairsTypeBased() throws IOException {
//		typesToAcceptablePreds = new HashMap<>();
//		typesToAcceptableArgPairs = new HashMap<>();
//		
//		Map<String, Map<String, Integer>> relCounts = new HashMap<>();
//		Map<String, Map<String, Integer>> argPairCounts = new HashMap<>();
//
//		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(relAddress), "UTF-8"));
//
//		int lineNumbers = 0;
//		JsonParser jsonParser = new JsonParser();
//
//		// long t0;
//		// long sharedTime = 0;
//
//		String line;
//		while ((line = br.readLine()) != null) {
//			lineNumbers++;
////			if (lineNumbers == 100000) {
////				break;
////			}
//			if (lineNumbers % 100000 == 0) {
//				System.out.println("quick scan: " + lineNumbers);
//			}
//			if (line.startsWith("exception for") || line.contains("nlp.pipeline")) {
//				continue;
//			}
//			try {
//				
//				//start: exactly copied from EGA
//				
//				JsonObject jObj = jsonParser.parse(line).getAsJsonObject();
//
//				// typedOp.println("line: " + newsLine);
//				JsonArray jar = jObj.get("rels").getAsJsonArray();
//				for (int i = 0; i < jar.size(); i++) {
//					JsonObject relObj = jar.get(i).getAsJsonObject();
//					String relStr = relObj.get("r").getAsString();
//					relStr = relStr.substring(1, relStr.length() - 1);
//					String[] parts = relStr.split("::");
//					String pred = parts[0];
//
//					if (!Util.acceptablePredFormat(pred, ConstantsAgg.isCCG)) {
//						continue;
//					}
//					
//					
//					String[] predicateLemma;
//					if (!ConstantsAgg.isForeign) {
//						predicateLemma = Util.getPredicateNormalized(pred, ConstantsAgg.isCCG);
//					} else {
//						predicateLemma = new String[] { pred, "false" };
//					}
//
//					pred = predicateLemma[0];
//					
//					if (ConstantsAgg.removeStopPreds && Util.stopPreds.contains(pred)) {
//						continue;
//					}
//					
//					if (pred.equals("")) {
//						continue;
//					}
//
//					
//					for (int j = 1; j < 3; j++) {
//						parts[j] = Util.simpleNormalize(parts[j]);
//					}
//					
//					String type1 = null, type2 = null;
//
//					if (ConstantsAgg.isForeign) {
//						if (ConstantsAgg.isTyped) {
//							type1 = parts[3];// .substring(1);
//							type2 = parts[4];// .substring(1);
////							System.out.println("types foreign: " + type1 + " " + type2);
//						} else {
//							type1 = "thing";
//							type2 = "thing";
//						}
//
//					} else if (!ConstantsAgg.useTimeEx) {
//						try {
//							type1 = Util.getType(parts[1], parts[3].charAt(0) == 'E', null);
//							type2 = Util.getType(parts[2], parts[3].charAt(1) == 'E', null);
//						} catch (Exception e) {
//							System.out.println("t exception for: " + line);
//						}
//					} else {
//						try {
//							type1 = Util.getType(parts[1], true, null);
//							type2 = Util.getType(parts[2], true, null);
//						} catch (Exception e) {
//							System.out.println("t exception for: " + line);
//						}
//					}
//
//					String arg1;
//					String arg2;
//
//					// false means args are reversed.
//					if (predicateLemma[1].equals("false")) {
//						arg1 = parts[1];
//						arg2 = parts[2];// type1 and type2 are fine
//					} else {
//						arg1 = parts[2];
//						arg2 = parts[1];
//						// let's swap type1 and type2
//						String tmp = type1;
//						type1 = type2;
//						type2 = tmp;
//					}
//
//					if (pred.equals("")) {
//						continue;
//					}
//					
//					//end: exactly copied from EGA. Now we have pred, arg1 (t1) and arg2 (t2)
//					
//					if (ConstantsAgg.removeGGFromTopPairs
//							&& EntailGraphFactoryAggregator.type2RankNS.containsKey(type1 + "#" + type2)
//							&& EntailGraphFactoryAggregator.type2RankNS
//									.get(type1 + "#" + type2) < ConstantsAgg.numTopTypePairs
//							&& parts[3].charAt(0) == 'G' && parts[3].charAt(1) == 'G') {
//						continue;
//					}
//
//					String types = type1 + "#" + type2;
//					String types_reverse = type2 + "#" + type1;
//
//					relCounts.putIfAbsent(types, new HashMap<String, Integer>());
//					relCounts.putIfAbsent(types_reverse, new HashMap<String, Integer>());
//					argPairCounts.putIfAbsent(types, new HashMap<String, Integer>());
//					argPairCounts.putIfAbsent(types_reverse, new HashMap<String, Integer>());
//
//					if (type1.equals(type2)) {
//
//						String typeD = type1 + "_1" + "#" + type1 + "_2";
//						String typeR = type1 + "_2" + "#" + type1 + "_1";
//
//						String predD = pred + "#" + typeD;
//						String predR = pred + "#" + typeR;
//
//						relCounts.get(types).putIfAbsent(predD, 0);
//						relCounts.get(types).put(predD, relCounts.get(types).get(predD) + 1);
//						relCounts.get(types).putIfAbsent(predR, 0);
//						relCounts.get(types).put(predR, relCounts.get(types).get(predR) + 1);
//
//						String argPairD = arg1 + "#" + arg2;
//						String argPairR = arg2 + "#" + arg1;
//
//						argPairCounts.get(types).putIfAbsent(argPairD, 0);
//						argPairCounts.get(types).put(argPairD, argPairCounts.get(types).get(argPairD) + 1);
//						argPairCounts.get(types).putIfAbsent(argPairR, 0);
//						argPairCounts.get(types).put(argPairR, argPairCounts.get(types).get(argPairR) + 1);
//
//					} else {
//						String predD = pred + "#" + type1 + "#" + type2;
//						String argPairD = arg1 + "#" + arg2;
//
//						relCounts.get(types).putIfAbsent(predD, 0);
//						relCounts.get(types_reverse).putIfAbsent(predD, 0);
//						relCounts.get(types).put(predD, relCounts.get(types).get(predD) + 1);
//						relCounts.get(types_reverse).put(predD, relCounts.get(types_reverse).get(predD) + 1);
//
//						argPairCounts.get(types).putIfAbsent(argPairD, 0);
//						argPairCounts.get(types).put(argPairD, argPairCounts.get(types).get(argPairD) + 1);
//						argPairCounts.get(types_reverse).putIfAbsent(argPairD, 0);
//						argPairCounts.get(types_reverse).put(argPairD,
//								argPairCounts.get(types_reverse).get(argPairD) + 1);
//
//					}
//
//				}
//
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//		}
//
//		br.close();
//
//		for (String types : relCounts.keySet()) {
//			formAcceptableObjects(relCounts.get(types), types, typesToAcceptablePreds, 0);
//			formAcceptableObjects(argPairCounts.get(types), types, typesToAcceptableArgPairs, 1);
//		}
//	}

//	// either preds or argpairs; cutoffIdx: 0 for preds and 1 for argPairs
//	static void formAcceptableObjects(Map<String, Integer> thisObjCounts, String types,
//			Map<String, Set<String>> typesToAcceptableObjects, int cutoffIdx) {
//		typesToAcceptableObjects.put(types, new LinkedHashSet<>());
//		List<SimpleSpot> ss = new ArrayList<>();
//		for (String obj : thisObjCounts.keySet()) {
//			// System.out.println("pred's count: "+pred + " " + thisRelCounts.get(pred));
//			ss.add(new SimpleSpot(obj, thisObjCounts.get(obj)));
//		}
//
//		Collections.sort(ss, Collections.reverseOrder());
//		int numAllowedUB;
//		if (cutoffIdx == 0) {
//			numAllowedUB = ConstantsAgg.maxPredsTotalTypeBased;// an upper bound
//		} else {
//			numAllowedUB = ConstantsAgg.maxArgPairsTotalTypeBased;// an upper bound
//		}
//
//		boolean shouldCutoffNSBased = ConstantsAgg.cutoffBasedonNSGraphs
//				&& EntailGraphFactoryAggregator.cutOffsNS.containsKey(types)
//				&& EntailGraphFactoryAggregator.type2RankNS.get(types) < ConstantsAgg.numTopTypePairs;
//
//		if (shouldCutoffNSBased && EntailGraphFactoryAggregator.cutOffsNS.containsKey(types)) {
//			// we still multiply by 1+eps because we will cutoff based on aparis first and
//			// don't want to bias the results...
//			numAllowedUB = (int) (EntailGraphFactoryAggregator.cutOffsNS.get(types)[cutoffIdx] * 1.1);
//		}
//
//		System.out.println("num allowed for " + types + " " + (cutoffIdx == 0 ? "pred " : "argpairs ") + numAllowedUB);
//		if (cutoffIdx == 0) {
//			System.out.println("all acceptable preds for : " + types);
//		} else {
//			System.out.println("all acceptable argpairs for : " + types);
//		}
//
//		for (int i = 0; i < Math.min(ss.size(), numAllowedUB); i++) {
//			SimpleSpot s = ss.get(i);
//			typesToAcceptableObjects.get(types).add(s.spot);
//			System.out.println(s.spot + " " + s.count);
//		}
//	}

	public EntailGraphFactoryAggregator() {
		try {
			dsPreds = new HashSet<>();
			String root = "../../python/gfiles/ent/";
			String[] dsPaths;
			if (ConstantsAgg.isCCG) {
				// dsPaths = new String[] { root + "train1_rels.txt", root + "dev1_rels.txt",
				// root + "test1_rels.txt" };
				dsPaths = new String[] { root + "all_new_rels_l8.txt" };// TODO: change this to combined!
			} else {
				// dsPaths = new String[] { root + "train1_rels_oie.txt", root +
				// "dev1_rels_oie.txt",
				// root + "test1_rels_oie.txt" };
				dsPaths = new String[] { root + "all_new_rels_oie.txt" };
			}

			if (ConstantsAgg.onlyDSPreds) {
				for (String dsPath : dsPaths) {
					Util.fillDSPredsandPairs(dsPath, dsPreds, dsRawPredPairs);
				}

				System.err.println("num dspreds: " + dsPreds.size());

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
		final BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(ConstantsAgg.numThreads);
		threadPool = new ThreadPoolExecutor(ConstantsAgg.numThreads, ConstantsAgg.numThreads, 600, TimeUnit.SECONDS,
				queue);
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

		entGrFacts = new EntailGraphFactory[ConstantsAgg.numThreads];
		for (int i = 0; i < entGrFacts.length; i++) {
			entGrFacts[i] = new EntailGraphFactory(fName, entTypesFName, genTypesFName, typedEntGrDir);
			entGrFacts[i].threadNum = i;
		}

		assignTypesToEntGrFacts();
		
		if (ConstantsAgg.backupToStanNER) {
			try {
				StanfordNERHandler.loadNER(ConstantsAgg.NERAddress);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

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

//		op = new PrintStream(new File("predDocs0.txt"));
//		for (String pred : EntailGraphFactory.predToDocument.keySet()) {
//			op.println(pred + "\tX\t" + EntailGraphFactory.predToDocument.get(pred).trim());
//		}
//		op.close();
	}

	void assignTypesToEntGrFacts() {
		System.out.println("assigning types");
		Set<String> allTypes = new HashSet<>();

		allTypes.add("thing");
		if (ConstantsAgg.isTyped) {
			if (!ConstantsAgg.isForeign) {
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
			} else {
				Scanner sc;
				try {
					sc = new Scanner(new File(ConstantsAgg.foreinTypesAddress));
					while (sc.hasNextLine()) {
						String s = sc.nextLine();
						allTypes.add(s);
					}
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			}
		}

		List<String> allTypesArr = new ArrayList<>();

		for (String s : allTypes) {

			allTypesArr.add(s);
		}

		Collections.sort(allTypesArr);

		System.out.println("alltypes size: " + allTypes.size());

		for (int i = 0; i < allTypesArr.size(); i++) {
			// System.out.println("type: " +allTypesArr.get(i) );
			int r = (int) (Math.random() * ConstantsAgg.numThreads);
			// entGrFacts[r].acceptableTypes.add(allTypesArr.get(i));

			for (int j = i; j < allTypesArr.size(); j++) {
				String t1 = allTypesArr.get(i) + "#" + allTypesArr.get(j);
				String t2 = allTypesArr.get(j) + "#" + allTypesArr.get(i);
				r = (int) (Math.random() * ConstantsAgg.numThreads);
				System.out.println("adding " + t1 + " to thread " + r);
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
		if (ConstantsAgg.linkPredBasedRandWalk) {
			RandWalkMatrix.loadLinkPredInfo();
		}
		EntailGraphFactoryAggregator agg = new EntailGraphFactoryAggregator();
		if (EntailGraphFactoryAggregator.typeScheme == TypeScheme.LDA) {
			DistrTyping.loadLDATypes();
		}
		System.out.println("fileName: " + ConstantsAgg.relAddress);
		agg.runAllEntGrFacts(ConstantsAgg.relAddress, "", "", ConstantsAgg.simsFolder);

	}

	public enum TypeScheme {
		GKG, FIGER, LDA
	}

	public enum ProbModel {
		PE, PEL, PL, Cos, RandWalk;
	}

	public enum LinkPredModel {
		TransE, ConvE;
	}
}
