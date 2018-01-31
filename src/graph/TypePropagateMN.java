package graph;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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

import edu.stanford.nlp.util.CollectionUtils;

public class TypePropagateMN {
	ThreadPoolExecutor threadPool;
	ArrayList<PGraph> pGraphs;
	// public final static float edgeThreshold = -1;// edgeThreshold
	static int numThreads = 60;
	static int numIters = 4;
	public static double lmbda = .001;// lmbda for L1 regularization
	public static double lmbda2 = 0.0;
	public static double tau = .3;
	public static double smoothParam = 5.0;
	// static final String tPropSuffix = "_tProp_i4_predBased_areg_trans_1.0.txt";
	 static final String tPropSuffix = "_tProp_trans_0_.3_obj2_sn.txt";
//	static final String tPropSuffix = "_tProp_u.txt";
	public final static boolean addTargetRels = false;
	static final boolean predBasedPropagation = true;
	static final boolean sizeBasedPropagation = false;
	static final boolean obj1 = false;// obj1: max(w-tau), false: 1(w>tau)w
	
	Map<String, Integer> graphToNumEdges;
	String compatiblesPath = "../../python/gfiles/ent/compatibles_all.txt";
	static Map<String, Double> compatibles;
	static Map<String, Integer> predToOcc;// ex: (visit.1,visit.2)#person#location => 10344
	static Map<String, Set<Integer>> rawPred2PGraphs;
	static Map<String, Double> predTypeCompatibility;// p#t1#t2#t3#t4 (it will be symmetric)
	static int allPropEdges = 0;
	static double objChange = 0;

	public TypePropagateMN(String root) {
		PGraph.emb = false;
		PGraph.suffix = "_sim.txt";
		PGraph.formBinaryGraph = false;
		if (sizeBasedPropagation) {
			setPredToOcc(root);
		}
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

	static void setPredToOcc(String root) {
		predToOcc = new HashMap<>();

		File folder = new File(root);
		File[] files = folder.listFiles();
		Arrays.sort(files);

		for (File f : files) {

			String fname = f.getName();

			if (fname.contains("_sim") || fname.contains("_tProp") || fname.contains("_emb")) {
				continue;
			}

			System.out.println("occ f name: " + fname);

			try {
				readOccFile(predToOcc, root + fname);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	static void readOccFile(Map<String, Integer> predToOcc, String fname) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(fname));
		String line = null;
		String currentPred = null;
		int currOcc = 0;
		while ((line = br.readLine()) != null) {
			if (line.equals("")) {
				continue;
			} else if (line.startsWith("predicate:")) {
				if (currentPred != null) {
					// System.out.println("pred: "+currentPred+" "+currOcc);
					predToOcc.put(currentPred, currOcc);
				}
				currentPred = line.substring(11);
				currOcc = 0;
			} else if (line.startsWith("inv idx")) {
				predToOcc.put(currentPred, currOcc);
				break;
			} else {
				int colIdx = line.lastIndexOf(":");
				int occ = (int) Float.parseFloat(line.substring(colIdx + 2));
				currOcc += occ;
			}
		}
		br.close();
	}

	static void memStat() {
		int mb = 1024 * 1024;
		long usedMb = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / mb;
		System.out.println("usedMb: " + usedMb);
	}

	Set<String> deletableFiles;

	// If we have something in test, but we don't even have a graph for it, just
	// create fake empty graph (and then delete it!)
	void createEmptySimFiles(String root) {
		deletableFiles = new HashSet<>();
		for (String types : PGraph.types2TargetRels.keySet()) {
			String types2 = types.split("#")[1] + "#" + types.split("#")[0];
			// System.out.println("address: "+root+types+"_sim.txt");
			File f = new File(root + types + "_sim.txt");
			File f2 = new File(root + types2 + "_sim.txt");

			if (!f.exists() && !f2.exists()) {
				System.out.println("f not exists for: " + types + " " + f.getName());
				try {
					new PrintStream(new File(f.getPath()));
					deletableFiles.add(types);
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			}

			// if(f.exists() && f.length()==0) {
			// f.delete();
			// }
			// if (f2.exists() && f2.length()==0) {
			// f2.delete();
			// }
		}
	}

	void readPGraphs(String root) {
		pGraphs = new ArrayList<>();
		graphToNumEdges = new HashMap<String, Integer>();
		if (TypePropagateMN.addTargetRels) {
			createEmptySimFiles(root);
		}

		File folder = new File(root);
		File[] files = folder.listFiles();

		Arrays.sort(files);

		rawPred2PGraphs = new HashMap<>();

		// boolean seenLoc = false;//TODO: be carful
		int gc = 0;
		for (File f : files) {

			String fname = f.getName();
			// if (!fname.contains("chemistry#thing")) {
			// continue;
			// }

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

			// if (gc == 50) {
			// break;
			// }

			System.out.println("fname: " + fname);
			PGraph pgraph = new PGraph(root + fname);
			if (pgraph.nodes.size() == 0) {
				continue;
			}

			pgraph.g0 = pgraph.formWeightedGraph(pgraph.sortedEdges, pgraph.nodes.size());

			if (TypePropagateMN.addTargetRels && deletableFiles.contains(pgraph.types)) {
				f.delete();
				// System.out.println("shall we delete?: "+f.getAbsolutePath());
			}
			pgraph.clean();

			if (pgraph.nodes.size() == 0) {
				continue;
			}

			pGraphs.add(pgraph);
			String[] ss = pgraph.types.split("#");
			String types2 = ss[1] + "#" + ss[0];
			graphToNumEdges.put(pgraph.types, pgraph.sortedEdges.size());
			graphToNumEdges.put(types2, pgraph.sortedEdges.size());

			System.out.println("allEdgesRem, allEdges: " + PGraph.allEdgesRemained + " " + PGraph.allEdges);
			gc++;
		}

		Collections.sort(pGraphs, Collections.reverseOrder());

		for (int i = 0; i < pGraphs.size(); i++) {
			PGraph pgraph = pGraphs.get(i);
			pgraph.sortIdx = i;

			for (String s : pgraph.pred2node.keySet()) {
				String rawPred = s.split("#")[0];
				if (!rawPred2PGraphs.containsKey(rawPred)) {
					rawPred2PGraphs.put(rawPred, new HashSet<>());
				}
				rawPred2PGraphs.get(rawPred).add(i);
			}

			System.out.println("pgraph name: " + pGraphs.get(i).name + " " + pGraphs.get(i).nodes.size());
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
			return 1.0 / smoothParam;
		}
	}

	// In pgraph: Given pred_r => pred_rp (types: t1, t2 + aligned), how likely is:
	// In pgraph_neigh: pred_p => pred_q (types: tp1, tp2 + aligned)
	static double getCompatibleScorePredBased_p(PGraph pgraph, PGraph pgraph_neigh, String rawPred_r, String rawPred_rp,
			String pred_r, String pred_rp, String pred_p, String pred_q, String t1_plain, String t2_plain,
			boolean aligned, String tp1_plain, String tp2_plain) {

		if (t1_plain.equals(tp1_plain) && t2_plain.equals(tp2_plain)) {
			return 1;
		} else if (!pgraph_neigh.pred2node.containsKey(pred_q) || !pgraph_neigh.pred2node.containsKey(pred_p)) {
			return 0;
		}

		// outgoing
		String key1 = rawPred_r + "#" + t1_plain + "#" + t2_plain + "#" + tp1_plain + "#" + tp2_plain + "#";
		String key1p = rawPred_r + "#" + tp1_plain + "#" + tp2_plain + "#" + t1_plain + "#" + t2_plain + "#";// because
																												// it's
																												// symmetric
																												// for
																												// g1,//
																												// g2 or
																												// g2,
																												// g1!

		// incoming
		String key2 = aligned ? ("#" + rawPred_rp + "#" + t1_plain + "#" + t2_plain + "#" + tp1_plain + "#" + tp2_plain)
				: ("#" + rawPred_rp + "#" + t2_plain + "#" + t1_plain + "#" + tp2_plain + "#" + tp1_plain);
		String key2p = aligned
				? ("#" + rawPred_rp + "#" + tp1_plain + "#" + tp2_plain + "#" + t1_plain + "#" + t2_plain)
				: ("#" + rawPred_rp + "#" + tp2_plain + "#" + tp1_plain + "#" + t2_plain + "#" + t1_plain);

		double score1, score2;
		if (predTypeCompatibility.containsKey(key1)) {
			score1 = predTypeCompatibility.get(key1);
			// System.out.println("hash key1: " + key1 + " " + score1);
		} else {

			// what are the outgoing edges of pred_r in pgraph?
			int r = pgraph.pred2node.get(pred_r).idx;
			Set<DefaultWeightedEdge> outE_r = pgraph.g0.outgoingEdgesOf(r);

			// What are the outgoing edges of pred_p in pgraph_neigh.
			int p = pgraph_neigh.pred2node.get(pred_p).idx;
			Set<DefaultWeightedEdge> outE_p = pgraph_neigh.g0.outgoingEdgesOf(p);

			// Now, intersection of these edges!
			Set<String> intersection = new HashSet<>();
			Set<String> out_r = new HashSet<>();
			Set<String> out_p = new HashSet<>();

			String[] ss_r = pred_r.split("#");
			String[] ss_p = pred_p.split("#");

			for (DefaultWeightedEdge e : outE_r) {
				int idx = pgraph.g0.getEdgeTarget(e);
				String id = pgraph.nodes.get(idx).id;
				String[] ss = id.split("#");

				// is r aligned with this guy? (one of its many outgoing edges)
				boolean a = ss_r[1].equals(ss[1]);
				out_r.add(ss[0] + "#" + a);
			}

			for (DefaultWeightedEdge e : outE_p) {

				int idx = pgraph_neigh.g0.getEdgeTarget(e);
				String id = pgraph_neigh.nodes.get(idx).id;
				String[] ss = id.split("#");

				// is p aligned with this guy? (one of its many outgoing edges)
				boolean a = ss_p[1].equals(ss[1]);

				out_p.add(ss[0] + "#" + a);
			}

			intersection = CollectionUtils.intersection(out_r, out_p);
			score1 = (((double) intersection.size() + 1)
					/ (-intersection.size() + out_r.size() + out_p.size() + smoothParam));

			predTypeCompatibility.put(key1, score1);
			predTypeCompatibility.put(key1p, score1);
			System.out.println("key1: " + key1 + " " + score1);
			// System.out.println("p key1: " + key1p + " " + score1);
		}

		if (predTypeCompatibility.containsKey(key2)) {
			score2 = predTypeCompatibility.get(key2);
			// System.out.println("hash key2: " + key2 + " " + score2);
		} else {

			// what are the incoming edges of pred_rp in pgraph?
			int rp = pgraph.pred2node.get(pred_rp).idx;
			Set<DefaultWeightedEdge> inE_rp = pgraph.g0.incomingEdgesOf(rp);

			// What are the incoming edges of pred_q in pgraph_neigh. pgraph_neigh might not
			// have it!
			int q = pgraph_neigh.pred2node.get(pred_q).idx;
			Set<DefaultWeightedEdge> inE_q = pgraph_neigh.g0.incomingEdgesOf(q);

			// Now, intersection of these edges!
			Set<String> intersection = new HashSet<>();
			Set<String> in_rp = new HashSet<>();
			Set<String> in_q = new HashSet<>();

			String[] ss_rp = pred_rp.split("#");
			String[] ss_q = pred_q.split("#");

			for (DefaultWeightedEdge e : inE_rp) {
				int idx = pgraph.g0.getEdgeSource(e);
				String id = pgraph.nodes.get(idx).id;
				String[] ss = id.split("#");

				// is r aligned with this guy? (one of its many outgoing edges)
				boolean a = ss_rp[1].equals(ss[1]);
				in_rp.add(ss[0] + "#" + a);
			}

			for (DefaultWeightedEdge e : inE_q) {
				int idx = pgraph_neigh.g0.getEdgeSource(e);
				String id = pgraph_neigh.nodes.get(idx).id;
				String[] ss = id.split("#");

				// is r aligned with this guy? (one of its many outgoing edges)
				boolean a = ss_q[1].equals(ss[1]);
				in_q.add(ss[0] + "#" + a);
			}

			intersection = CollectionUtils.intersection(in_rp, in_q);
			score2 = ((double) intersection.size() + 1)
					/ (-intersection.size() + in_rp.size() + in_q.size() + smoothParam);

			// System.out.println("key2: " + key2 + " " + score2);
			// System.out.println("p key2: " + key2 + " " + score2);

			predTypeCompatibility.put(key2, score2);
			predTypeCompatibility.put(key2p, score2);
		}

		// System.out.println(
		// "comp score: " + pred_r + "=>" + pred_rp + " " + pred_p + "=>" + pred_q + " "
		// + (score1 * score2));

		// return score1 * score2;
		return score1;

	}

	// In pgraph: Given pred_r => pred_rp (types: t1, t2 + aligned), how likely is:
	// In pgraph_neigh: pred_p => pred_q (types: tp1, tp2 + aligned)
	static double getCompatibleScorePredBased(PGraph pgraph, PGraph pgraph_neigh, String rawPred_r, String rawPred_rp,
			String pred_r, String pred_rp, String pred_p, String pred_q, String t1_plain, String t2_plain,
			boolean aligned, String tp1_plain, String tp2_plain) {

		if (t1_plain.equals(tp1_plain) && t2_plain.equals(tp2_plain)) {
			// The second condition below is not quite consistent with out MN, because we
			// don't have a \beta for RHS. But, we put it
			// as it's necessary not to rely on the zero similarity which isn't based on the
			// data. It will converge anyway!
			if (PGraph.targetRelsAddedToGraphs.contains(pred_r) || PGraph.targetRelsAddedToGraphs.contains(pred_rp)) {
				return 1e-6;// small epsilon.
			}

			// TODO: changed, be careful
			if (pgraph.pred2node.containsKey(pred_r)) {
				return Math.sqrt(pgraph.pred2node.get(pred_r).getNumNeighs());
				// return pgraph.pred2node.get(pred_r).getNumNeighs();
			 }
			return 1;
		} else if (PGraph.targetRelsAddedToGraphs.contains(pred_r)
				|| PGraph.targetRelsAddedToGraphs.contains(pred_rp)) {
			return 0;// Don't propagate noise! Not consistent with MN again.
		} else if (PGraph.targetRelsAddedToGraphs.contains(pred_p) || PGraph.targetRelsAddedToGraphs.contains(pred_q)) {
			return 1.0;// The neighbor graph hasn't seen any evidence, it's happy to receive main
						// graph's edges! Not consistent with MN again.
		} else if (/* !pgraph_neigh.pred2node.containsKey(pred_q) || */ !pgraph_neigh.pred2node.containsKey(pred_p)) {
			return 0;
		} else if (TypePropagateMN.lmbda2 == 0) {
			return 0;
		}

		// outgoing
		String key1 = rawPred_r + "#" + t1_plain + "#" + t2_plain + "#" + tp1_plain + "#" + tp2_plain + "#";
		String key1p = rawPred_r + "#" + tp1_plain + "#" + tp2_plain + "#" + t1_plain + "#" + t2_plain + "#";// because
																												// it's
																												// symmetric
																												// for
																												// g1,//
																												// g2 or
																												// g2,
																												// g1!

		double score1;
		if (predTypeCompatibility.containsKey(key1)) {
			score1 = predTypeCompatibility.get(key1);
			// System.out.println("hash key1: " + key1 + " " + score1);
		} else {

			// what are the outgoing edges of pred_r in pgraph?
			int r = pgraph.pred2node.get(pred_r).idx;
			Set<DefaultWeightedEdge> outE_r = pgraph.g0.outgoingEdgesOf(r);

			// What are the outgoing edges of pred_p in pgraph_neigh.
			int p = pgraph_neigh.pred2node.get(pred_p).idx;
			Set<DefaultWeightedEdge> outE_p = pgraph_neigh.g0.outgoingEdgesOf(p);

			String[] ss_r = pred_r.split("#");
			String[] ss_p = pred_p.split("#");

			double sum = 0;

			for (DefaultWeightedEdge e : outE_r) {
				double w1 = pgraph.g0.getEdgeWeight(e);
				int idx = pgraph.g0.getEdgeTarget(e);
				String id = pgraph.nodes.get(idx).id;
				String[] ss = id.split("#");

				// is r aligned with this guy? (one of its many outgoing edges)
				boolean a = ss_r[1].equals(ss[1]);

				String cand;
				if (a) {
					cand = ss[0] + "#" + ss_p[1] + "#" + ss_p[2];
				} else {
					cand = ss[0] + "#" + ss_p[2] + "#" + ss_p[1];
				}

				// if it doesn't have the cand, it wouldn't appear in the sum
				if (pgraph_neigh.pred2node.containsKey(cand)) {
					double w2 = 0;
					int qIdx2 = pgraph_neigh.pred2node.get(cand).idx;
					if (pgraph_neigh.g0.containsEdge(p, qIdx2)) {
						DefaultWeightedEdge ee = pgraph_neigh.g0.getEdge(p, qIdx2);
						w2 = pgraph_neigh.g0.getEdgeWeight(ee);
					}
					sum += Math.pow(w1 - w2, 2);
					// System.out.println("adding: "+pred_r+" "+pred_p+" "+id+" "+cand+" "+w1+"
					// "+w2);
				}

			}

			for (DefaultWeightedEdge e : outE_p) {

				double w1 = pgraph.g0.getEdgeWeight(e);
				int idx = pgraph_neigh.g0.getEdgeTarget(e);
				String id = pgraph_neigh.nodes.get(idx).id;
				String[] ss = id.split("#");

				// is p aligned with this guy? (one of its many outgoing edges)
				boolean a = ss_p[1].equals(ss[1]);

				String cand;
				if (a) {
					cand = ss[0] + "#" + ss_r[1] + "#" + ss_r[2];
				} else {
					cand = ss[0] + "#" + ss_r[2] + "#" + ss_r[1];
				}

				// if it doesn't have the cand, it wouldn't appear in the sum
				if (pgraph.pred2node.containsKey(cand)) {
					// double w2 = 0;
					int qIdx2 = pgraph.pred2node.get(cand).idx;
					if (!pgraph.g0.containsEdge(r, qIdx2)) {
						// DefaultWeightedEdge ee = pgraph.g0.getEdge(r,qIdx2);
						// w2 = pgraph_neigh.g0.getEdgeWeight(ee);
						// System.out.println("adding2: "+pred_r+" "+pred_p+" "+id+" "+cand+" "+w1);
						sum += Math.pow(w1, 2);
					}
				}

			}

			if (TypePropagateMN.lmbda2 == 0) {
				score1 = 0;
			} else {
				score1 = (TypePropagateMN.lmbda2 - sum) / (TypePropagateMN.lmbda2);
				score1 = Math.max(score1, 0);
			}

			synchronized (predTypeCompatibility) {
				predTypeCompatibility.put(key1, score1);
				predTypeCompatibility.put(key1p, score1);
			}

			System.out.println("key1: " + key1 + " " + score1);
			System.out.println("p key1: " + key1p + " " + score1);
		}
		return score1;
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
			// System.out.println("compatibles: " + ss[0] + " " + prob);
			compatibles.put(ss[0], prob);
		}
		sc.close();
	}

	// for now, just one iteration
	void MNPropagateSims() {

		for (int iter = 0; iter < numIters; iter++) {
			objChange = 0;
			predTypeCompatibility = Collections.synchronizedMap(new HashMap<>());
			System.err.println("iter " + iter);
			for (PGraph pgraph : pGraphs) {
				// initialize gMN (next g) based on g0 (cur g)
				int N = pgraph.g0.vertexSet().size();
				pgraph.gMN = new DefaultDirectedWeightedGraph<>(DefaultWeightedEdge.class);
				for (int i = 0; i < N; i++) {
					pgraph.gMN.addVertex(i);
					DefaultWeightedEdge ee = pgraph.gMN.addEdge(i, i);
					pgraph.gMN.setEdgeWeight(ee, 1);
				}
				pgraph.edgeToMNWeight = new ConcurrentHashMap<>();

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

			// propagate between graphs
			try {
				propagateAll(0);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}

			// propagate within graphs
			try {
				propagateAll(1);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}

			// Now, just let's get the average for gMNs

			try {
				propagateAll(2);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}

			// now let's put g0 = gMN
			if (iter != numIters - 1) {
				for (PGraph pgraph : pGraphs) {
					pgraph.g0 = pgraph.gMN;
				}
			}
			System.out.println("obj change: " + objChange);
		}

		// now, let's write the results
		try {
			propagateAll(3);
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
		System.out.println(tPropSuffix);

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
