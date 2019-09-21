package graph.softConst;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;

import constants.ConstantsSoftConst;
import edu.stanford.nlp.util.CollectionUtils;
import graph.PGraph;

public class LabelPropagationBetweenGraphs implements Runnable {

	int numThreads;
	int threadIdx;
	PGraph pgraph;
	List<PGraph> allpGraphs;

	public LabelPropagationBetweenGraphs(int threadIdx, int numThreads, PGraph pgraph, List<PGraph> allpGraphs) {
		this.threadIdx = threadIdx;
		this.numThreads = numThreads;
		this.pgraph = pgraph;
		this.allpGraphs = allpGraphs;
	}

	@Override
	public void run() {
		Map<String, Set<Integer>> rawPred2PGraphs = PGraph.rawPred2PGraphs;
		DefaultDirectedWeightedGraph<Integer, DefaultWeightedEdge> gPrev = pgraph.g0;

		for (int r = 0; r < gPrev.vertexSet().size(); r++) {

			// if (r % numThreads != threadIdx) {
			// continue;
			// }

			if (r % 100 == 0) {
				System.out.println("r: " + r);
				TypePropagateMN.memStat();
			}

			// graphs having r's pred
			String pred_r = pgraph.idx2node.get(r).id;
			String[] ss_r = pred_r.split("#");
			String t1_r_plain = ss_r[1].replace("_1", "").replace("_2", "");
			String t2_r_plain = ss_r[2].replace("_1", "").replace("_2", "");

			String rawPred_r = ss_r[0];// the raw predicate: e.g., (visit.1,visit.2)
			Set<Integer> rawPred_r_PGraphs = rawPred2PGraphs.get(rawPred_r); // pgraphs with this predicate

			// Let's compute sum of coef*sim
			for (DefaultWeightedEdge e : gPrev.outgoingEdgesOf(r)) {
				int rp = gPrev.getEdgeTarget(e);

				// Let's get all the neighbors
				// graphs having rp's pred
				String pred_rp = pgraph.idx2node.get(rp).id;
				String[] ss_rp = pred_rp.split("#");
				String rawPred_rp = ss_rp[0];// the raw predicate: e.g., // (visit.1,visit.2)
				boolean aligned = ss_r[1].equals(ss_rp[1]);
				Set<Integer> rawPred_rp_PGraphs = rawPred2PGraphs.get(rawPred_rp); // pgraphs with this predicate

				Set<Integer> neighborGraphs = CollectionUtils.intersection(rawPred_r_PGraphs, rawPred_rp_PGraphs);
				if (ConstantsSoftConst.lmbda2 == 0) {
					neighborGraphs = new HashSet<>();
					neighborGraphs.add(pgraph.sortIdx);
				}

				double sim = gPrev.getEdgeWeight(e);
				int minPairOcc1 = 1;

				if (ConstantsSoftConst.sizeBasedPropagation) {
					minPairOcc1 = Math.min(PGraph.predToOcc.get(pred_r), PGraph.predToOcc.get(pred_rp));
				}

				// Let's propagate to all the neighbor graphs
				// neighGraphs have the rawPred, but we don't know about the exact type
				// ordering.
				// So, we try both ways!

				for (int ngIdx : neighborGraphs) {

					if (ngIdx % numThreads != threadIdx) {
						continue;
					}

					PGraph pgraph_neigh = allpGraphs.get(ngIdx);
					String tp1 = pgraph_neigh.types.split("#")[0];// don't get confused with rp, etc. tp1 is for the
																	// neigh graph
					String tp2 = pgraph_neigh.types.split("#")[1];
					String tp1_plain = tp1;
					String tp2_plain = tp2;

					if (tp1.equals(tp2)) {
						tp1 += "_1";
						tp2 += "_2";
					}

					// case 1: tp1 and tp2
					String pred_p = rawPred_r + "#" + tp1 + "#" + tp2;
					String pred_q;

					if (aligned) {
						pred_q = rawPred_rp + "#" + tp1 + "#" + tp2;
					} else {
						pred_q = rawPred_rp + "#" + tp2 + "#" + tp1;
					}

					double compScore1 = 1, compScore2 = 1;

					if (!ConstantsSoftConst.predBasedPropagation) {
						//You can get rid of this totally! Not used anymore
						// make sure you give to both cases: tp1#tp2 and tp2#tp1
//						compScore1 = TypePropagateMN.getCompatibleScore(tp1_plain, tp2_plain, aligned, t1_r_plain,
//								t2_r_plain);
						// compScore1 *= Math.min(pgraph.nodes.size(), pgraph_neigh.nodes.size());//
						// TODO: added, be
						// careful

					} else {
						// System.out.println("from label prop1");
						// make sure you give to both cases: tp1#tp2 and tp2#tp1
						// how much pred_r => pred_rp is compatible with pred_p => pred_q
						// pred_r(p) are in pgraph. pred_p(q) are in pgraph_neigh
						// types are t1_r, t2_r and tp1, tp2
						compScore1 = TypePropagateMN.getCompatibleScorePredBased(pgraph, pgraph_neigh, rawPred_r,
								pred_r, pred_rp, pred_p, pred_q, t1_r_plain, t2_r_plain, tp1_plain, tp2_plain);
						// compScore1 *= Math.min(pgraph.nodes.size(), pgraph_neigh.nodes.size());//
						// TODO: added, be
						// careful

					}

					// System.out.println("propagating from: "+pred_r+" "+pred_rp);
					// propagate similarity to pred_p => pred_q. Also, compute the coef sum of all
					// the neighbors for those (pre-compute all at once)
					propagateOneEdge(pgraph, pgraph_neigh, pred_p, pred_q, sim, compScore1, rawPred_r, rawPred_rp,
							tp1_plain, tp2_plain, aligned, neighborGraphs, minPairOcc1);

					// case 2: tp2 and tp1
					pred_p = rawPred_r + "#" + tp2 + "#" + tp1;

					if (aligned) {
						pred_q = rawPred_rp + "#" + tp2 + "#" + tp1;
					} else {
						pred_q = rawPred_rp + "#" + tp1 + "#" + tp2;
					}

					if (!ConstantsSoftConst.predBasedPropagation) {
						//You can get rid of this totally! Not used anymore
//						compScore2 = TypePropagateMN.getCompatibleScore(tp2_plain, tp1_plain, aligned, t1_r_plain,
//								t2_r_plain);
						// compScore2 *= Math.min(pgraph.nodes.size(), pgraph_neigh.nodes.size());//
						// TODO: added, be
						// careful
					} else {
						// System.out.println("from label prop2");
						compScore2 = TypePropagateMN.getCompatibleScorePredBased(pgraph, pgraph_neigh, rawPred_r,
								pred_r, pred_rp, pred_p, pred_q, t1_r_plain, t2_r_plain, tp2_plain, tp1_plain);
						// compScore2 *= Math.min(pgraph.nodes.size(), pgraph_neigh.nodes.size());//
						// TODO: added, be
						// careful
					}

					propagateOneEdge(pgraph, pgraph_neigh, pred_p, pred_q, sim, compScore2, rawPred_r, rawPred_rp,
							tp2_plain, tp1_plain, aligned, neighborGraphs, minPairOcc1);
				}
			}
		}

	}

	// propagate for one edge (r => rp) from pgraph to pgraph_neigh (pred_p=>pred_q)
	void propagateOneEdge(PGraph pgraph, PGraph pgraph_neigh, String pred_p, String pred_q, double sim,
			double compScore, String rawPred_p, String rawPred_q, String tp1, String tp2, boolean aligned,
			Set<Integer> neighborGraphs, int minPairOcc1) {
		if (pgraph_neigh.pred2node.containsKey(pred_p) && pgraph_neigh.pred2node.containsKey(pred_q)) {
			// System.out.println("propagating from graph: " + pgraph.types + " to graph " +
			// pgraph_neigh.types + " for "
			// + pred_p + " " + pred_q + " " + compScore + " " + sim);
			int minPairOcc2 = 1;
			if (ConstantsSoftConst.sizeBasedPropagation) {
				minPairOcc2 = Math.min(PGraph.predToOcc.get(pred_p), PGraph.predToOcc.get(pred_q));
				compScore *= Math.min(minPairOcc1, minPairOcc2);
			}

			DefaultDirectedWeightedGraph<Integer, DefaultWeightedEdge> gMN = pgraph_neigh.gMN;
			int p = pgraph_neigh.pred2node.get(pred_p).idx;
			int q = pgraph_neigh.pred2node.get(pred_q).idx;

			double w;
			DefaultWeightedEdge ee;

			synchronized (gMN) {
				if (!gMN.containsEdge(p, q)) {
					ee = gMN.addEdge(p, q);
					gMN.setEdgeWeight(ee, 0);
					w = 0;
					TypePropagateMN.allPropEdges++;
				} else {
					ee = gMN.getEdge(p, q);
					w = gMN.getEdgeWeight(ee);
				}

				gMN.setEdgeWeight(ee, w + sim * compScore);

			}

			String edgeStr = p + "#" + q;

			boolean shouldAdd = false;
			synchronized (pgraph_neigh.edgeToMNWeight) {
				if (!pgraph_neigh.edgeToMNWeight.containsKey(edgeStr)) {
					shouldAdd = true;
					pgraph_neigh.edgeToMNWeight.put(edgeStr, -1.0);
				}
			}

			if (shouldAdd) {
				// Don't synchronize when we wanna compute sumCoefs!!!
				double sumCoefs = getSumNeighboringCoefs(pgraph_neigh, rawPred_p, rawPred_q, pred_p, pred_q, tp1, tp2,
						aligned, neighborGraphs, minPairOcc2);
				synchronized (pgraph_neigh.edgeToMNWeight) {
					// if (Double.isNaN(sumCoefs)) {
					// System.err.println("sum coefs nan: "+edgeStr);
					// }

					// System.out.println(
					// "sumCoefs: " + sumCoefs + " " + neighborGraphs.size() + " " + pred_p + " " +
					// pred_q);

					pgraph_neigh.edgeToMNWeight.put(edgeStr, sumCoefs);
				}
			}
		}
	}

	// rawPred_p#rawPred_q#aligned#tp1#tp2 will receive message from neighGraphs
	double getSumNeighboringCoefs(PGraph pgraph, String rawPred_p, String rawPred_q, String pred_p, String pred_q,
			String tp1, String tp2, boolean aligned, Set<Integer> neighborGraphs, int minPairOcc2) {

		double sumCoefs = 0;
		for (int gIdx : neighborGraphs) {
			PGraph pgraph_neigh = allpGraphs.get(gIdx);
			String ss[] = pgraph_neigh.types.split("#");
			String t1 = ss[0];
			String t2 = ss[1];
			String t1_plain = t1;
			String t2_plain = t2;

			if (t1.equals(t2)) {
				t1 += "_1";
				t2 += "_2";
			}

			String p1 = rawPred_p + "#" + t1 + "#" + t2;
			String p2 = rawPred_p + "#" + t2 + "#" + t1;
			String q1, q2;

			if (aligned) {
				q1 = rawPred_q + "#" + t1 + "#" + t2;
				q2 = rawPred_q + "#" + t2 + "#" + t1;
			} else {
				q1 = rawPred_q + "#" + t2 + "#" + t1;
				q2 = rawPred_q + "#" + t1 + "#" + t2;
			}

			double compScore1 = 1;
			double compScore2 = 1;

			// make sure you get from both cases: t1#t2 and t2#t1
			if (!ConstantsSoftConst.predBasedPropagation) {
				//You can get rid of this totally! Not used anymore
//				compScore1 = TypePropagateMN.getCompatibleScore(tp1, tp2, aligned, t1_plain, t2_plain);
//				// compScore1 *= Math.min(pgraph.nodes.size(), pgraph_neigh.nodes.size());//
//				// TODO: added, be careful
//
//				compScore2 = TypePropagateMN.getCompatibleScore(tp1, tp2, aligned, t2_plain, t1_plain);
				// compScore2 *= Math.min(pgraph.nodes.size(), pgraph_neigh.nodes.size());//
				// TODO: added, be careful
			} else {

				// It should be originally propagating from pred_r=>pred_rp to pred_rp=>pred_q
				// but here, we do from pred_p=>pred_q to p1, q1. Doesn't matter!
				compScore1 = TypePropagateMN.getCompatibleScorePredBased(pgraph, pgraph_neigh, rawPred_p, pred_p,
						pred_q, p1, q1, tp1, tp2, t1_plain, t2_plain);
				compScore2 = TypePropagateMN.getCompatibleScorePredBased(pgraph, pgraph_neigh, rawPred_p, pred_p,
						pred_q, p2, q2, tp1, tp2, t2_plain, t1_plain);

			}

			if (pgraph_neigh.pred2node.containsKey(p1) && pgraph_neigh.pred2node.containsKey(q1)) {
				// System.out.println("propagating: "+rawPred_p+" "+rawPred_q+" "+tp1+" "+tp2+"
				// "+aligned+" "+t1+" "+t2+" "+compScore1);

				if (ConstantsSoftConst.sizeBasedPropagation) {
					int minPairOcc1 = Math.min(PGraph.predToOcc.get(p1), PGraph.predToOcc.get(q1));
					compScore1 *= Math.min(minPairOcc1, minPairOcc2);
				}
				sumCoefs += compScore1;
			}
			if (pgraph_neigh.pred2node.containsKey(p2) && pgraph_neigh.pred2node.containsKey(q2)) {
				// System.out.println("propagating: "+rawPred_p+" "+rawPred_q+" "+tp1+" "+tp2+"
				// "+aligned+" "+t2+" "+t1+" "+compScore2);
				if (ConstantsSoftConst.sizeBasedPropagation) {
					int minPairOcc1 = Math.min(PGraph.predToOcc.get(p2), PGraph.predToOcc.get(q2));
					compScore2 *= Math.min(minPairOcc1, minPairOcc2);
				}

				sumCoefs += compScore2;
			}
		}
		return sumCoefs;
	}

}
