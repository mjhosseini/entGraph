package graph;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;

import edu.stanford.nlp.util.CollectionUtils;

public class LabelPropagateMN implements Runnable {

	List<PGraph> allpGraphs;
	List<PGraph> thispGraphs;
	int runIdx;// 0: propagate, 1: average
	int threadIdx;

	public LabelPropagateMN(List<PGraph> allpGraphs, int threadIdx, int numThreads, int runIdx) {
		if (runIdx == 0) {
			this.allpGraphs = allpGraphs;
		}
		thispGraphs = new ArrayList<>();
		for (int i = 0; i < allpGraphs.size(); i++) {
			if (i % numThreads == threadIdx) {
				thispGraphs.add(allpGraphs.get(i));
			}
		}
		this.runIdx = runIdx;
		this.threadIdx = threadIdx;
	}

	private Set<Integer> getUnionIntersection(DefaultDirectedWeightedGraph<Integer, DefaultWeightedEdge> g,
			Set<DefaultWeightedEdge> edges1, Set<DefaultWeightedEdge> edges2, boolean incoming1, boolean incoming2,
			boolean intersect) {
		Set<Integer> e1s = new HashSet<>();
		Set<Integer> e2s = new HashSet<>();
		for (DefaultWeightedEdge e : edges1) {
			if (incoming1) {
				e1s.add(g.getEdgeSource(e));
			} else {
				e1s.add(g.getEdgeTarget(e));
			}
		}

		for (DefaultWeightedEdge e : edges2) {
			if (incoming2) {
				e2s.add(g.getEdgeSource(e));
			} else {
				e2s.add(g.getEdgeTarget(e));
			}
		}

		if (intersect) {
			return CollectionUtils.intersection(e1s, e2s);
		} else {
			return CollectionUtils.unionAsSet(e1s, e2s);
		}

	}

	void addNumeratorDenom(PGraph pgraph, int i, int j, double numerator, double denom) {
		if (numerator == 0 && denom == 0) {
			// System.err.println("both numer and denom zero "+i+" "+j+" "+pgraph.name);
			return;
		}
		DefaultDirectedWeightedGraph<Integer, DefaultWeightedEdge> gMN = pgraph.gMN;
		double w;
		DefaultWeightedEdge ee;

		if (numerator != 0) {
			// synchronized (gMN) {

			if (!gMN.containsEdge(i, j)) {
				ee = gMN.addEdge(i, j);
				gMN.setEdgeWeight(ee, 0);
				w = 0;
				TypePropagateMN.allPropEdges++;
			} else {
				ee = gMN.getEdge(i, j);
				w = gMN.getEdgeWeight(ee);
			}

			gMN.setEdgeWeight(ee, w + numerator);

			// }
		}

		if (denom != 0) {
			String edgeStr = i + "#" + j;
			synchronized (pgraph.edgeToMNWeight) {
				if (!pgraph.edgeToMNWeight.containsKey(edgeStr)) {
					pgraph.edgeToMNWeight.put(edgeStr, denom);
				} else {
					double prevDenom = pgraph.edgeToMNWeight.get(edgeStr);
					pgraph.edgeToMNWeight.put(edgeStr, prevDenom + denom);
				}
			}
		}

	}
	
	static int numOperations = 0;
	static int numPassedEdges = 0;

	// label propagate inside a graph!
	void propagateLabelWithinGraphs() {
		for (PGraph pgraph : thispGraphs) {
			System.out.println("mn prop within graphs: " + pgraph.fname + " " + threadIdx);
			DefaultDirectedWeightedGraph<Integer, DefaultWeightedEdge> gPrev = pgraph.g0;

			// (3) in formulations: add to numerator of w_ij: -wji* \sum_k
			// [(w_ik-w_jk)^2+(w_ki-w_kj)^2]
			
			for (int j = 0; j < gPrev.vertexSet().size(); j++) {
				if (j % 100 == 0) {
					System.out.println("3 j: " + j + " " + pgraph.name);
				}
				
				for (DefaultWeightedEdge e : gPrev.outgoingEdgesOf(j)) {
					int i = gPrev.getEdgeTarget(e);
					double w_ji = gPrev.getEdgeWeight(e) - TypePropagateMN.tau;

					if (w_ji <= 0) {
						continue;
					}
					
					if (i == j) {
						continue;
					}
					
					numPassedEdges++;

					double numerator = 0;

					// Now, form i's in \\union j's in
					Set<DefaultWeightedEdge> e_incoming_i = gPrev.incomingEdgesOf(i);
					Set<DefaultWeightedEdge> e_incoming_j = gPrev.incomingEdgesOf(j);

					Set<Integer> ks = getUnionIntersection(gPrev, e_incoming_i, e_incoming_j, true, true, false);
					
					for (int k : ks) {
						if (k == i || k == j) {
							continue;
						}
						double w_ki = 0;
						double w_kj = 0;

						if (gPrev.containsEdge(k, i)) {
							w_ki = gPrev.getEdgeWeight(gPrev.getEdge(k, i));
						}

						if (gPrev.containsEdge(k, j)) {
							w_kj = gPrev.getEdgeWeight(gPrev.getEdge(k, j));
						}

						if (w_ki == 0 && w_kj == 0) {
							System.err.println(
									"both incoming edges zero, how??" + i + " " + j + " " + k + " " + pgraph.name);
							System.exit(0);
						}
						
						numerator -= w_ji * Math.pow(w_ki - w_kj, 2);
						
					}

					// Now, form i's out \\union j's out
					Set<DefaultWeightedEdge> e_outgoing_i = gPrev.outgoingEdgesOf(i);
					Set<DefaultWeightedEdge> e_outgoing_j = gPrev.outgoingEdgesOf(j);

					ks = getUnionIntersection(gPrev, e_outgoing_i, e_outgoing_j, false, false, false);
					for (int k : ks) {
						if (k == i || k == j) {
							continue;
						}
						double w_ik = 0;
						double w_jk = 0;

						if (gPrev.containsEdge(i, k)) {
							w_ik = gPrev.getEdgeWeight(gPrev.getEdge(i, k));
						}

						if (gPrev.containsEdge(j, k)) {
							w_jk = gPrev.getEdgeWeight(gPrev.getEdge(j, k));
						}

						if (w_ik == 0 && w_jk == 0) {
							System.err.println(
									"both outgoing edges zero, how??" + i + " " + j + " " + k + " " + pgraph.name);
							System.exit(0);
						}

						numerator -= w_ji * Math.pow(w_ik - w_jk, 2);
					}
					
					numOperations += e_incoming_i.size()+e_incoming_j.size()+e_outgoing_i.size()+e_outgoing_j.size();
					System.out.println("here num op: "+numOperations+" "+w_ji+" "+numPassedEdges);
					
					// now, use denominator and numerator for i and j
					if (numerator > 0) {
						System.err.println("numerator >0, how??");
						System.exit(0);
					}
					addNumeratorDenom(pgraph, i, j, numerator, 0);
				}
			}

			// (1) in formulations: add to numerator of w_ij: w_jk*w_kj*w_ik and
			// denominator: w_jk*w_kj
			// (2) in formulations: add to numerator of w_ij: w_ik*w_ki*w_kj and
			// denominator: w_ik*w_ki. For 2, i and j are really reverse in the below code

			double[] reflexSumWeights = new double[gPrev.vertexSet().size()];
//			Set<String> nzijs = new HashSet<>();
			for (int k = 0; k < gPrev.vertexSet().size(); k++) {
				if (k % 100 == 0) {
					System.out.println("1,2 k: " + k + " " + pgraph.name);
				}
				// Find k's out \\intersection k's in
				Set<DefaultWeightedEdge> e_outgoing_k = gPrev.outgoingEdgesOf(k);
				Set<DefaultWeightedEdge> e_incoming_k = gPrev.incomingEdgesOf(k);

				Set<Integer> k_neighs = getUnionIntersection(gPrev, e_outgoing_k, e_incoming_k, false, true, true);
				
				numOperations += e_incoming_k.size()*e_incoming_k.size() + e_outgoing_k.size()*e_outgoing_k.size();
						
				for (int j : k_neighs) {

					if (j == k) {
						continue;
					}

					double w_jk = gPrev.getEdgeWeight(gPrev.getEdge(j, k)) - TypePropagateMN.tau;
					double w_kj = gPrev.getEdgeWeight(gPrev.getEdge(k, j)) - TypePropagateMN.tau;

					if (w_jk <= 0 || w_kj <= 0) {
						continue;
					}

					double denom = 2 * w_jk * w_kj;
					reflexSumWeights[j] += denom;

					// (1) in formulation
					for (DefaultWeightedEdge e : e_incoming_k) {
						int i = gPrev.getEdgeSource(e);
						if (i == k || i == j) {
							continue;
						}

						double w_ik = gPrev.getEdgeWeight(e);

						double numerator = denom * w_ik;

//						nzijs.add(i + "#" + j);

						addNumeratorDenom(pgraph, i, j, numerator, 0);// save all denoms for later!
					}

					// (2) in formulation
					for (DefaultWeightedEdge e : e_outgoing_k) {
						int i = gPrev.getEdgeSource(e);
						if (i == k || i == j) {
							continue;
						}

						double w_ki = gPrev.getEdgeWeight(e);
//						nzijs.add(j + "#" + i);

						double numerator = denom * w_ki;

						addNumeratorDenom(pgraph, j, i, numerator, 0);// save all denoms for later!
					}

				}

			}

			// Now, handle the rest of the denoms for 1 and 2! We saved it until here,
			// because we didn't know which numerators are zero
			// until now
			for (int i = 0; i < pgraph.gMN.vertexSet().size(); i++) {
				if (i % 100 == 0) {
					System.out.println("1,2 k: " + i + " " + pgraph.name);
				}
				for (DefaultWeightedEdge e : pgraph.gMN.outgoingEdgesOf(i)) {
					int j = gPrev.getEdgeTarget(e);
					double denom = reflexSumWeights[i] + reflexSumWeights[j];
//					if (denom == 0 && nzijs.contains(i + "#" + j)) {
//						System.err.println("here denom 0" + i + " " + j);
//					}
					
					addNumeratorDenom(pgraph, i, j, 0, denom);
				}
			}

			System.out.println("done within graphs: " + pgraph.fname + " " + threadIdx);
			System.out.println("num operations: "+numOperations);

		}
	}

	// typePropagate
	void propagateLabelBetweenGraphs() {

		Map<String, Set<Integer>> rawPred2PGraphs = TypePropagateMN.rawPred2PGraphs;

		// r => rp is used to update p=>q. propagate similarities of pgraph to all its
		// neighbors
		for (PGraph pgraph : thispGraphs) {
			System.out.println("MN prop between graphs: " + pgraph.fname + " " + threadIdx);
			DefaultDirectedWeightedGraph<Integer, DefaultWeightedEdge> gPrev = pgraph.g0;

			for (int r = 0; r < gPrev.vertexSet().size(); r++) {

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

					double sim = gPrev.getEdgeWeight(e);
					int minPairOcc1 = 1;
					if (TypePropagateMN.sizeBasedPropagation) {
						minPairOcc1 = Math.min(TypePropagateMN.predToOcc.get(pred_r),
								TypePropagateMN.predToOcc.get(pred_rp));
					}

					// Let's propagate to all the neighbor graphs
					// neighGraphs have the rawPred, but we don't know about the exact type
					// ordering.
					// So, we try both ways!
					for (int ngIdx : neighborGraphs) {
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

						double compScore1, compScore2;

						if (!TypePropagateMN.predBasedPropagation) {
							// make sure you give to both cases: tp1#tp2 and tp2#tp1
							compScore1 = TypePropagateMN.getCompatibleScore(tp1_plain, tp2_plain, aligned, t1_r_plain,
									t2_r_plain);
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
									rawPred_rp, pred_r, pred_rp, pred_p, pred_q, t1_r_plain, t2_r_plain, aligned,
									tp1_plain, tp2_plain);
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

						if (!TypePropagateMN.predBasedPropagation) {
							compScore2 = TypePropagateMN.getCompatibleScore(tp2_plain, tp1_plain, aligned, t1_r_plain,
									t2_r_plain);
							// compScore2 *= Math.min(pgraph.nodes.size(), pgraph_neigh.nodes.size());//
							// TODO: added, be
							// careful
						} else {
							// System.out.println("from label prop2");
							compScore2 = TypePropagateMN.getCompatibleScorePredBased(pgraph, pgraph_neigh, rawPred_r,
									rawPred_rp, pred_r, pred_rp, pred_p, pred_q, t1_r_plain, t2_r_plain, aligned,
									tp2_plain, tp1_plain);
							// compScore2 *= Math.min(pgraph.nodes.size(), pgraph_neigh.nodes.size());//
							// TODO: added, be
							// careful
						}

						propagateOneEdge(pgraph, pgraph_neigh, pred_p, pred_q, sim, compScore2, rawPred_r, rawPred_rp,
								tp2_plain, tp1_plain, aligned, neighborGraphs, minPairOcc1);

					}
				}
			}
			// pgraph.g0 = null;//TODO: make this null? need it for distance
			System.out.println("all prop edges: " + TypePropagateMN.allPropEdges);
		}
	}

	// private static String removeUnderlines(String t) {
	// return t.replace("_1", "").replace("_2", "");
	// }

	// propagate for one edge (r => rp) from pgraph to pgraph_neigh (pred_p=>pred_q)
	void propagateOneEdge(PGraph pgraph, PGraph pgraph_neigh, String pred_p, String pred_q, double sim,
			double compScore, String rawPred_p, String rawPred_q, String tp1, String tp2, boolean aligned,
			Set<Integer> neighborGraphs, int minPairOcc1) {
		if (pgraph_neigh.pred2node.containsKey(pred_p) && pgraph_neigh.pred2node.containsKey(pred_q)) {
			// System.out.println("propagating from graph: " + pgraph.types + " to graph " +
			// pgraph_neigh.types + " for "
			// + pred_p + " " + pred_q+" "+compScore);
			int minPairOcc2 = 1;
			if (TypePropagateMN.sizeBasedPropagation) {
				minPairOcc2 = Math.min(TypePropagateMN.predToOcc.get(pred_p), TypePropagateMN.predToOcc.get(pred_q));
				compScore *= Math.min(minPairOcc1, minPairOcc2);
			}

			DefaultDirectedWeightedGraph<Integer, DefaultWeightedEdge> gMN = pgraph_neigh.gMN;
			int p = pgraph_neigh.pred2node.get(pred_p).idx;
			int q = pgraph_neigh.pred2node.get(pred_q).idx;

			double w;
			DefaultWeightedEdge ee;
			String edgeStr = p + "#" + q;
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

			synchronized (pgraph_neigh.edgeToMNWeight) {
				if (!pgraph_neigh.edgeToMNWeight.containsKey(edgeStr)) {

					double sumCoefs = getSumNeighboringCoefs(pgraph_neigh, rawPred_p, rawPred_q, pred_p, pred_q, tp1,
							tp2, aligned, neighborGraphs, minPairOcc2);
//					if (Double.isNaN(sumCoefs)) {
//						System.err.println("sum coefs nan: "+edgeStr);
//					}
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

			double compScore1;
			double compScore2;

			// make sure you get from both cases: t1#t2 and t2#t1
			if (!TypePropagateMN.predBasedPropagation) {
				compScore1 = TypePropagateMN.getCompatibleScore(tp1, tp2, aligned, t1_plain, t2_plain);
				// compScore1 *= Math.min(pgraph.nodes.size(), pgraph_neigh.nodes.size());//
				// TODO: added, be careful

				compScore2 = TypePropagateMN.getCompatibleScore(tp1, tp2, aligned, t2_plain, t1_plain);
				// compScore2 *= Math.min(pgraph.nodes.size(), pgraph_neigh.nodes.size());//
				// TODO: added, be careful
			} else {

				// It should be originally propagating from pred_r=>pred_rp to pred_rp=>pred_q
				// but here, we do from pred_p=>pred_q to p1, q1. Doesn't matter!
				compScore1 = TypePropagateMN.getCompatibleScorePredBased(pgraph, pgraph_neigh, rawPred_p, rawPred_q,
						pred_p, pred_q, p1, q1, tp1, tp2, aligned, t1_plain, t2_plain);
				compScore2 = TypePropagateMN.getCompatibleScorePredBased(pgraph, pgraph_neigh, rawPred_p, rawPred_q,
						pred_p, pred_q, p2, q2, tp1, tp2, aligned, t2_plain, t1_plain);

			}

			if (pgraph_neigh.pred2node.containsKey(p1) && pgraph_neigh.pred2node.containsKey(q1)) {
				// System.out.println("propagating: "+rawPred_p+" "+rawPred_q+" "+tp1+" "+tp2+"
				// "+aligned+" "+t1+" "+t2+" "+compScore1);

				if (TypePropagateMN.sizeBasedPropagation) {
					int minPairOcc1 = Math.min(TypePropagateMN.predToOcc.get(p1), TypePropagateMN.predToOcc.get(q1));
					compScore1 *= Math.min(minPairOcc1, minPairOcc2);
				}
				sumCoefs += compScore1;
			}
			if (pgraph_neigh.pred2node.containsKey(p2) && pgraph_neigh.pred2node.containsKey(q2)) {
				// System.out.println("propagating: "+rawPred_p+" "+rawPred_q+" "+tp1+" "+tp2+"
				// "+aligned+" "+t2+" "+t1+" "+compScore2);
				if (TypePropagateMN.sizeBasedPropagation) {
					int minPairOcc1 = Math.min(TypePropagateMN.predToOcc.get(p2), TypePropagateMN.predToOcc.get(q2));
					compScore2 *= Math.min(minPairOcc1, minPairOcc2);
				}

				sumCoefs += compScore2;
			}
		}
		return sumCoefs;
	}

	void writeTPropResults(PGraph pgraph, List<DefaultDirectedWeightedGraph<Integer, DefaultWeightedEdge>> gs,
			String fnameTProp) {
		// list of all predicates can be found from the last graph. The indexes
		// are also the same (if existing) with previous graphs
		PrintStream op = null;
		try {
			op = new PrintStream(new File(fnameTProp));
		} catch (Exception e) {
		}
		// List<String> predList = allPredsList.get(allPredsList.size() - 1);

		int N = pgraph.idx2node.size();
		op.println(pgraph.name + " " + " type propagation num preds: " + N);

		DefaultDirectedWeightedGraph<Integer, DefaultWeightedEdge> lastG = gs.get(gs.size() - 1);
		for (int i = 0; i < N; i++) {
			String pred = pgraph.idx2node.get(i).id;
			op.println("predicate: " + pred);
			op.println("num max neighbors: " + lastG.outgoingEdgesOf(i).size());
			op.println();
			for (int iter = 0; iter < gs.size(); iter++) {
				DefaultDirectedWeightedGraph<Integer, DefaultWeightedEdge> thisG = gs.get(iter);
				op.println("iter " + iter + " sims");
				List<SimpleScore> scores = new ArrayList<>();
				if (thisG.containsVertex(i)) {
					for (DefaultWeightedEdge e : thisG.outgoingEdgesOf(i)) {
						int j = thisG.getEdgeTarget(e);
						String pred2 = pgraph.idx2node.get(j).id;
						double w = thisG.getEdgeWeight(e);
						scores.add(new SimpleScore("", pred2, (float) w));
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

	void getAvg() {

		for (PGraph pgraph : thispGraphs) {
			// Get the average
			DefaultDirectedWeightedGraph<Integer, DefaultWeightedEdge> gMN = pgraph.gMN;
			int curN = gMN.vertexSet().size();
			List<DefaultWeightedEdge> removableEdges = new ArrayList<>();
			for (int p = 0; p < curN; p++) {
				for (DefaultWeightedEdge e : gMN.outgoingEdgesOf(p)) {
					int q = gMN.getEdgeTarget(e);
					if (p == q) {
						gMN.setEdgeWeight(e, 1);
					} else {
						double c = gMN.getEdgeWeight(e);

						// if (c<0) {
						// System.err.println("neg c: "+c);
						// }

						if (c <= 0 || (c <= TypePropagateMN.lmbda && c >= -TypePropagateMN.lmbda)) {
							removableEdges.add(e);
							continue;
						} else {
//							if (!pgraph.edgeToMNWeight.containsKey(p + "#" + q)) {
//								System.err.println("doesn't have: " + p + "#" + q + " " + c + " " + pgraph.name);
//							}
							double denom = pgraph.edgeToMNWeight.get(p + "#" + q);
							double w;
							if (c > 0) {
								w = (c - TypePropagateMN.lmbda) / denom;
							} else {
								w = (c + TypePropagateMN.lmbda) / denom;
							}


							// System.out.println(
							// "avg: " + pgraph.idx2node.get(p).id + " " + pgraph.idx2node.get(q).id + " ");
							// System.out.println("avg: " + w + " " + gMN.getEdgeWeight(e) + " " + denom);

							if (w > 1.01) {
								System.out.println("bug: " + w + " " + gMN.getEdgeWeight(e) + " " + denom);
								System.out.println(pgraph.nodes.get(p).id + " " + pgraph.nodes.get(q).id + " ");
							}
							gMN.setEdgeWeight(e, w);

							double w0 = 0;
							if (pgraph.g0.containsEdge(p, q)) {
								DefaultWeightedEdge e0 = pgraph.g0.getEdge(p, q);
								w0 = pgraph.g0.getEdgeWeight(e0);
							}

							TypePropagateMN.objChange += Math.pow(w - w0, 2);

						}

						//
					}
				}
			}

			gMN.removeAllEdges(removableEdges);// TODO: you can do better here, by changing the order of the stuff

			// now, g0 is null, gMN is the next one

		}

	}

	// write the output of all of thispGraphs
	void writeResults() {
		while (thispGraphs.size() > 0) {
			PGraph pgraph = thispGraphs.remove(0);

			List<DefaultDirectedWeightedGraph<Integer, DefaultWeightedEdge>> gs = new ArrayList<>();
			pgraph.g0 = pgraph.formWeightedGraph(pgraph.sortedEdges, pgraph.nodes.size());
			gs.add(pgraph.g0);
			gs.add(pgraph.gMN);

			String fnameTProp = pgraph.fname.substring(0, pgraph.fname.lastIndexOf('_')) + TypePropagateMN.tPropSuffix;
			writeTPropResults(pgraph, gs, fnameTProp);
		}
	}

	@Override
	public void run() {
		if (runIdx == 0) {
			propagateLabelBetweenGraphs();
			System.out.println("between prop done!");
			allpGraphs = null;
		} else if (runIdx == 1) {
			propagateLabelWithinGraphs();
			System.out.println("within prop done!");
			System.out.println("thread Idx +" + threadIdx + " done");
		} else if (runIdx == 2) {
			getAvg();
		} else if (runIdx==3){
			writeResults();
		}

	}

}
