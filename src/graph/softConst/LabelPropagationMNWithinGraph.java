package graph.softConst;

import java.util.HashSet;
import java.util.Set;

import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;

import constants.ConstantsSoftConst;
import edu.stanford.nlp.util.CollectionUtils;
import graph.PGraph;

//Propagate labels within a graph for the range start to end
public class LabelPropagationMNWithinGraph implements Runnable {
	int numThreads;
	int threadIdx;
	PGraph pgraph;
	double[] reflexSumWeights;

	public LabelPropagationMNWithinGraph(int threadIdx, int numThreads, PGraph pgraph, double[] reflexSumWeights) {
		this.threadIdx = threadIdx;
		this.numThreads = numThreads;
		this.pgraph = pgraph;
		this.reflexSumWeights = reflexSumWeights;
	}

	@Override
	public void run() {
		DefaultDirectedWeightedGraph<Integer, DefaultWeightedEdge> gPrev = pgraph.g0;

		// (3) in formulations: add to numerator of w_ij: -wji* \sum_k
		// [(w_ik-w_jk)^2+(w_ki-w_kj)^2]

		for (int j = 0; j < gPrev.vertexSet().size(); j++) {

			if (j % numThreads != threadIdx) {
				continue;
			}

			if ((j / numThreads) % 100 == 0) {
				System.out.println("3 j: " + j + " " + pgraph.name);
			}

			for (DefaultWeightedEdge e : gPrev.outgoingEdgesOf(j)) {
				int i = gPrev.getEdgeTarget(e);

				if (i == j) {
					continue;
				}
				double w_ij = 0;
				if (gPrev.containsEdge(i, j)) {
					w_ij = gPrev.getEdgeWeight(gPrev.getEdge(i, j)) - ConstantsSoftConst.epsilon;
				}

				double w_ji = gPrev.getEdgeWeight(e) - ConstantsSoftConst.epsilon;

				if (w_ji <= 0 || w_ij <= 0) {
					continue;
				} else {
					// System.out.println("wij greater than 0: "+w_ij+" "+w_ji+"
					// "+TypePropagateMN.tau);
				}

				if (!ConstantsSoftConst.obj1) {
					w_ij += ConstantsSoftConst.epsilon;
					w_ji += ConstantsSoftConst.epsilon;
				}

				// if (!TypePropagateMN.obj1) {
				// w_ji +=
				// }

				LabelPropagateMN.numPassedEdges++;

				double numerator = 0;

				// Now, form i's in \\union j's in
				Set<DefaultWeightedEdge> e_incoming_i = gPrev.incomingEdgesOf(i);
				Set<DefaultWeightedEdge> e_incoming_j = gPrev.incomingEdgesOf(j);

				Set<Integer> ks = LabelPropagationMNWithinGraph.getUnionIntersection(gPrev, e_incoming_i, e_incoming_j,
						true, true, false);

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
						System.err
								.println("both incoming edges zero, how??" + i + " " + j + " " + k + " " + pgraph.name);
						System.exit(0);
					}

					numerator -= w_ji * Math.pow(w_ki - w_kj, 2);
					// System.out.println("numerator small 3 j: "+pgraph.nodes.get(i).id+"
					// "+pgraph.nodes.get(j).id+" "+pgraph.nodes.get(k).id+" "+w_ji * Math.pow(w_ki
					// - w_kj, 2));
				}

				// Now, form i's out \\union j's out
				Set<DefaultWeightedEdge> e_outgoing_i = gPrev.outgoingEdgesOf(i);
				Set<DefaultWeightedEdge> e_outgoing_j = gPrev.outgoingEdgesOf(j);

				ks = LabelPropagationMNWithinGraph.getUnionIntersection(gPrev, e_outgoing_i, e_outgoing_j, false, false,
						false);
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
						System.err
								.println("both outgoing edges zero, how??" + i + " " + j + " " + k + " " + pgraph.name);
						System.exit(0);
					}

					numerator -= w_ji * Math.pow(w_ik - w_jk, 2);
					// System.out.println("numerator small 3 j: "+pgraph.nodes.get(i).id+"
					// "+pgraph.nodes.get(j).id+" "+pgraph.nodes.get(k).id+" "+w_ji * Math.pow(w_ik
					// - w_jk, 2));
				}

				LabelPropagateMN.numOperations += e_incoming_i.size() + e_incoming_j.size() + e_outgoing_i.size()
						+ e_outgoing_j.size();
				// System.out.println("here num op: "+LabelPropagateMN.numOperations+" "+w_ji+"
				// "+LabelPropagateMN.numPassedEdges);

				// now, use denominator and numerator for i and j
				if (numerator > 0) {
					System.err.println("numerator >0, how??");
					System.exit(0);
				}

				// System.out.println("numerator 3 j: "+pgraph.nodes.get(i).id+"
				// "+pgraph.nodes.get(j).id+" "+numerator);

				// TODO: added, be careful
				// numerator/=(pgraph.nodes.get(i).getNumNeighs()+pgraph.nodes.get(j).getNumNeighs());

				LabelPropagationMNWithinGraph.addNumeratorDenom(pgraph, i, j, numerator, 0);
			}
		}

		// (1) in formulations: add to numerator of w_ij: w_jk*w_kj*w_ik and
		// denominator: w_jk*w_kj
		// (2) in formulations: add to numerator of w_ij: w_ik*w_ki*w_kj and
		// denominator: w_ik*w_ki. For 2, i and j are really reverse in the below code

		// Set<String> nzijs = new HashSet<>();
		for (int k = 0; k < gPrev.vertexSet().size(); k++) {
			if (k % numThreads != threadIdx) {
				continue;
			}
			if ((k % numThreads) % 100 == 0) {
				System.out.println("1,2 k: " + k + " " + pgraph.name);
			}
			// Find k's out \\intersection k's in
			Set<DefaultWeightedEdge> e_outgoing_k = gPrev.outgoingEdgesOf(k);
			Set<DefaultWeightedEdge> e_incoming_k = gPrev.incomingEdgesOf(k);

			Set<Integer> k_neighs = LabelPropagationMNWithinGraph.getUnionIntersection(gPrev, e_outgoing_k,
					e_incoming_k, false, true, true);

			LabelPropagateMN.numOperations += e_incoming_k.size() * e_incoming_k.size()
					+ e_outgoing_k.size() * e_outgoing_k.size();

			for (int j : k_neighs) {

				if (j == k) {
					continue;
				}

				double w_jk = gPrev.getEdgeWeight(gPrev.getEdge(j, k)) - ConstantsSoftConst.epsilon;
				double w_kj = gPrev.getEdgeWeight(gPrev.getEdge(k, j)) - ConstantsSoftConst.epsilon;

				if (w_jk <= 0 || w_kj <= 0) {
					continue;
				} else {
					// System.out.println("wjk great than 0: "+w_jk+" "+w_kj+"
					// "+TypePropagateMN.tau);
				}

				if (!ConstantsSoftConst.obj1) {
					w_jk += ConstantsSoftConst.epsilon;
					w_kj += ConstantsSoftConst.epsilon;
				}

				double denom = 2 * w_jk * w_kj;

				// TODO: added, be careful
				// double sumNeighs =
				// pgraph.nodes.get(j).getNumNeighs()+pgraph.nodes.get(k).getNumNeighs();
				// denom /= sumNeighs;

				reflexSumWeights[j] += denom;

				// (1) in formulation
				for (DefaultWeightedEdge e : e_incoming_k) {
					int i = gPrev.getEdgeSource(e);
					if (i == k || i == j) {
						continue;
					}

					double w_ik = gPrev.getEdgeWeight(e);

					double numerator = denom * w_ik;

					// nzijs.add(i + "#" + j);

					// System.out.println("numerator 1 k: "+pgraph.nodes.get(i).id+"
					// "+pgraph.nodes.get(j).id+" "+ pgraph.nodes.get(k).id+" "+ numerator);

					LabelPropagationMNWithinGraph.addNumeratorDenom(pgraph, i, j, numerator, 0);// save all denoms for
																								// later!
				}

				// (2) in formulation
				for (DefaultWeightedEdge e : e_outgoing_k) {
					int i = gPrev.getEdgeSource(e);
					if (i == k || i == j) {
						continue;
					}

					double w_ki = gPrev.getEdgeWeight(e);
					// nzijs.add(j + "#" + i);

					double numerator = denom * w_ki;

					// System.out.println("numerator 2 k: "+pgraph.nodes.get(j).id+"
					// "+pgraph.nodes.get(i).id+" "+ pgraph.nodes.get(k).id+" "+numerator);

					LabelPropagationMNWithinGraph.addNumeratorDenom(pgraph, j, i, numerator, 0);// save all denoms for
																								// later!
				}
			}

		}
		System.out.println("here num op: " + LabelPropagateMN.numOperations + " " + LabelPropagateMN.numPassedEdges);
	}

	static Set<Integer> getUnionIntersection(DefaultDirectedWeightedGraph<Integer, DefaultWeightedEdge> g,
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

	static void addNumeratorDenom(PGraph pgraph, int i, int j, double numerator, double denom) {
		if (numerator == 0 && denom == 0) {
			// System.err.println("both numer and denom zero "+i+" "+j+" "+pgraph.name);
			return;
		}
		DefaultDirectedWeightedGraph<Integer, DefaultWeightedEdge> gMN = pgraph.gMN;
		double w;
		DefaultWeightedEdge ee;

		if (numerator != 0) {
			synchronized (gMN) {
				if (!gMN.containsEdge(i, j)) {
					ee = gMN.addEdge(i, j);
					// This means the edges hasn't been added in the typeProp phase, so add it's
					// default value to numerator

					w = 0;
					if (pgraph.g0.containsEdge(i, j)) {
						// DefaultWeightedEdge e0 = pgraph.g0.getEdge(i, j);
						// w = pgraph.g0.getEdgeWeight(e0);

						System.err.println("g0 has the edge, but it hasn't been propagated!!!");
						// System.exit(1);
					}
					gMN.setEdgeWeight(ee, w);
					TypePropagateMN.allPropEdges++;
				} else {
					ee = gMN.getEdge(i, j);
					w = gMN.getEdgeWeight(ee);
				}

				gMN.setEdgeWeight(ee, w + numerator);

			}
		}

		if (denom != 0) {
			String edgeStr = i + "#" + j;
			synchronized (pgraph.edgeToMNWeight) {
				if (!pgraph.edgeToMNWeight.containsKey(edgeStr)) {
					pgraph.edgeToMNWeight.put(edgeStr, 1 + denom);// we still need to add the "1" that we haven't added
																	// any other place! Although, the numerator is 0!
				} else {
					double prevDenom = pgraph.edgeToMNWeight.get(edgeStr);
					pgraph.edgeToMNWeight.put(edgeStr, prevDenom + denom);
				}
			}
		}

	}

	static void addDenomNewEdge(PGraph pgraph, int i, int j) {

		String edgeStr = i + "#" + j;
		synchronized (pgraph.edgeToMNWeight) {
			if (!pgraph.edgeToMNWeight.containsKey(edgeStr)) {
				pgraph.edgeToMNWeight.put(edgeStr, 1.0);// we still need to add the "1" that we haven't added
																// any other place! Although, the numerator is 0!
			}
		}

	}

}
