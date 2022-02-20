package graph.softConst;

import java.util.HashSet;
import java.util.Set;

import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;

import constants.ConstantsSoftConst;
import graph.PGraph;

public class LabelPropagationMNWithinGraphTrans implements Runnable {
	int numThreads;
	int threadIdx;
	PGraph pgraph;
	Set<String> touchedEdges;
	public static long numVio = 0;

	public LabelPropagationMNWithinGraphTrans(int threadIdx, int numThreads, PGraph pgraph) {
		this.threadIdx = threadIdx;
		this.numThreads = numThreads;
		this.pgraph = pgraph;
		touchedEdges = new HashSet<>();
	}

	@Override
	public void run() {
		DefaultDirectedWeightedGraph<Integer, DefaultWeightedEdge> gPrev = pgraph.g0;

		// let's compute \sum_k [1(wij<=wjk ^ wij>wik) + 1(wij<=wki ^ wij>wkj) -
		// 1(wij<=min(wik,wkj))]

		// first, let's do: \sum_k 1(wij<=wki ^ wij>wkj), then \sum_k 1(wij<=wjk ^
		// wij>wik), and finally \sum_k 1(wij<=min(wik,wkj))

		for (int i = 0; i < gPrev.vertexSet().size(); i++) {

			if (i % numThreads != threadIdx) {
				continue;
			}

			if ((i / numThreads) % 100 == 0) {
				System.out.println("A,B i: " + i + " " + pgraph.name);
			}

			for (DefaultWeightedEdge e : gPrev.outgoingEdgesOf(i)) {
				int j = gPrev.getEdgeTarget(e);

				if (i == j) {
					continue;
				}
				double w_ij = gPrev.getEdgeWeight(gPrev.getEdge(i, j));
				if (w_ij <= ConstantsSoftConst.epsilonTrans) {
					continue;
				}

				LabelPropagateMN.numPassedEdges++;

				double numerator = 0;

				// Now, let's look at i's input: This is (B) in formulation

				Set<DefaultWeightedEdge> e_incoming_i = gPrev.incomingEdgesOf(i);

				Set<Integer> ks = LabelPropagationMNWithinGraph.getUnionIntersection(gPrev, e_incoming_i,
						new HashSet<>(), true, true, false);

				for (int k : ks) {
					if (k == i || k == j) {
						continue;
					}
					double w_ki = 0, w_kj = 0;

					w_ki = gPrev.getEdgeWeight(gPrev.getEdge(k, i));
					if (w_ki <= ConstantsSoftConst.epsilonTrans) {
						continue;
					}

					if (gPrev.containsEdge(k, j)) {
						w_kj = gPrev.getEdgeWeight(gPrev.getEdge(k, j));
					}

					if (w_ki == 0) {
						System.err.println("incoming edge zero, how??" + i + " " + j + " " + k + " " + pgraph.name);
						System.exit(0);
					}

					if (w_ij < w_ki && w_ij > w_kj) {// && i < k && !touchedEdges.contains(k + "#" + j)
						numerator -= ConstantsSoftConst.lmbda3 / 2;
						// touchedEdges.add(i + "#" + j);
						// System.out.println(
						// "violation B: " + k + " " + i + " " + j + " " + w_ki + " " + w_ij + " " +
						// w_kj);
						// System.out.println("decrease: " + i + " " + j);
						numVio++;
					}

				}

				// Now, let's look at j's output: This is (A) in formulation
				Set<DefaultWeightedEdge> e_outgoing_j = gPrev.outgoingEdgesOf(j);

				ks = LabelPropagationMNWithinGraph.getUnionIntersection(gPrev, new HashSet<>(), e_outgoing_j, false,
						false, false);
				for (int k : ks) {
					if (k == i || k == j) {
						continue;
					}
					double w_jk = 0, w_ik = 0;
					if (w_ij <= ConstantsSoftConst.epsilonTrans) {
						continue;
					}

					if (gPrev.containsEdge(i, k)) {
						w_ik = gPrev.getEdgeWeight(gPrev.getEdge(i, k));
					}

					if (gPrev.containsEdge(j, k)) {
						w_jk = gPrev.getEdgeWeight(gPrev.getEdge(j, k));
					}

					if (w_jk == 0) {
						System.err.println("outgoing edge zero, how??" + i + " " + j + " " + k + " " + pgraph.name);
						System.exit(0);
					}

					if (w_ij < w_jk && w_ij > w_ik) {// && j < k && !touchedEdges.contains(i + "#" + k)
						numerator -= ConstantsSoftConst.lmbda3 / 2;
						// touchedEdges.add(i + "#" + j);
						// System.out.println(
						// "violation A: " + i + " " + j + " " + k + " " + w_ij + " " + w_jk + " " +
						// w_ik);
						// System.out.println("decrease: " + i + " " + j);
						numVio++;
					}

				}

				LabelPropagationMNWithinGraph.addNumeratorDenom(pgraph, i, j, numerator, 0);
			}
		}

		// Now, let's do (C) in the formulation

		for (int k = 0; k < gPrev.vertexSet().size(); k++) {
			if (k % numThreads != threadIdx) {
				continue;
			}
			if ((k % numThreads) % 100 == 0) {
				System.out.println("C k: " + k + " " + pgraph.name);
			}

			// Find k's in and out
			Set<DefaultWeightedEdge> e_incoming_k = gPrev.incomingEdgesOf(k);
			Set<DefaultWeightedEdge> e_outgoing_k = gPrev.outgoingEdgesOf(k);

			Set<Integer> is = LabelPropagationMNWithinGraph.getUnionIntersection(gPrev, e_incoming_k, new HashSet<>(),
					true, false, false);
			Set<Integer> js = LabelPropagationMNWithinGraph.getUnionIntersection(gPrev, new HashSet<>(), e_outgoing_k,
					true, false, false);

			// LabelPropagateMN.numOperations += e_incoming_k.size() * e_incoming_k.size()
			// + e_outgoing_k.size() * e_outgoing_k.size();

			for (int i : is) {

				if (i == k) {
					continue;
				}

				double w_ik = gPrev.getEdgeWeight(gPrev.getEdge(i, k));
				if (w_ik <= ConstantsSoftConst.epsilonTrans) {
					continue;
				}

				for (int j : js) {
					if (j == k || j == i) {
						continue;
					}

					double w_kj = gPrev.getEdgeWeight(gPrev.getEdge(k, j));
					if (w_kj <= ConstantsSoftConst.epsilonTrans) {
						continue;
					}
					double w_ij = 0;
					if (gPrev.containsEdge(i, j)) {
						w_ij = gPrev.getEdgeWeight(gPrev.getEdge(i, j));
					}

					double numerator = 0;

					if (w_ij < w_ik && w_ij < w_kj) {
						// if ((w_ik < w_kj && j < k && !touchedEdges.contains(i + "#" + k))
						// || (i < k && !touchedEdges.contains(k + "#" + j))) {
						// touchedEdges.add(i + "#" + j);
						numerator += ConstantsSoftConst.lmbda3 / 2;
						// System.out.println(
						// "violation C: " + i + " " + k + " " + j + " " + w_ik + " " + w_kj + " " +
						// w_ij);
						// System.out.println("increase: " + i + " " + j);
						LabelPropagationMNWithinGraph.addNumeratorDenom(pgraph, i, j, numerator, 0);
						numVio++;
						// }
					}

				}

			}

		}

		// System.out.println("here num op: " + LabelPropagateMN.numOperations + " " +
		// LabelPropagateMN.numPassedEdges);
	}
}
