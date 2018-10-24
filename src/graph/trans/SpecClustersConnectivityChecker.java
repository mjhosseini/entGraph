package graph.trans;

import java.util.List;
import java.util.Map;

//We've already done ILP inside each cluster. Given two nodes, we see if we have already decided about its connectivity
public class SpecClustersConnectivityChecker implements ConnectivityChecker {

	List<gtGraph> sccs;
	List<List<Integer>> clusters;
	int[] labels;
	List<int[]> node2compList;
	List<Map<Integer, Integer>> origIdxtoIdxList;

	// specCluster is the main clustering on the nodes
	public SpecClustersConnectivityChecker(List<gtGraph> sccs, List<int[]> node2compList,
			SpectralClustering specCluster, List<Map<Integer, Integer>> origIdxtoIdxList) {
		this.sccs = sccs;
		this.clusters = specCluster.clusters;
		this.labels = specCluster.labels;
		this.node2compList = node2compList;
		this.origIdxtoIdxList = origIdxtoIdxList;
	}

	@Override
	public int isConnected(int i, int j) {
		if (labels[i] != labels[j]) {// They weren't originally in one cluster, so we don't know they're labels.
			return -1;
		}

		int k = labels[i];
		int idx1 = origIdxtoIdxList.get(k).get(i);
		int idx2 = origIdxtoIdxList.get(k).get(j);
		gtGraph scc = sccs.get(k);
		int[] node2comp = node2compList.get(k);
		boolean connected = TransClUtils.isConnectedScc(scc, node2comp, idx1, idx2);
		return connected ? 1 : 0;

	}

}
