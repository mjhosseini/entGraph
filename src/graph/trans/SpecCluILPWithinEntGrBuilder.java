package graph.trans;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import constants.ConstantsTrans;
import graph.Node;
import graph.Oedge;
import graph.PGraph;

//Builds entailment graph by first doing spectral clustering into C clusters. Then run ILP on each cluster. Then, run the HTL method
//on the rest!

public class SpecCluILPWithinEntGrBuilder {
	PGraph pgraph;
	SpectralClustering specCluster;
	double lmbda;
	PrintStream op;
	gtGraph scc;

	public SpecCluILPWithinEntGrBuilder(PGraph pgraph, SpectralClustering specCluster, double lmbda, PrintStream op) {
		this.pgraph = pgraph;
		this.specCluster = specCluster;
		this.lmbda = lmbda;
		this.op = op;
	}

	void buildEntGraph() {

		List<Map<Integer, Integer>> origIdxtoIdxList = new ArrayList<>();
		List<PGraph> cluPGraphs = makeClusterPGraphs(origIdxtoIdxList);
		List<gtGraph> sccs = new ArrayList<>();
		List<int[]> node2compList = new ArrayList<>();

		for (PGraph cluPGraph : cluPGraphs) {

			LinProgTransGraphBuilder lptgb = new LinProgTransGraphBuilder(cluPGraph, lmbda);
			gtGraph scc = lptgb.findTransGraph();// this is the transitive entailment graph for one of the (small)
													// clusters
			sccs.add(scc);
			node2compList.add(lptgb.node2comp);
			System.out.println("ILP done for: "+cluPGraph.name);
		}

		ConnectivityChecker spConnectivityChecker = new SpecClustersConnectivityChecker(sccs, node2compList,
				specCluster, origIdxtoIdxList);

		TransClUtils tnf = new TransClUtils(pgraph, op, lmbda, ConstantsTrans.checkFrgVio, spConnectivityChecker);
		this.scc = tnf.scc;
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

	List<PGraph> makeClusterPGraphs(List<Map<Integer, Integer>> origIdxtoIdxList) {
		List<PGraph> pgraphs = new ArrayList<>();
		int cIdx = 0;
		
		for (List<Integer> clu : specCluster.clusters) {

			Map<Integer, Integer> origIdxtoIdx = new HashMap<>();
			origIdxtoIdxList.add(origIdxtoIdx);

			// if (clu.size()==0) {
			// continue;
			// }

			PGraph thisPgraph = makeOneClusterPGraph(pgraph, clu, cIdx, origIdxtoIdx);
			pgraphs.add(thisPgraph);
			System.out.println("thisNL " + thisPgraph.nodes.size());
			cIdx++;
		}
		return pgraphs;
	}

	// make a pgraph for the nodes in the cluster
	PGraph makeOneClusterPGraph(PGraph pgraph, List<Integer> clu, int cIdx, Map<Integer, Integer> origIdxtoIdx) {
		PGraph cluPGraph = new PGraph();

		cluPGraph.fname = pgraph.fname + " " + cIdx;
		cluPGraph.nodes = new ArrayList<>();
		cluPGraph.idx2node = new HashMap<>();
		cluPGraph.pred2node = new HashMap<>();
		cluPGraph.types = pgraph.types;
		cluPGraph.name = cluPGraph.types + " " + cIdx;

		// insert the nodes
		for (int i = 0; i < clu.size(); i++) {
			origIdxtoIdx.put(clu.get(i), i);
			Node node = new Node(i, pgraph.nodes.get(clu.get(i)).id);
			cluPGraph.insertNode(node);
		}

		// insert all edges
		for (int i : clu) {
			Node thisNode = cluPGraph.nodes.get(origIdxtoIdx.get(i));
			Node origNode = pgraph.nodes.get(i);

			for (Oedge oedge : origNode.oedges) {
				if (!origIdxtoIdx.containsKey(oedge.nIdx)) {// not inside this cluster!
					continue;
				}

				int idx2 = origIdxtoIdx.get(oedge.nIdx);
				thisNode.addNeighbor(idx2, oedge.sim);
			}
		}

		return cluPGraph;
	}

}
