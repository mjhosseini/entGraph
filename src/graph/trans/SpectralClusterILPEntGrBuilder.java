package graph.trans;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import graph.Node;
import graph.Oedge;
import graph.PGraph;

//Builds entailment graph by first doing spectral clustering into C clusters. Then run ILP between clusters.

public class SpectralClusterILPEntGrBuilder {
	PGraph pgraph;
	SpectralClustering specCluster;
	double lmbda;
	PrintStream op;
	gtGraph scc;

	public SpectralClusterILPEntGrBuilder(PGraph pgraph, SpectralClustering specCluster, double lmbda, PrintStream op) {
		this.pgraph = pgraph;
		this.specCluster = specCluster;
		this.lmbda = lmbda;
		this.op = op;
	}

	void buildEntGraph() {
		PGraph pgraph2 = pgraph;
		if (specCluster != null) {
			pgraph2 = makeClusterPGraph();
		}

		LinProgTransGraphBuilder lptgb = new LinProgTransGraphBuilder(pgraph2, lmbda);
		this.scc = lptgb.findTransGraph();

		if (specCluster != null) {
			makeSCCFinal(scc);
		}
	}

	void writeEntGraph() {
		TransClUtils.writeSCC(scc, lmbda, op, pgraph);
		double obj = TransClUtils.computeObj(scc, pgraph, lmbda);
		System.out.println("objective function: " + obj);
	}

	void makeSCCFinal(gtGraph scc0) {
		List<List<Integer>> origComps = new ArrayList<>();
		for (int i = 0; i < scc0.comps.size(); i++) {
			List<Integer> clusterGraphNodes = scc0.comps.get(i);// a component from cluster graph nodes
			List<Integer> thisC = new ArrayList<>();
			for (int k : clusterGraphNodes) {
				for (int j : specCluster.clusters.get(k)) {
					thisC.add(j);
				}
			}
			origComps.add(thisC);
		}

		scc0.comps = origComps;
	}

	PGraph makeClusterPGraph() {
		PGraph cluPGraph = new PGraph();

		cluPGraph.fname = pgraph.fname;
		cluPGraph.nodes = new ArrayList<>();
		cluPGraph.idx2node = new HashMap<>();
		cluPGraph.pred2node = new HashMap<>();
		cluPGraph.types = pgraph.types;
		cluPGraph.name = cluPGraph.types;

		// insert all the nodes
		List<List<Integer>> clusters = specCluster.clusters;
		// Map<String, Integer> clusterPair2EdgeCount = new HashMap<>();
		int[] labels = specCluster.labels;
		for (int k = 0; k < clusters.size(); k++) {
			Node node = new Node(k, pgraph.nodes.get(clusters.get(k).get(0)).id);
			cluPGraph.insertNode(node);
		}

		// insert all edges
		for (Node node : pgraph.nodes) {
			int k = labels[node.idx];
			Node node1 = cluPGraph.nodes.get(k);
			int size1 = clusters.get(k).size();
			for (Oedge oedge : node.oedges) {
				int k2 = labels[oedge.nIdx];
				if (k == k2) {
					continue;
				}
				int size2 = clusters.get(k2).size();
				if (!node1.idx2oedges.containsKey(k2)) {
					node1.addNeighbor(k2, oedge.sim / (size1 * size2));
					// clusterPair2EdgeCount.put(k + "#" + k2, 1);
				} else {
					node1.idx2oedges.get(k2).sim += oedge.sim / (size1 * size2);
					// System.out.println(clusterPair2EdgeCount.get(k + "#" + k2));
					// clusterPair2EdgeCount.put(k + "#" + k2, clusterPair2EdgeCount.get(k + "#" +
					// k2) + 1);
				}
			}
		}

		// divide the edges
		// for (Node node : cluPGraph.nodes) {
		// int k = node.idx;
		// for (Oedge oedge : node.oedges) {
		// int k2 = oedge.nIdx;
		// oedge.sim /= clusterPair2EdgeCount.get(k + "#" + k2);
		// }
		// }

		return cluPGraph;
	}

}
