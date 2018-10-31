package graph.trans;

import java.util.ArrayList;
import java.util.List;

import constants.ConstantsTrans;
import graph.Node;
import graph.Oedge;
import graph.PGraph;

//gets a pgraph, runs spectral clustering.
//if M given, makes sure cluster sizes < M

public class SpectralClustering {
	PGraph pgraph;
	private int K;// initial K
	int N;
	List<List<Integer>> clusters;
	int[] labels;
	int M;
	private double[][] w;
	
	public SpectralClustering(PGraph pgraph, int K) {
		this(pgraph, K, -1);
	}

	public SpectralClustering(PGraph pgraph, int K, int M) {
		this.pgraph = pgraph;
		this.N = pgraph.nodes.size();
		this.K = K;
		this.M = M;
	}

	public void cluster() {

		if (pgraph.nodes.size() > 45000) {
			clusters = TransClUtils.findComponents(pgraph, 0.15);
			int maxSize = 0;
			for (List<Integer> clu:clusters) {
				if (clu.size()>maxSize) {
					maxSize = clu.size();
				}
			}
			System.out.println("first max size: "+maxSize);
			fillLabels();
			K = -1;

		} else {
			// put everything in one cluster
			clusters = new ArrayList<>();
			List<Integer> clu = new ArrayList<>();
			clusters.add(clu);
			for (int i = 0; i < pgraph.nodes.size(); i++) {
				clu.add(i);
			}
			fillLabels();
		}

		this.w = getAdjMatrix(pgraph);
		// smile.clustering.SpectralClustering clusterer = new
		// smile.clustering.SpectralClustering(w, K);
		// int[] y = clusterer.getClusterLabel();
		// this.labels = y;
		// this.clusters = new ArrayList<>();
		// for (int k = 0; k < K; k++) {
		// clusters.add(new ArrayList<>());
		// }
		// for (int i = 0; i < y.length; i++) {
		// int thisK = y[i];
		// clusters.get(thisK).add(i);
		// }
		//
		// System.err.println("clustering res: ");
		// int maxSize = 0;
		// for (int k = 0; k < clusters.size(); k++) {
		// int thisSize = clusters.get(k).size();
		// if (thisSize > maxSize) {
		// maxSize = thisSize;
		// }
		// System.out.println("cluster " + k + " size:" + thisSize);
		// for (int i : clusters.get(k)) {
		// System.out.println(pgraph.nodes.get(i).id);
		// }
		// System.out.println();
		// }
		// System.out.println("maxSize: " + maxSize);

		// if (maxSize > M) {
		reClusterAll(K);
		// }

		// remove empty cluster
		removeEmptyClus();
	}

	void removeEmptyClus() {
		List<List<Integer>> newClusters = new ArrayList<>();
		for (List<Integer> clu : this.clusters) {
			if (clu.size() > 0) {
				newClusters.add(clu);
			}
		}
		this.clusters = newClusters;

		fillLabels();

		System.err.println("final clustering res ");
		for (int k = 0; k < clusters.size(); k++) {
			int thisSize = clusters.get(k).size();

			System.out.println("cluster " + k + " size:" + thisSize);
			for (int i : clusters.get(k)) {
				System.out.println(pgraph.nodes.get(i).id);
			}
			System.out.println();
		}

	}

	void fillLabels() {
		this.labels = new int[N];
		int k = 0;
		for (List<Integer> clu : this.clusters) {
			for (int i : clu) {
				this.labels[i] = k;
			}
			k++;
		}
	}

	// reclusters the clusters that are larger than K elemen
	void reClusterAll(int firstK) {

		boolean Msatisfied = false;
		while (!Msatisfied) {
			System.out.println("reclustering" + pgraph.name);
			Msatisfied = true;// unless we realize later!
			List<List<Integer>> newClusters = new ArrayList<>();
			for (List<Integer> clu : clusters) {
				if (clu.size() > M || firstK != -1) {
					// recluster a certain cluster
					List<List<Integer>> thisClusters = reCluster(clu, firstK);
					firstK = -1;// because that's only for the first time!
					for (List<Integer> newClu : thisClusters) {
						if (newClu.size() > M) {
							Msatisfied = false;
						}
						newClusters.add(newClu);
					}
				} else {
					newClusters.add(clu);
				}
			}
			this.clusters = newClusters;
		}

		fillLabels();

		System.err.println("Msatistied clustering res: ");
		int maxSize = 0;
		for (int k = 0; k < clusters.size(); k++) {
			int thisSize = clusters.get(k).size();
			if (thisSize > maxSize) {
				maxSize = thisSize;
			}
			System.out.println("cluster " + k + " size:" + thisSize);
			for (int i : clusters.get(k)) {
				System.out.println(pgraph.nodes.get(i).id);
			}
			System.out.println();
		}
		System.out.println("Msatistied maxSize: " + maxSize + " num clusters: " + clusters.size());
	}

	private List<List<Integer>> reCluster(List<Integer> clu, int firstK) {

		List<List<Integer>> thisClusters = new ArrayList<>();

		// First, let's separate the isolated nodes
		List<Integer> clup = new ArrayList<>();// the idxes that are not isolated
		for (int i = 0; i < clu.size(); i++) {
			int x = clu.get(i);
			boolean isolated = true;
			for (int j = 0; j < clu.size(); j++) {
				int y = clu.get(j);
				if (w[x][y] != 0) {
					isolated = false;
					break;
				}
			}
			if (isolated) {
				List<Integer> l = new ArrayList<>();
				l.add(x);
				thisClusters.add(l);
			} else {
				clup.add(x);
			}
		}

		clu = clup;

		double[][] thisW = getSubsetW(clu, this.w);
		System.out.println("thisW size: " + thisW.length);

		int thisK;
		if (firstK != -1) {
			thisK = firstK;
		} else {
			thisK = (int) Math.ceil((double) (clu.size()) / ConstantsTrans.specILPMaxClusterAllowed);

		}
		if (thisK != 1 && thisK != 0) {
			smile.clustering.SpectralClustering clusterer = new smile.clustering.SpectralClustering(thisW, thisK);
			int[] y = clusterer.getClusterLabel();

			for (int k = 0; k < thisK; k++) {
				thisClusters.add(new ArrayList<>());
			}
			for (int i = 0; i < y.length; i++) {
				int thisk = y[i];
				thisClusters.get(thisk).add(clu.get(i));
			}
		} else {
			ArrayList<Integer> singClu = new ArrayList<>();
			for (int i : clu) {
				singClu.add(i);
			}
			thisClusters.add(singClu);
		}

		return thisClusters;
	}

	private double[][] getSubsetW(List<Integer> clu, double[][] w) {
		int S = clu.size();
		double[][] subsetW = new double[S][S];
		for (int i = 0; i < S; i++) {
			for (int j = 0; j < S; j++) {
				subsetW[i][j] = w[clu.get(i)][clu.get(j)];
			}
		}
		return subsetW;
	}

	double[][] getAdjMatrix(PGraph pgraph) {
		int N = pgraph.nodes.size();
		double[][] w = new double[N][N];
		for (int i = 0; i < N; i++) {
			Node n = pgraph.nodes.get(i);

			for (Oedge oedge : n.oedges) {

				double sim = oedge.sim;// -lmbda;
				if (sim <= 0) {
					continue;
				}

				if (w[i][oedge.nIdx] == 0) {
					w[i][oedge.nIdx] = 1;
				}
				if (w[oedge.nIdx][i] == 0) {
					w[oedge.nIdx][i] = 1;
				}
				w[i][oedge.nIdx] *= Math.sqrt(sim);
				w[oedge.nIdx][i] *= Math.sqrt(sim);
			}
			w[i][i] = 0;
		}
		return w;
	}

}
