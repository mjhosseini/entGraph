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
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;

import constants.ConstantsGraphs;
import constants.ConstantsMNEmbIter;
import constants.ConstantsSoftConst;
import entailment.Util;
import graph.EmbIter.MNEmbIter;

//entry point for running transitive graph making methods + iterative embedding (which never worked well)
public class PGraph implements Comparable<PGraph> {

	// fields
	static Map<FeatName, String> featNameToStr;
	public int sortIdx = -1;// the index of the graph after sorting all the graphs based on their sizes. 0
							// is the largest.

	static {
		featNameToStr = new HashMap<>();
		featNameToStr.put(FeatName.Cos, "cos");
		featNameToStr.put(FeatName.WeedProb, "weed's probabilistic");
		featNameToStr.put(FeatName.WeedPMI, "weed's pmi sim");
		featNameToStr.put(FeatName.Lin, "lin");
		featNameToStr.put(FeatName.BINC, "binc");
		featNameToStr.put(FeatName.WeedPMIPr, "weed's pmi precision sim");
		featNameToStr.put(FeatName.Iter, "iter 1");
		featNameToStr.put(FeatName.rw, "rand walk 0 sims");
		
	}

	public static int allEdges = 0;
	public static int allEdgesRemained = 0;

	public String name;
	public String types;
	public String fname;
	public List<Node> nodes;
	public Map<Integer, Node> idx2node;
	public Map<String, Node> pred2node;

	public static Set<String> targetRelsAddedToGraphs = new HashSet<>();
	public static Map<String, Set<String>> types2TargetRels = new HashMap<>();
	public ArrayList<Edge> sortedEdges;

	// These three fields should be actually in softGraphs, but for simplicity, I'm
	// not changing the code
	public DefaultDirectedWeightedGraph<Integer, DefaultWeightedEdge> g0;
	public DefaultDirectedWeightedGraph<Integer, DefaultWeightedEdge> gMN;
	public Map<String, Double> edgeToMNWeight;
	public static List<PGraph> pGraphs;
	public static Map<String, Set<Integer>> rawPred2PGraphs;
	public static Map<String, Integer> predToOcc;// ex: (visit.1,visit.2)#person#location => 10344

	// static List<SimpleScore> scores = new ArrayList<>();

	public PGraph() {

	}

	public PGraph(String fname) {
		this.fname = fname;
		nodes = new ArrayList<>();
		idx2node = new HashMap<>();
		pred2node = new HashMap<>();

		try {
			buildGraphFromFile(fname);
			System.out.println("the name: " + this.name);
			if (this.name == null) {
				return;
			}
			// for (int i = 0; i < nodes.size(); i++) {
			// System.out.println(i + ": " + nodes.get(i).id);
			// }
		} catch (IOException e) {
			System.err.println("exception before sorted edges");
			e.printStackTrace();
			return;
		}
		// System.out.println("here sorted edges1: "+sortedEdges);
//		this.setSortedEdges();//This was moved to the end of pgraphs building...
		// Now, build the graph using embeddings

	}

	public List<Edge> getSortedEdges() {
		return sortedEdges;
	}

	public void setSortedEdges() {
		ArrayList<Edge> ret = new ArrayList<>();
		for (Node n : this.nodes) {
			int i = n.idx;
			for (Oedge oedge : n.oedges) {
				int j = oedge.nIdx;
				float sim = oedge.sim;
				if (sim > 0) {
					Edge edge = new Edge(i, j, sim, this);
					if (ConstantsGraphs.sortEdgesConfidenceBased) {
						edge.setConfidence();
					}
					ret.add(edge);
					// System.out.println("edge: "+i+" "+j+" "+sim);
				}
			}
		}
		Collections.sort(ret, Collections.reverseOrder());
		this.sortedEdges = ret;
	}

	/*
	 * 
	 * 2) for p#q##t1#t2#a, in a graph for t1 and t2, we were checking both p#t1#t2
	 * and q#t1#t2 (if aligned), and p#t2#t1 and q#t2#t1. We're not doing this
	 * anymore and just checking the correct one.
	 * 
	 */
	// prevIds will change (new ids might get added)

	public DefaultDirectedWeightedGraph<Integer, DefaultWeightedEdge> formWeightedGraph(List<Edge> sortedEdges, int N) {
		DefaultDirectedWeightedGraph<Integer, DefaultWeightedEdge> g0 = new DefaultDirectedWeightedGraph<>(
				DefaultWeightedEdge.class);
		// System.out.println("sorted edges: "+sortedEdges);
		for (int i = 0; i < N; i++) {
			g0.addVertex(i);
			if (ConstantsSoftConst.forceSelfEdgeOne) {
				DefaultWeightedEdge ee = g0.addEdge(i, i);
				g0.setEdgeWeight(ee, 1);
			}
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

	public float getW(int i, int j) {
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

			// targetRels = readTargetRels(tfpath);
			if (ConstantsGraphs.addTargetRels) {
				setTargetRelsMap();
			}
		} catch (NumberFormatException | IOException e) {
			e.printStackTrace();
		}
	}

	void buildGraphFromFile(String fname) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(fname));
		String line = "";
		int lIdx = 0;
		boolean first = true;
		String simName = "";
		Node node = null;

		boolean shouldRemove = false;
		int rank = 1;
		while ((line = br.readLine()) != null) {
			line = line.replace("` ", "").trim();
			if (lIdx % 1000000 == 0) {
				System.out.println("lidx: " + lIdx);
			}
			lIdx++;
			if (first) {
				// this.name = line;
				
				line = line.replace("types: ", "").replace(",", " ");
				line = line.substring(0, line.indexOf(' '));
				
				this.types = line;
				this.name = this.types;
//				System.out.println("types in pgraph: "+this.types);
				
				first = false;
			} else if (line.equals("")) {
				continue;
			} else if (line.startsWith("predicate:")) {
				String pred = line.substring(11);
				rank = 1;

				if (shouldRemovePred(pred)) {
					shouldRemove = true;
					continue;
				} else {
					shouldRemove = false;
				}

				if (!pred2node.containsKey(pred)) {
					int nIdx = nodes.size();
					node = new Node(nIdx, pred);
					this.insertNode(node);
				} else {
					node = pred2node.get(pred);
				}
			} else {
				if (shouldRemove) {
					continue;
				}
				if (line.startsWith("num neighbors:")) {
					continue;
				}
				if (line.endsWith("sims") || line.endsWith("sim")) {
					simName = line.toLowerCase();
				} else {

					if (!simName.contains(featNameToStr.get(ConstantsGraphs.featName)) || simName.contains("unary")
							|| simName.contains("sep")) {
						continue;
					}
					// if (!simName.contains("iter 1")) {
					// continue;
					// }
					String nPred = "";
					float sim = 0;

					try {
						String[] ss = line.split(" ");
						nPred = ss[0];
						if (shouldRemovePred(nPred)) {
							continue;
						}
						// System.out.println("npred: "+nPred);
						sim = Float.parseFloat(ss[1]);
					} catch (Exception e) {
						continue;
					}
					allEdges++;
					if (sim < ConstantsGraphs.edgeThreshold) {
						// System.out.println("lt: " + sim);
						continue;
					}
					
//					if (rank>1000) {
//						continue;//TODO: remove this!
//					}
					
					if (ConstantsGraphs.rankDiscount) {
						sim *= (1/Math.sqrt(rank));
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
					if (!ConstantsGraphs.removeStopPreds
							|| (!Util.stopPreds.contains(node.id) && !Util.stopPreds.contains(nNode.id))) {
						node.addNeighbor(nIdx, sim);
						rank++;
					}
				}
			}
		}

		// Now, add the target rels without any neighbors
		if (ConstantsGraphs.addTargetRels) {
			Set<String> targetPreds = null;
			String[] ss = types.split("#");
			String types2 = ss[1] + "#" + ss[0];
			System.out.println("ss: " + ss[0] + " " + ss[1]);
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
						targetRelsAddedToGraphs.add(pred);
						this.insertNode(node);
					}
					// System.out.println("sss: "+ss[0]+" "+ss[1]+" "+(ss[0].equals(ss[1])));
					if (ss[0].equals(ss[1])) {
						String[] pss = pred.split("#");
						String pred2 = pss[0] + "#" + pss[2] + "#" + pss[1];
						// System.out.println("pred2: "+pred2);
						if (!pred2node.containsKey(pred2)) {
							int nIdx = nodes.size();
							System.out.println("adding new node: " + pred2);
							targetRelsAddedToGraphs.add(pred2);
							node = new Node(nIdx, pred2);
							this.insertNode(node);
						}
					}
				}
			}
		}

		br.close();

	}

	public void insertNode(Node n) {
		// System.out.println("adding node: "+n.id+" "+n.idx);
		nodes.add(n);
		idx2node.put(n.idx, n);
		pred2node.put(n.id, n);
	}

	static boolean shouldRemovePred(String pred) {
		if (ConstantsGraphs.removeNegs && pred.startsWith("NEG__")) {
			return true;
		}
		if (ConstantsGraphs.removeEventEventModifers && pred.contains("__") && !pred.startsWith("NEG__")) {
			return true;
		}
		return isConjunction(pred);
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

	// static Set<String> readTargetRels(String fpath) throws IOException {
	// Set<String> ret = new HashSet<>();
	// BufferedReader br = new BufferedReader(new FileReader(fpath));
	// String line = null;
	// while ((line = br.readLine()) != null) {
	// ret.add(line);
	// }
	// return ret;
	// }

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
		BufferedReader br = new BufferedReader(new FileReader(ConstantsMNEmbIter.allExamplesPath));
		types2TargetRels = new HashMap<>();
		MNEmbIter.targetRels = new HashSet<>();
		targetRelsAddedToGraphs = new HashSet<>();
		String line = null;

		while ((line = br.readLine()) != null) {
			String[] ss = line.split("\t");
			if (!ss[0].equals("")) {
				String[] rel1t = getTypedRel(ss[0]);
				String types1 = rel1t[1];
				String types1p = types1.split("#")[1] + types1.split("#")[0];
				String rel1 = rel1t[0];
				MNEmbIter.targetRels.add(rel1);

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
				MNEmbIter.targetRels.add(rel2);

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

	// carried here form python
	public static boolean sameCCGArgs(String p, String q) {
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
		// return new Integer(this.nodes.size()).compareTo(new Integer(o.nodes.size()));
		if (this.nodes.size() > o.nodes.size()) {
			return 1;
		} else if (this.nodes.size() < o.nodes.size()) {
			return -1;
		}
		return 0;
	}

	public static void setRawPred2PGraphs(List<PGraph> pGraphs) {
		rawPred2PGraphs = new HashMap<>();
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

	public static void readOccFile(Map<String, Integer> predToOcc, String fname) throws IOException {
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

	public static void setPredToOcc(String root) {
		PGraph.predToOcc = new HashMap<>();

		File folder = new File(root);
		File[] files = folder.listFiles();
		Arrays.sort(files);

		for (File f : files) {

			String fname = f.getName();

			// if (fname.contains("_sim") || fname.contains("_tProp") ||
			// fname.contains("_emb")) {
			// continue;
			// }

			if (!fname.contains("_rels.txt")) {
				continue;
			}

			System.out.println("occ f name: " + fname);

			try {
				PGraph.readOccFile(PGraph.predToOcc, root + fname);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public enum FeatName {
		Cos, WeedProb, WeedPMI, Lin, BINC, WeedPMIPr, Iter, rw
	}

	public enum TransitiveMethod {
		HTLFRG, BerantTNF, SpectralILP, SpectralILPWithin;
	}

}
