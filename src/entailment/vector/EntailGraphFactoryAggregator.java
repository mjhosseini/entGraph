package entailment.vector;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import entailment.Util;
import entailment.entityLinking.DistrTyping;
import entailment.entityLinking.SimpleSpot;

//This is to do multithreading over EntGrFactory
public class EntailGraphFactoryAggregator {

	ThreadPoolExecutor threadPool;

	EntailGraphFactory[] entGrFacts;
	public static HashSet<String> dsPreds = new HashSet<>();

	// All parameters:

	// public static boolean isCCG = false;
	// public static boolean isTyped = true;
	// static final int minArgPairForPred = 10;
	// static final int minPredForArgPair = 10;// min num of unique predicate
	// for
	// static final int minPredForArg = 30;// min num of unique predicates for
	// arg

	public static boolean onlyDSPreds = false;
	public static boolean rawExtractions = false;// gbooks
	public static boolean useTimeEx = false;
	public static boolean isCCG = true;
	public static boolean isTyped = true;
	public static boolean figerTypes = true;
	public static TypeScheme typeScheme = TypeScheme.FIGER;
	public static boolean lemmatizePredWords = false;// if it has been already lemmatized in rel extraction. Must be
														// false.

	public static final boolean lemmatizePredicate = true;
	public static final boolean backupToStanNER = false;//You can make this true, but it will take some good time to run!
	public static final int smoothParam = 0;// 0 means no smoothing
	static final int minArgPairForPred = 3;
	static final int minPredForArgPair = 3;// min num of unique predicates for
											// argpair
	static final int minPredForArg = -1;// min num of unique predicates for 

	static final String relAddress = "news_gen8_aida.json";
	static final String simsFolder = "typedEntGrDir_aida_figer_3_3_f";
	static final int numThreads = 15;
	
	static final boolean writePMIorCount=false;//false:count, true: PMI

	static int allNonZero = 0;
	static int allEdgeCounts = 0;

	public EntailGraphFactoryAggregator() {
		try {
			dsPreds = new HashSet<>();
			String root = "data/ent/";
			String[] dsPaths;
			if (isCCG) {
				// dsPaths = new String[] { root + "train1_rels.txt", root + "dev1_rels.txt",
				// root + "test1_rels.txt" };
				dsPaths = new String[] { root + "train_new_rels_l5.txt", root + "dev_new_rels_l5.txt",
						root + "test_new_rels_l5.txt" };
			} else {
				dsPaths = new String[] { root + "train1_rels_oie.txt", root + "dev1_rels_oie.txt",
						root + "test1_rels_oie.txt" };
			}

			for (String dsPath : dsPaths) {
				HashSet<String> rels = Util.loadAllDSPreds(dsPath);
				for (String r : rels) {
					dsPreds.add(r);
				}
			}

			System.err.println("num dspreds: " + dsPreds.size());

			if (onlyDSPreds) {
				System.err.println("all DS Rels");
				for (String s : dsPreds) {
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
		if (EntailGraphFactoryAggregator.isTyped) {
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
		String fileName;
		String entTypesFName;
		String genTypesFName;
		String typedEntGrDir;
		int numThreads;
		System.out.println("here");
		if (args.length == 5) {
			fileName = args[0];
			entTypesFName = args[1];
			genTypesFName = args[2];
			typedEntGrDir = args[3];
			numThreads = Integer.parseInt(args[4]);
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
			numThreads = EntailGraphFactoryAggregator.numThreads;
		}

		EntailGraphFactoryAggregator agg = new EntailGraphFactoryAggregator();
		if (EntailGraphFactoryAggregator.typeScheme == TypeScheme.LDA) {
			DistrTyping.loadLDATypes();
		}
		agg.runAllEntGrFacts(fileName, "", "", typedEntGrDir);

	}

	public enum TypeScheme {
		GKG, FIGER, LDA
	}
}
