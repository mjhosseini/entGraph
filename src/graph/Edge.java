package graph;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;

import constants.ConstantsGraphs;
import constants.ConstantsTrans;
import edu.stanford.nlp.util.CollectionUtils;

public class Edge implements Comparable<Edge> {
	public int i;
	public int j;
	public float sim;
	int hashCode = -1;
	PGraph pgraph;
	public double confidence = -1;

	public static Map<String, Double> edgeToConf = new HashMap<>();// gIdx#i#j => 7

	public static double getConfidenceCrossGraph(int i, int j, PGraph pgraph) {
		
		if (ConstantsTrans.writeDebug) {
			System.out.println("cross graph coef for: " + pgraph.nodes.get(i).id + " " + pgraph.nodes.get(j).id);
		}

		String[] ss_i = pgraph.nodes.get(i).id.split("#");
		String[] ss_j = pgraph.nodes.get(j).id.split("#");

		String rawPred_i = ss_i[0];
		String rawPred_j = ss_j[0];

		if (ConstantsTrans.writeDebug) {
			System.out.println(rawPred_i);
		}

		Set<Integer> rawPred_i_PGraphs = PGraph.rawPred2PGraphs.get(rawPred_i); // pgraphs with this predicate
		Set<Integer> rawPred_j_PGraphs = PGraph.rawPred2PGraphs.get(rawPred_j); // pgraphs with this predicate

		boolean aligned = ss_i[1].equals(ss_j[1]);
		if (ConstantsTrans.writeDebug) {
			System.out.println("aligned: " + aligned + " " + pgraph.nodes.get(i).id + " " + pgraph.nodes.get(j).id);
		}

		Set<Integer> neighborGraphs = CollectionUtils.intersection(rawPred_i_PGraphs, rawPred_j_PGraphs);
		double conf = 0;
		double denom = 0;
		for (int ngIdx : neighborGraphs) {
			// System.out.println("neigh graph: "+ngIdx);
			PGraph neighPGraph = PGraph.pGraphs.get(ngIdx);
			String[] ss = neighPGraph.types.split("#");
			String type1 = ss[0];
			String type2 = ss[1];
			if (type1.equals(type2)) {
				type1 += "_1";
				type2 += "_2";
			}

			String p = rawPred_i + "#" + type1 + "#" + type2;
			String q;
			if (aligned) {
				q = rawPred_j + "#" + type1 + "#" + type2;
			} else {
				q = rawPred_j + "#" + type2 + "#" + type1;
			}

			// System.out.println("cand: "+p+" "+q);

			if (neighPGraph.pred2node.containsKey(p) && neighPGraph.pred2node.containsKey(q)) {
				
				denom += 1;// TODO: use beta
				double this_conf = Edge.getConfidenceThisGraph(neighPGraph.pred2node.get(p).idx,
						neighPGraph.pred2node.get(q).idx, neighPGraph);
				conf += this_conf;
				if (ConstantsTrans.writeDebug) {
					System.out.println(p + " " + q + " for " + pgraph.nodes.get(i).id + " " + pgraph.nodes.get(j).id
							+ " " + this_conf);
				}
			}

			if (!type1.equals(type2)) {
				p = rawPred_i + "#" + type2 + "#" + type1;
				if (aligned) {
					q = rawPred_j + "#" + type2 + "#" + type1;
				} else {
					q = rawPred_j + "#" + type1 + "#" + type2;
				}

				if (neighPGraph.pred2node.containsKey(p) && neighPGraph.pred2node.containsKey(q)) {
					denom += 1;// TODO: use beta
					double this_conf = Edge.getConfidenceThisGraph(neighPGraph.pred2node.get(p).idx,
							neighPGraph.pred2node.get(q).idx, neighPGraph);
					conf += this_conf;
					if (ConstantsTrans.writeDebug) {

						System.out.println(p + " " + q + " for " + pgraph.nodes.get(i).id + " " + pgraph.nodes.get(j).id
								+ " " + this_conf);
					}
				}

			}
		}

		
		if (conf == 0) {
			if (ConstantsTrans.writeDebug) {
				System.out.println("conf is 1");
			}
			return 1;
		} else {
			if (ConstantsTrans.writeDebug) {
				System.out.println("conf, denom: " + conf + " " + denom + " " + (conf / denom));
			}
			return conf / denom;
		}

	}

	// public Edge(int i, int j, float sim) {
	// this.i = i;
	// this.j = j;
	// this.sim = sim;
	// }

	public Edge(int i, int j, float sim, PGraph pgraph) {
		this.i = i;
		this.j = j;
		this.sim = sim;
		this.pgraph = pgraph;
	}

	void setConfidence() {
		this.confidence = getConfidence(this.i, this.j, this.pgraph);
	}

	public static double getConfidence(int i, int j, PGraph pgraph) {
		String edgeStr = pgraph.sortIdx + "#" + i + "#" + j;
		if (edgeToConf.containsKey(edgeStr)) {
			return edgeToConf.get(edgeStr);
		}
		double ret;
		if (!ConstantsTrans.confAvg) {
			ret = getConfidenceThisGraph(i, j, pgraph);
		} else {
			ret = getConfidenceCrossGraph(i, j, pgraph);
		}
		edgeToConf.put(edgeStr, ret);
		return ret;
	}

	public static double getConfidenceThisGraph(int i, int j, PGraph pgraph) {
		double confidence = 1;
		try {
			int occ1 = PGraph.predToOcc.get(pgraph.nodes.get(i).id);
			int occ2 = PGraph.predToOcc.get(pgraph.nodes.get(j).id);
			confidence = Math.max((Math.min(occ1, occ2)), 1);
		} catch (Exception ex) {

		}
		return confidence;
	}

	public int compareTo(Edge e) {
		if (!ConstantsGraphs.sortEdgesConfidenceBased || confidence == -1) {
			if (this.sim > e.sim) {
				return 1;
			} else if (this.sim < e.sim) {
				return -1;
			}
		} else {
			if (this.sim * this.confidence > e.sim * e.confidence) {
				return 1;
			} else if (this.sim * this.confidence < e.sim * e.confidence) {
				return -1;
			}
		}
		return 0;
	}

	public boolean equals(Object o) {
		Edge e = (Edge) o;
		return e.i == i && e.j == j;
	}

	public int hashCode() {
		if (hashCode != -1) {
			return hashCode;
		}
		int h = new ImmutablePair<Integer, Integer>(i, j).hashCode();
		this.hashCode = h;
		return h;
	}

	public static void main(String[] args) {
		// Edge e1 = new Edge(5, 10, -1);
		// Edge e2 = new Edge(5, 10, 2);
		// // System.out.println(e1.hashCode());
		// // System.out.println(e2.hashCode());
		// // System.out.println(e1.equals(e2));
		//
		// HashMap<Edge, Float> s = new HashMap<>();
		// s.put(e1, 2f);
		// System.out.println(s.containsKey(e2));

	}

}
