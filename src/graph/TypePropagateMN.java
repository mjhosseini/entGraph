package graph;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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

import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;

public class TypePropagateMN {
	ThreadPoolExecutor threadPool;
	ArrayList<PGraph> pGraphs;
	// public final static float edgeThreshold = -1;// edgeThreshold
	static int numThreads = 30;
	static int numIters = 4;
	public static double lmbda = .001;// lmbda for L1 regularization
	public static double smoothParam = 20.0;
	static final String tPropSuffix = "_tProp_sm20_i4.txt";
	Map<String, Integer> graphToNumEdges;
	String compatiblesPath = "../../python/gfiles/ent/compatibles_all.txt";
	static Map<String, Double> compatibles;
	static Map<String, Set<Integer>> rawPred2PGraphs;
	static int allPropEdges = 0;
	static double objChange = 0;

	public TypePropagateMN(String root) {
		PGraph.emb = false;
		PGraph.suffix = "_sim.txt";
		PGraph.formBinaryGraph = false;
		// PGraph.edgeThreshold = edgeThreshold;
		try {
			readCompatibles();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		readPGraphs(root);

		System.gc();
		System.err.println("after reading all pgraphs");
		memStat();

		MNPropagateSims();

	}

	static void memStat() {
		int mb = 1024 * 1024;
		long usedMb = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / mb;
		System.out.println("usedMb: " + usedMb);
	}

	void readPGraphs(String root) {
		pGraphs = new ArrayList<>();
		graphToNumEdges = new HashMap<String, Integer>();

		File folder = new File(root);
		File[] files = folder.listFiles();
		Arrays.sort(files);

		rawPred2PGraphs = new HashMap<>();

		// boolean seenLoc = false;//TODO: be carful
		int gc = 0;
		for (File f : files) {

			String fname = f.getName();

			// TODO: remove
			// if (gc==0) {
			// fname = "astral_body#thing_sim.txt";
			// }
			// if (fname.startsWith("location#title_sim.txt")) {
			// seenLoc = true;
			// }
			// if (!seenLoc) {
			// continue;
			// }

			if (!fname.contains(PGraph.suffix)) {
				continue;
			}

//			if (gc == 100) {
//				break;
//			}

			System.out.println("fname: " + fname);
			PGraph pgraph = new PGraph(root + fname);
			if (pgraph.nodes.size() == 0) {
				continue;
			}
			pgraph.g0 = pgraph.formWeightedGraph(pgraph.sortedEdges, pgraph.nodes.size());
			pgraph.clean();

			if (pgraph.nodes.size() == 0) {
				continue;
			}

			for (String s : pgraph.pred2node.keySet()) {
				String rawPred = s.split("#")[0];
				if (!rawPred2PGraphs.containsKey(rawPred)) {
					rawPred2PGraphs.put(rawPred, new HashSet<>());
				}
				rawPred2PGraphs.get(rawPred).add(gc);
			}

			pGraphs.add(pgraph);
			String[] ss = pgraph.types.split("#");
			String types2 = ss[1] + "#" + ss[0];
			graphToNumEdges.put(pgraph.types, pgraph.sortedEdges.size());
			graphToNumEdges.put(types2, pgraph.sortedEdges.size());

			System.out.println("allEdgesRem, allEdges: " + PGraph.allEdgesRemained + " " + PGraph.allEdges);
			gc++;
		}
	}

	static double getCompatibleScore(String t1, String t2, boolean aligned, String tp1, String tp2) {
		
		String comb = t1 + "#" + t2 + "#" + aligned + "#" + tp1 + "#" + tp2;
		if (t1.equals(tp1) && t2.equals(tp2)) {
			return 1;
		} else if (compatibles.containsKey(comb)) {
			double ret = compatibles.get(comb);
			// System.out.println("compscore: " + comb + " " + ret);
			return ret;
		} else {
			return 1.0/smoothParam;
		}
	}

	void readCompatibles() throws FileNotFoundException {
		Scanner sc = new Scanner(new File(compatiblesPath));
		compatibles = new HashMap<>();
		while (sc.hasNextLine()) {
			String line = sc.nextLine();
			line = line.replace("_1", "");
			line = line.replace("_2", "");

			String[] ss = line.split(" ");
			double prob = (Float.parseFloat(ss[1]) + 1) / (Float.parseFloat(ss[2]) + smoothParam);
			System.out.println("compatibles: " + ss[0] + " " + prob);
			compatibles.put(ss[0], prob);
		}
		sc.close();
	}

	// for now, just one iteration
	void MNPropagateSims() {

		for (int iter = 0; iter < numIters; iter++) {
			objChange = 0;
			for (PGraph pgraph : pGraphs) {

				// initialize gMN (next g) based on g0 (cur g)
				int N = pgraph.g0.vertexSet().size();
				pgraph.gMN = new DefaultDirectedWeightedGraph<>(DefaultWeightedEdge.class);
				for (int i = 0; i < N; i++) {
					pgraph.gMN.addVertex(i);
					DefaultWeightedEdge ee = pgraph.gMN.addEdge(i, i);
					pgraph.gMN.setEdgeWeight(ee, 1);
				}

				// maybe for performance?
				// for (Edge e : sortedEdges) {
				// DefaultWeightedEdge ee = g0.addEdge(e.i, e.j);
				// if (ee == null) {
				// continue;// In one case, because of replacing '`', we had an
				// // edge twice in sortedEdges
				// }
				// g0.setEdgeWeight(ee, e.sim);
				// }
			}

			try {
				propagateAll(0);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}

			// Now, just let's get the average for gMNs

			try {
				propagateAll(1);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}

			// now let's put g0 = gMN
			if (iter != numIters - 1) {
				for (PGraph pgraph : pGraphs) {
					pgraph.g0 = pgraph.gMN;
				}
			}
			System.out.println("obj change: "+objChange);
		}

		// now, let's write the results
		try {
			propagateAll(2);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		
	}

	// void writeEmbeddingResults(PGraph pgraph,
	// String fnameTProp) {
	// // list of all predicates can be found from the last graph. The indexes
	// // are also the same (if existing) with previous graphs
	// PrintStream op = null;
	// try {
	// op = new PrintStream(new File(fnameTProp));
	// } catch (Exception e) {
	// }
	//// List<String> predList = allPredsList.get(allPredsList.size() - 1);
	// DefaultDirectedWeightedGraph<Integer, DefaultWeightedEdge> gMN = pgraph.gMN;
	// int N = pgraph.idx2node.size();
	//
	// op.println(pgraph.name + " " + " type propagation num preds: " + N);
	//
	// for (int i = 0; i < N; i++) {
	// String pred = pgraph.idx2node.get(i).id;
	// op.println("predicate: " + pred);
	// op.println("num max neighbors: " + gMN.outgoingEdgesOf(i).size());
	// op.println();
	// DefaultDirectedWeightedGraph<Integer, DefaultWeightedEdge> thisG =
	// gs.get(iter);
	// op.println("iter " + iter + " sims");
	// List<SimpleScore> scores = new ArrayList<>();
	// if (thisG.containsVertex(i)) {
	// for (DefaultWeightedEdge e : thisG.outgoingEdgesOf(i)) {
	// int j = thisG.getEdgeTarget(e);
	// double w = thisG.getEdgeWeight(e);
	// scores.add(new SimpleScore("", predList.get(j), (float) w));
	// }
	//
	// Collections.sort(scores, Collections.reverseOrder());
	// for (SimpleScore sc : scores) {
	// op.println(sc.pred2 + " " + sc.score);
	// }
	// }
	// op.println();
	// }
	// op.close();
	// }

	public static void main(String[] args) {
		String root = PGraph.root;
		TypePropagateMN tpmn = new TypePropagateMN(root);
	}

	void propagateAll(int runIdx) throws InterruptedException {

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

		for (int threadIdx = 0; threadIdx < numThreads; threadIdx++) {
			LabelPropagateMN lmn = new LabelPropagateMN(pGraphs, threadIdx, numThreads, runIdx);
			threadPool.execute(lmn);
		}

		threadPool.shutdown();
		// Wait hopefully all threads are finished. If not, forget about it!
		threadPool.awaitTermination(200, TimeUnit.HOURS);

	}

}
