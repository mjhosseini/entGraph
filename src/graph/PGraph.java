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
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;

public class PGraph implements Comparable<PGraph>{

	public final static boolean checkFrgVio = true;
	public final static boolean shouldWrite = true;
	public static boolean emb = false;
	public static boolean weightEdgeSimilarities = true;
	public static boolean formBinaryGraph = false;
	public static boolean transitive = true;
	public final static int maxNeighs = 1000;// more than 30!
	public static float relMinSim = -1f;// -1 if don't want to
	public final static boolean addTargetRels = true;

	public static String suffix = "_sim.txt";
	static final String embSuffix = "_embsims25.txt";
	static final String fpath = "../../python/gfiles/ent/ccg5.sim";
	// static final String tfpath = "../../python/gfiles/ent/target_rels_CCG.txt";//
	// TODO: update this
	static final String allExamplesPath = "../../python/gfiles/ent/all_new_comb_rels.txt";// TODO: update this
	static final String root = "../../python/gfiles/typedEntGrDir_aida_figer_3_3_f/";

	// static final String root =
	// "../../python/gfiles/typedEntGrDir_aida_LDA15_2_2/";
	static final int maxEmbIter = 1;
	int sortIdx = -1;//the index of the graph after sorting all the graphs based on their sizes. 0 is the largets.

	public static float edgeThreshold = 0.01f;// isn't worth it! .05 reduces
	// edges by half, but not worth it

	static int allEdges = 0;
	static int allEdgesRemained = 0;

	String name;
	String types;
	String fname;
	List<Node> nodes;
	Map<Integer, Node> idx2node;
	Map<String, Node> pred2node;
	public static Map<String, List<PredSim>> rels2Sims;
	public static Map<String, List<PredSim>> invRels2Sims;
	public static Set<String> targetRels;
	public static Map<String, Set<String>> types2TargetRels;
	ArrayList<Edge> sortedEdges;
	DefaultDirectedWeightedGraph<Integer, DefaultWeightedEdge> g0;
	DefaultDirectedWeightedGraph<Integer, DefaultWeightedEdge> gMN;
	Map<String, Double> edgeToMNWeight;

	// static List<SimpleScore> scores = new ArrayList<>();

	public PGraph(String fname) {
		this.fname = fname;
		nodes = new ArrayList<>();
		idx2node = new HashMap<>();
		pred2node = new HashMap<>();

		try {
			buildGraphFromFile(fname);
			System.out.println("the name: "+this.name);
			if (this.name == null) {
				return;
			}
			for (int i = 0; i < nodes.size(); i++) {
				System.out.println(i + ": " + nodes.get(i).id);
			}
		} catch (IOException e) {
			System.err.println("exception before sorted edges");
			e.printStackTrace();
			return;
		}
//		System.out.println("here sorted edges1: "+sortedEdges);
		this.setSortedEdges();
		// Now, build the graph using embeddings

		if (PGraph.emb) {
			int N = this.nodes.size();
			DefaultDirectedWeightedGraph<Integer, DefaultWeightedEdge> g0 = formWeightedGraph(sortedEdges, N);
			List<String> prevIds = new ArrayList<>();
			Map<String, Integer> prevPred2Node = new HashMap<>();

			for (int i = 0; i < N; i++) {
				prevIds.add(this.nodes.get(i).id);
				prevPred2Node.put(prevIds.get(i), i);
			}

			DefaultDirectedWeightedGraph<Integer, DefaultWeightedEdge> nextG = g0;

			List<DefaultDirectedWeightedGraph<Integer, DefaultWeightedEdge>> gs = new ArrayList<>();
			List<List<String>> allPredsList = new ArrayList<>();
			gs.add(g0);
			List<String> nextIds = new ArrayList<>();
			for (String s : prevIds) {
				nextIds.add(s);
			}
			allPredsList.add(nextIds);

			for (int k = 0; k < PGraph.maxEmbIter; k++) {
				System.out.println("iter: " + (k + 1));
				nextG = getNextEmbeddingGr(nextG, PGraph.invRels2Sims, prevIds, prevPred2Node, targetRels,
						k == PGraph.maxEmbIter - 1);
				gs.add(nextG);
				nextIds = new ArrayList<>();
				for (String s : prevIds) {
					nextIds.add(s);
				}
				allPredsList.add(nextIds);
				System.out.println("num preds: " + nextIds.size());
			}
			String fnameEmbeds = fname.substring(0, fname.lastIndexOf('_')) + PGraph.embSuffix;
			writeEmbeddingResults(gs, allPredsList, fnameEmbeds);
		}
	}

	void writeEmbeddingResults(List<DefaultDirectedWeightedGraph<Integer, DefaultWeightedEdge>> gs,
			List<List<String>> allPredsList, String fnameEmbeds) {
		// list of all predicates can be found from the last graph. The indexes
		// are also the same (if existing) with previous graphs
		PrintStream op = null;
		try {
			op = new PrintStream(new File(fnameEmbeds));
		} catch (Exception e) {
		}
		List<String> predList = allPredsList.get(allPredsList.size() - 1);
		op.println("types: " + this.name + ", " + " label propagation num preds: " + predList.size());
		DefaultDirectedWeightedGraph<Integer, DefaultWeightedEdge> lastG = gs.get(gs.size() - 1);
		for (int i = 0; i < predList.size(); i++) {
			op.println("predicate: " + predList.get(i));
			op.println("num max neighbors: " + lastG.outgoingEdgesOf(i).size());
			op.println();
			for (int iter = 0; iter < gs.size(); iter++) {
				DefaultDirectedWeightedGraph<Integer, DefaultWeightedEdge> thisG = gs.get(iter);
				op.println("iter " + iter + " sims");
				List<SimpleScore> scores = new ArrayList<>();
				if (thisG.containsVertex(i)) {
					for (DefaultWeightedEdge e : thisG.outgoingEdgesOf(i)) {
						int j = thisG.getEdgeTarget(e);
						double w = thisG.getEdgeWeight(e);
						scores.add(new SimpleScore("", predList.get(j), (float) w));
					}

					Collections.sort(scores, Collections.reverseOrder());
					for (SimpleScore sc : scores) {
						op.println(sc.pred2 + " " + sc.score);
					}
				}
				op.println();
			}
		}
		op.close();
	}

	List<Edge> getSortedEdges() {
		return sortedEdges;
	}

	void setSortedEdges() {
		ArrayList<Edge> ret = new ArrayList<>();
		for (Node n : this.nodes) {
			int i = n.idx;
			for (Oedge oedge : n.oedges) {
				int j = oedge.nIdx;
				float sim = oedge.sim;
				if (sim > 0) {
					ret.add(new Edge(i, j, sim));
					// System.out.println("edge: "+i+" "+j+" "+sim);
				}
			}
		}
		Collections.sort(ret, Collections.reverseOrder());
		this.sortedEdges = ret;
	}

	// typed similar ones that are in pgraph. We force the types to be the same
	// (and same order)
	// onlyGraph: get neighbors only from the graph (don't wanna add anymore!)
	List<PredSim> getTypedPredSimList(String rId, Map<String, List<PredSim>> invrels2Sims,
			Map<String, List<PredSim>> currentInvPredToSimList, Map<String, Integer> curPred2Node, boolean onlyGraph) {

		if (onlyGraph && currentInvPredToSimList.containsKey(rId)) {
			return currentInvPredToSimList.get(rId);
		}

		String[] ss = rId.split("#");
		String rawPred = ss[0];// without types
		String type1 = ss[1];
		String type2 = ss[2];

		List<PredSim> pss = invrels2Sims.get(rawPred);

		if (pss == null) {
			pss = new ArrayList<>();
			pss.add(new PredSim(rawPred, 1));
		}

		List<PredSim> ret = new ArrayList<>();

		// System.out.println("returning simlist of " + rId);

		for (PredSim ps : pss) {
			String cand = ps.pred + "#" + type1 + "#" + type2;
			if (onlyGraph && !curPred2Node.containsKey(cand)) {
				continue;
			}
			ret.add(new PredSim(cand, ps.sim));
			// System.out.println(ret.get(ret.size() - 1));

		}

		// if (ret.size() > 1) {
		// System.out.println("OK, found a similar one!");
		// }
		if (onlyGraph) {
			currentInvPredToSimList.put(rId, ret);
		}
		return ret;
	}

	/*
	 * 
	 * 2) for p#q##t1#t2#a, in a graph for t1 and t2, we were checking both p#t1#t2
	 * and q#t1#t2 (if aligned), and p#t2#t1 and q#t2#t1. We're not doing this
	 * anymore and just checking the correct one.
	 * 
	 */
	// prevIds will change (new ids might get added)

	DefaultDirectedWeightedGraph<Integer, DefaultWeightedEdge> getNextEmbeddingGr(
			DefaultDirectedWeightedGraph<Integer, DefaultWeightedEdge> g0, Map<String, List<PredSim>> invrels2Sims,
			List<String> prevIds, Map<String, Integer> prevPred2Node, Set<String> targetRels, boolean addNewRel) {

		int prevN = g0.vertexSet().size();
		DefaultDirectedWeightedGraph<Integer, DefaultWeightedEdge> g = new DefaultDirectedWeightedGraph<>(
				DefaultWeightedEdge.class);

		Map<String, Integer> curPred2Node = new HashMap<>();
		List<String> curIds = new ArrayList<>();

		for (int i = 0; i < prevN; i++) {
			g.addVertex(i);
			curIds.add(prevIds.get(i));
			curPred2Node.put(prevIds.get(i), i);
		}

		int curN = prevN;

		if (addNewRel) {
			// Now, add nodes from target relations!
			for (int r = 0; r < prevN; r++) {
				// System.out.println(prevIds.get(r));
				List<PredSim> pss = getTypedPredSimList(prevIds.get(r), invrels2Sims, null, null, false);

				for (PredSim ps : pss) {
					String rawPred = ps.pred.substring(0, ps.pred.indexOf("#"));
					// System.out.println("rawPred: " + rawPred);
					if (!curPred2Node.containsKey(ps.pred) && targetRels.contains(rawPred)) {
						g.addVertex(curN);
						curIds.add(ps.pred);
						curPred2Node.put(ps.pred, curN);
						// System.out.println("adding new node: " +
						// curIds.get(curN)
						// + " based on " + prevIds.get(r));
						curN++;
					}
				}
			}

			System.out.println("new nodes added: " + prevN + " " + curN);

		}

		Map<String, List<PredSim>> currentInvPredToSimList = new HashMap<>();

		// sum of similarities of all rs that have p around them...
		// double[] neighWeights = new double[curN];
		// Map<Edge, Double> edgeSumWeights = new HashMap<>();

		int numAllProp = 0;

		for (int r = 0; r < g0.vertexSet().size(); r++) {
			if (r % 100 == 0) {
				System.out.println("r: " + r + " " + numAllProp);
			}

			List<PredSim> pss = getTypedPredSimList(curIds.get(r), invrels2Sims, currentInvPredToSimList, curPred2Node,
					true);

			// for (PredSim ps : pss) {
			//
			// if (!curPred2Node.containsKey(ps.pred)) {
			// // System.out.println("can't find: " + ps.pred);
			// continue;
			// }
			// int p = curPred2Node.get(ps.pred);
			// neighWeights[p] += ps.sim;
			// }

			for (DefaultWeightedEdge e : g0.outgoingEdgesOf(r)) {
				int rp = g0.getEdgeTarget(e);
				// DefaultWeightedEdge e0 = g0.getEdge(r, rp);// It must have
				// the
				// // edge because it's
				// // a superset of
				// // gDits!
				double sim = g0.getEdgeWeight(e);

				List<PredSim> qss = getTypedPredSimList(curIds.get(rp), invrels2Sims, currentInvPredToSimList,
						curPred2Node, true);

				// System.out.println("curId: " + curIds.get(r));
				// System.out.println("neigh: " + curIds.get(rp));

				for (PredSim ps : pss) {
					// if (!curPred2Node.containsKey(ps.pred)) {
					// // System.out.println("no pred " + ps.pred);
					// continue;
					// }
					int p = curPred2Node.get(ps.pred);

					for (PredSim qs : qss) {
						// if (!curPred2Node.containsKey(qs.pred)) {
						// continue;
						// }

						int q = curPred2Node.get(qs.pred);

						// System.out.println("avg for 1: " + ps);
						// System.out.println("avg for 2: " + qs);

						double w;
						DefaultWeightedEdge ee;
						if (!g.containsEdge(p, q)) {
							ee = g.addEdge(p, q);
							numAllProp++;
							g.setEdgeWeight(ee, 0);
							w = 0;
						} else {
							ee = g.getEdge(p, q);
							w = g.getEdgeWeight(ee);
						}
						if (!PGraph.weightEdgeSimilarities) {
							g.setEdgeWeight(ee, w + sim * ps.sim * qs.sim);
						} else {
							List<Integer> l = new ArrayList<>();
							// r and rp are always in g0. p and q might be new!
							if (p < prevN) {
								l.add(g0.outDegreeOf(p));
							}
							l.add(g0.outDegreeOf(r));
							if (q < prevN) {
								l.add(g0.inDegreeOf(q));
							}

							l.add(g0.inDegreeOf(rp));

							double beta = Math.max(Collections.min(l), 1);

							g.setEdgeWeight(ee, w + sim * ps.sim * qs.sim * beta);
						}

						// if (sim != 0) {// similar to python. Avg only if
						// r->rp
						// exists!
						// ImmutablePair<Integer, Integer> ip = new
						// ImmutablePair<Integer, Integer>(p, q);
						// Edge mye = new Edge(p, q, -1);
						// if (!edgeSumWeights.containsKey(mye)) {
						// edgeSumWeights.put(mye, 0.0);
						// }
						// // System.out.println("sim1, sim2: " + ps.sim + " "
						// // + qs.sim);
						// edgeSumWeights.put(mye, edgeSumWeights.get(mye) +
						// (ps.sim * qs.sim));
						// }

					}
				}
			}
		}

		Map<String, List<PredSim>> currentPredToSimList = new HashMap<>();

		for (int p = 0; p < curN; p++) {
			if (p % 100 == 0) {
				System.out.println("p: " + p);
			}
			if (!g.containsEdge(p, p)) {
				g.addEdge(p, p);
			}

			List<PredSim> pss = getTypedPredSimList(curIds.get(p), PGraph.rels2Sims, currentPredToSimList, curPred2Node,
					true);

			DefaultWeightedEdge ee = g.getEdge(p, p);
			g.setEdgeWeight(ee, 1);

			for (DefaultWeightedEdge e : g.outgoingEdgesOf(p)) {
				int q = g.getEdgeTarget(e);
				if (p == q) {
					continue;
				}

				List<PredSim> qss = getTypedPredSimList(curIds.get(q), PGraph.rels2Sims, currentPredToSimList,
						curPred2Node, true);

				double wCor = 0;
				// For pairs that both are in the graph, we consider their feats
				// (even if zero)
				for (PredSim ps : pss) {
					int r = curPred2Node.get(ps.pred);
					if (curPred2Node.get(ps.pred) >= prevN) {
						// System.out.println("cont1: " + curIds.get(p) + " " +
						// ps.pred + " " + curPred2Node.get(ps.pred)
						// + " " + prevN);
						continue;
					}
					for (PredSim qs : qss) {

						if (curPred2Node.get(qs.pred) >= prevN) {
							// System.out.println("cont2: " + curIds.get(q) + ""
							// + qs.pred + " "
							// + curPred2Node.get(qs.pred) + " " + prevN);
							continue;
						}
						// else{
						// System.out.println("add w: " + ps +" " + qs);
						// }
						if (!PGraph.weightEdgeSimilarities) {
							wCor += ps.sim * qs.sim;
						} else {
							int rp = curPred2Node.get(qs.pred);

							List<Integer> l = new ArrayList<>();
							// r and rp are always in g0. p and q might be new!
							if (p < prevN) {
								l.add(g0.outDegreeOf(p));
							}
							l.add(g0.outDegreeOf(r));
							if (q < prevN) {
								l.add(g0.inDegreeOf(q));
							}

							l.add(g0.inDegreeOf(rp));

							double beta = Math.max(Collections.min(l), 1);
							wCor += ps.sim * qs.sim * beta;
						}
					}
				}

				// double w = g.getEdgeWeight(e) / (neighWeights[p] *
				// neighWeights[q]);

				// ImmutablePair<Integer, Integer> ip = new
				// ImmutablePair<Integer, Integer>(p, q);
				// Edge mye = new Edge(p, q, -1);
				double w;
				// if (edgeSumWeights.containsKey(mye)) {
				// w = g.getEdgeWeight(e) / edgeSumWeights.get(mye);

				w = g.getEdgeWeight(e) / wCor;

				// System.out.println("avg: " + w + " " + g.getEdgeWeight(e) + "
				// " + wCor);
				// System.out.println("avg: "+curIds.get(p) + " " +
				// curIds.get(q) + " ");

				if (w > 1.01) {
					System.out.println("bug: " + w + " " + g.getEdgeWeight(e) + " " + wCor);
					System.out.println(curIds.get(p) + " " + curIds.get(q) + " ");
				}
				if (wCor != 1) {
					g.setEdgeWeight(e, w);
				}

				// }
				// else {
				// // System.out.println("no wight: "+curIds.get(p)+"
				// // "+curIds.get(q));
				// w = g.getEdgeWeight(e);
				// }

				// double prevSim = 0;
				// if (g0.containsEdge(p, q)) {
				// ee = g0.getEdge(p, q);
				// prevSim = g0.getEdgeWeight(ee);
				// }
				// if (w != prevSim) {
				// // System.out.println("emb sim: " + curIds.get(p) + " " +
				// // curIds.get(q) + " " + w);
				// // System.out.println("prev sim: " + prevSim);
				// }
			}
		}

		//
		// List<PredSim> pss = rels2Sims.get(r);
		//
		// for (DefaultWeightedEdge e : g.outgoingEdgesOf(p)) {
		//
		// }
		// }

		// make prevIds and prevPred2Node equal current ones
		prevPred2Node.clear();
		prevIds.clear();
		for (String p : curIds) {
			prevIds.add(p);
			prevPred2Node.put(p, curPred2Node.get(p));
		}
		// System.out.println("here prevId size: "+prevIds.size());
		return g;
	}

	DefaultDirectedWeightedGraph<Integer, DefaultWeightedEdge> formWeightedGraph(List<Edge> sortedEdges, int N) {
		DefaultDirectedWeightedGraph<Integer, DefaultWeightedEdge> g0 = new DefaultDirectedWeightedGraph<>(
				DefaultWeightedEdge.class);
//		System.out.println("sorted edges: "+sortedEdges);
		for (int i = 0; i < N; i++) {
			g0.addVertex(i);
			DefaultWeightedEdge ee = g0.addEdge(i, i);
			g0.setEdgeWeight(ee, 1);
		}

		for (Edge e : sortedEdges) {
			DefaultWeightedEdge ee = g0.addEdge(e.i, e.j);
			if (ee == null) {
				continue;// In one case, because of replacing '`', we had an
							// edge twice in sortedEdges
			}
			g0.setEdgeWeight(ee, e.sim);
		}
		return g0;
	}

	public void clean() {
		for (Node n : nodes) {
			n.clean();
		}
		// sortedEdges = null;
	}

	float getW(int i, int j) {
		if (i == j) {
			return 1;
		} else {
			try {
				Node n = nodes.get(i);
				return n.idx2oedges.get(j).sim;
			} catch (Exception e) {
				return 0;
			}
		}
	}

	static {

		try {
			read_rels_sim(fpath, true, true);
			// targetRels = readTargetRels(tfpath);
			setTargetRelsMap();
		} catch (NumberFormatException | IOException e) {
			e.printStackTrace();
		}
	}

	void buildGraphFromFile(String fname) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(fname));
		String typeStr = fname.substring(fname.lastIndexOf('/') + 1, fname.lastIndexOf('_'));
		this.types = typeStr;
		this.name = this.types;
		String line = "";
		int lIdx = 0;
		boolean first = true;
		String simName = "";
		Node node = null;

		boolean isConjunction = false;
		while ((line = br.readLine()) != null) {
			line = line.replace("` ", "").trim();
			if (lIdx % 1000000 == 0) {
				System.out.println("lidx: " + lIdx);
			}
			lIdx++;
			if (first) {
//				this.name = line;
				first = false;
			} else if (line.equals("")) {
				continue;
			} else if (line.startsWith("predicate:")) {
				String pred = line.substring(11);

				if (isConjunction(pred)) {
					isConjunction = true;
					continue;
				} else {
					isConjunction = false;
				}

				// System.out.println("pred: "+pred);
				if (!pred2node.containsKey(pred)) {
					int nIdx = nodes.size();
					node = new Node(nIdx, pred);
					this.insertNode(node);
				} else {
					node = pred2node.get(pred);
				}
			} else {
				if (isConjunction) {
					continue;
				}
				if (line.startsWith("num neighbors:")) {
					continue;
				}
				if (line.endsWith("sims") || line.endsWith("sim")) {
					simName = line.toLowerCase();
				} else {// TODO: be careful
					if (!simName.contains("binc") || simName.contains("unary") || simName.contains("sep")) {
						continue;
					}
					String nPred = "";
					float sim = 0;

					try {
						String[] ss = line.split(" ");
						nPred = ss[0];
						if (isConjunction(nPred)) {
							continue;
						}
						// System.out.println("npred: "+nPred);
						sim = Float.parseFloat(ss[1]);
					} catch (Exception e) {
						continue;
					}
					allEdges++;
					if (sim < edgeThreshold) {
						// System.out.println("lt: " + sim);
						continue;
					}
					// else{
					// System.out.println("gt: "+sim);
					// }
					allEdgesRemained++;
					Node nNode;
					int nIdx;
					if (!pred2node.containsKey(nPred)) {
						nIdx = nodes.size();
						nNode = new Node(nIdx, nPred);
						this.insertNode(nNode);
					} else {

						nNode = pred2node.get(nPred);
						nIdx = nNode.idx;
					}
					// if (sim > .5) {
					// PGraph.scores.add(new SimpleScore(node.id, nNode.id, sim));
					// }
					node.addNeighbor(nIdx, sim);
				}
			}
		}

		// Now, add the target rels without any neighbors
		if (addTargetRels) {
			Set<String> targetPreds = null;
			String[] ss = types.split("#");
			String types2 = ss[1] + "#" + ss[0];
			System.out.println("ss: "+ss[0]+" "+ss[1]);
			if (types2TargetRels.containsKey(types)) {
				targetPreds = types2TargetRels.get(types);
			} else if (types2TargetRels.containsKey(types2)) {
				targetPreds = types2TargetRels.get(types2);
			}
			if (targetPreds != null) {
				for (String pred : targetPreds) {
					if (!pred2node.containsKey(pred)) {
						int nIdx = nodes.size();
						System.out.println("adding new node: " + pred);
						node = new Node(nIdx, pred);
						this.insertNode(node);
					}
//					System.out.println("sss: "+ss[0]+" "+ss[1]+" "+(ss[0].equals(ss[1])));
					if (ss[0].equals(ss[1])) {
						String[] pss = pred.split("#");
						String pred2 = pss[0] + "#" + pss[2] + "#" + pss[1];
//						System.out.println("pred2: "+pred2);
						if (!pred2node.containsKey(pred2)) {
							int nIdx = nodes.size();
							System.out.println("adding new node: " + pred2);
							node = new Node(nIdx, pred2);
							this.insertNode(node);
						}
					}
				}
			}
		}

		br.close();

	}

	void insertNode(Node n) {
		// System.out.println("adding node: "+n.id+" "+n.idx);
		nodes.add(n);
		idx2node.put(n.idx, n);
		pred2node.put(n.id, n);
	}

	public static void main(String[] args) {
		// String root = "../../python/gfiles/typedEntGrDir_aida/";
		// PGraph pgraph = new PGraph(root+"location#person_sim.txt");

		// TODO: be careful
		double maxLmbda = .2;
		double numLmbdas = 11;
		List<Float> lmbdas = new ArrayList<>();
		for (float lmbda = 0; lmbda <= maxLmbda; lmbda += maxLmbda / (numLmbdas - 1)) {
			lmbdas.add(lmbda);
		}
		lmbdas.add(.3f);
		lmbdas.add(.4f);
		lmbdas.add(.5f);

		// List<Float> lmbdas = new ArrayList<>();
		// lmbdas.add(.04f);
		// lmbdas.add(.08f);
		// lmbdas.add(.12f);

		File folder = new File(root);
		File[] files = folder.listFiles();
		Arrays.sort(files);

		// boolean seenLoc = false;// TODO: be careful
		for (File f : files) {
			String fname = f.getName();
			// if (fname.startsWith("location#location_sim.txt")) {
			// seenLoc = true;
			// }
			// if (seenLoc) {
			// break;
			// }

			if (!fname.contains(PGraph.suffix)) {
				continue;
			}

			System.out.println("fname: " + fname);
			PGraph pgraph = new PGraph(root + fname);
			if (pgraph.nodes.size() == 0) {
				continue;
			}

			System.out.println("allEdgesRem, allEdges: " + allEdgesRemained + " " + allEdges);

			if (!PGraph.formBinaryGraph) {
				continue;
			}

			int lastDotIdx = pgraph.fname.lastIndexOf('.');
			String postFix = "_graphs.txt";// TODO: be careful

			if (!checkFrgVio) {
				postFix = "_graphsNoFrg2.txt";
			}

			String outPath = pgraph.fname.substring(0, lastDotIdx - 4) + postFix;
			PrintStream op = null;
			try {
				op = new PrintStream(new File(outPath));
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			for (float lmbda : lmbdas) {
				System.out.println("lambda: " + lmbda);
				TNF tnf = new TNF(pgraph, op, lmbda, PGraph.checkFrgVio);
				if (PGraph.shouldWrite) {
					tnf.writeSCC();
				}
			}
			op.close();
		}

		System.out.println("allEdgesRem, allEdges: " + allEdgesRemained + allEdges);

		// Collections.sort(scores, Collections.reverseOrder());
		// System.out.println("higest scoring relations:");
		// for (int i = 0; i < Math.min(1000000, scores.size()); i++) {
		// System.out.println(scores.get(i));
		// }

	}

	// isConjunction or a bad thing!
	static boolean isConjunction(String pred) {
		try {
			String[] parts = pred.split("#");
			if (parts.length != 3) {
				return true;// Or, it's a bad thing!
			}
			pred = pred.split("#")[0];
			pred = pred.substring(1, pred.length() - 1);
			String[] ss = pred.split(",");
			return ss[0].equals(ss[1]);
		} catch (Exception e) {
			return false;
		}
	}

	static Set<String> readTargetRels(String fpath) throws IOException {
		Set<String> ret = new HashSet<>();
		BufferedReader br = new BufferedReader(new FileReader(fpath));
		String line = null;
		while ((line = br.readLine()) != null) {
			ret.add(line);
		}
		return ret;
	}

	// (cause.1,cause.2) pharyngitis::disease fever::disease =>
	// {(cause.1,cause.2)#disease_1#disease_2,disease#disease}
	private static String[] getTypedRel(String s) {
		String[] ss = s.split(" ");
		String t1 = ss[1].split("::")[1];
		String t2 = ss[2].split("::")[1];

		String types = t1 + "#" + t2;

		if (t1.equals(t2)) {
			t1 += "_1";
			t2 += "_2";
		}
		return new String[] { ss[0] + "#" + t1 + "#" + t2, types };

	}

	static void setTargetRelsMap() throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(allExamplesPath));
		types2TargetRels = new HashMap<>();
		targetRels = new HashSet<>();
		String line = null;

		while ((line = br.readLine()) != null) {
			String[] ss = line.split("\t");
			if (!ss[0].equals("")) {
				String[] rel1t = getTypedRel(ss[0]);
				String types1 = rel1t[1];
				String types1p = types1.split("#")[1] + types1.split("#")[0];
				String rel1 = rel1t[0];
				targetRels.add(rel1);

				if (types2TargetRels.containsKey(types1)) {
					types2TargetRels.get(types1).add(rel1);
				} else if (types2TargetRels.containsKey(types1p)) {
					types2TargetRels.get(types1p).add(rel1);
				} else {
					types2TargetRels.put(types1, new HashSet<>());
					types2TargetRels.get(types1).add(rel1);
				}
			}
			
			if (!ss[1].equals("")) {
				String[] rel2t = getTypedRel(ss[1]);

				

				String types2 = rel2t[1];
				String types2p = types2.split("#")[1] + types2.split("#")[0];
				String rel2 = rel2t[0];
				targetRels.add(rel2);

				if (types2TargetRels.containsKey(types2)) {
					types2TargetRels.get(types2).add(rel2);
				} else if (types2TargetRels.containsKey(types2p)) {
					types2TargetRels.get(types2p).add(rel2);
				} else {
					types2TargetRels.put(types2, new HashSet<>());
					types2TargetRels.get(types2).add(rel2);
				}
			}
			
			
		}
		br.close();
	}

	static void read_rels_sim(String fpath, boolean isCCG, boolean useSims) throws NumberFormatException, IOException {
		Map<String, List<PredSim>> rels2Sims = new HashMap<>();

		// The reason is that we're getting top 30 relations. So, if r is in p's
		// neigh, it might not be otherwise.
		Map<String, List<PredSim>> invRels2Sims = new HashMap<>();
		if (!useSims) {
			return;
		}

		String line = null;
		BufferedReader br = new BufferedReader(new FileReader(fpath));

		while ((line = br.readLine()) != null) {
			String[] ss = line.split("\t");
			for (int i = 0; i < ss.length; i++) {
				ss[i] = ss[i].trim();
			}
			String p = ss[0];

			String modifier = "";
			if (isCCG) {
				int ridx = p.lastIndexOf("__");
				if (ridx != -1) {
					modifier = p.substring(0, ridx);
					p = p.substring(ridx + 2);
				}
			}
			List<PredSim> qs = new ArrayList<>();
			Set<String> currentPreds = new HashSet<>();
			int idx = 1;
			while (idx < ss.length && qs.size() < PGraph.maxNeighs) {
				String q = ss[idx];
				idx += 1;
				float sim = Float.parseFloat(ss[idx]);

				if (sim < PGraph.relMinSim) {
					break;
				}
				idx += 1;

				if (isCCG) {
					int ridx = q.lastIndexOf("__");
					if (ridx != -1) {
						q = q.substring(ridx + 2);
					}

					try {
						if (!sameCCGArgs(p, q)) {
							continue;
						}
					} catch (Exception e) {
						System.out.println("exception for: " + q);

					}

					if (!modifier.equals("")) {
						q = modifier + "__" + q;
					}

				}

				if (!currentPreds.contains(q)) {
					qs.add(new PredSim(q, sim));
					currentPreds.add(q);
				}

			}

			if (isCCG && !modifier.equals("")) {
				p = modifier + "__" + p;
			}
			if (!currentPreds.contains(p)) {
				qs.add(0, new PredSim(p, 1));
			}
			rels2Sims.put(p, qs);

			for (PredSim qsim : qs) {
				if (!invRels2Sims.containsKey(qsim.pred)) {
					invRels2Sims.put(qsim.pred, new ArrayList<>());
				}
				invRels2Sims.get(qsim.pred).add(new PredSim(p, qsim.sim));
			}

//			System.out.println("p: " + p);
//			System.out.println("sims: ");
//			for (PredSim qq : qs) {
//				System.out.print(qq.pred + " ");
//			}
//			System.out.println();
		}

		// make sure invRels2Sims has itself!
		for (String q : invRels2Sims.keySet()) {
			List<PredSim> ql = invRels2Sims.get(q);
			Set<String> qPreds = new HashSet<>();
			for (PredSim ps : ql) {
				qPreds.add(ps.pred);
			}
			if (!qPreds.contains(q)) {
				ql.add(0, new PredSim(q, 1));
			}
		}

		PGraph.rels2Sims = rels2Sims;
		PGraph.invRels2Sims = invRels2Sims;
	}

	// carried here form python
	static boolean sameCCGArgs(String p, String q) {
		String[] ss_p = p.substring(1, p.length() - 1).split(",");
		String[] ss_q = q.substring(1, q.length() - 1).split(",");

		int last_dot_p = ss_p[0].lastIndexOf('.');
		String main_pred_p = ss_p[0].substring(0, last_dot_p);

		int last_dot_q = ss_q[0].lastIndexOf('.');
		String main_pred_q = ss_q[0].substring(0, last_dot_q);

		List<Integer> voices_p = new ArrayList<>();
		List<Integer> voices_q = new ArrayList<>();

		voices_p.add(Integer.parseInt(ss_p[0].substring(last_dot_p + 1)));
		voices_q.add(Integer.parseInt(ss_q[0].substring(last_dot_q + 1)));

		// Check cases like (wash.in.2,wash.on.2)
		if (voices_p.get(0) == 2 && StringUtils.countMatches(ss_p[0], ".") > 1) {
			voices_p.set(0, 4);
		}
		if (voices_q.get(0) == 2 && StringUtils.countMatches(ss_q[0], ".") > 1) {
			voices_q.set(0, 4);
		}

		last_dot_p = ss_p[1].lastIndexOf('.');
		String rpred_p = ss_p[1].substring(0, last_dot_p);

		last_dot_q = ss_q[1].lastIndexOf('.');
		String rpred_q = ss_q[1].substring(0, last_dot_q);

		if (rpred_p.equals(main_pred_p) && voices_p.get(0) != 4) {
			voices_p.add(Integer.parseInt(ss_p[1].substring(last_dot_p + 1)));
		} else {
			voices_p.add(4);
		}

		if (rpred_q.equals(main_pred_q) && voices_q.get(0) != 4) {
			voices_q.add(Integer.parseInt(ss_q[1].substring(last_dot_q + 1)));
		} else {
			voices_q.add(4);
		}
		for (int i = 0; i < voices_p.size(); i++) {
			if (voices_p.get(i) != voices_q.get(i)) {
				return false;
			}
		}
		PriorityQueue<Integer> queue = new PriorityQueue<>(Collections.reverseOrder());
		return true;

	}

	@Override
	public int compareTo(PGraph o) {
		return new Integer(this.nodes.size()).compareTo(o.nodes.size());
	}
}
