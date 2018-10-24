package graph.trans;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import constants.ConstantsGraphs;
import graph.PGraph;

//Gets a pgraph and a lambda, runs ILP, returns transitive graph!

public class LinProgTransGraphBuilder {
	PGraph pgraph;
	double lmbda;
	gtGraph scc;
	int[] node2comp;

	public LinProgTransGraphBuilder(PGraph pgraph, double lmbda) {
		this.lmbda = lmbda;
		this.pgraph = pgraph;
	}

	public LinProgTransGraphBuilder(String fname, double lmbda) {
		this.lmbda = lmbda;
		this.pgraph = new PGraph(ConstantsGraphs.root + fname);
	}

	gtGraph findTransGraph() {

		int N = pgraph.nodes.size();
		System.err.println("N: "+N);

		this.node2comp = new int[N];
		gtGraph scc = new gtGraph(DefaultEdge.class);

		for (int i = 0; i < N; i++) {
			scc.addVertex(i);
			List<Integer> l = new ArrayList<>();
			l.add(i);
			scc.comps.add(l);
			node2comp[i] = i;
		}

		List<List<Integer>> cc = TransClUtils.findComponents(pgraph, lmbda);

		for (List<Integer> c : cc) {
			System.out.println("component: " + c.size() + " elements");
			// for (int x:c) {
			// System.out.print(x+" ");
			// }
			LinProgCplex lp = new LinProgCplex(pgraph, lmbda, c);
			double[] sol = lp.solveILPIncremental();
			int Nc = c.size();
			for (int idx = 0; idx < sol.length; idx++) {
				if (sol[idx] == 1.0) {
					int i0 = idx / Nc;
					int j0 = idx % Nc;

					int i = c.get(i0);
					int j = c.get(j0);

					scc.addEdge(i, j);

					// System.out.println(pgraph.nodes.get(idxes.get(i)).id + "=>" +
					// pgraph.nodes.get(idxes.get(j)).id);
					// System.out.println(idxes.get(i) + "=>" + idxes.get(j));
				}
			}

		}

		this.scc = TransClUtils.updateSCC(scc, node2comp);
		return scc;

	}

	public static void main(String[] args) {

		File folder = new File(ConstantsGraphs.root);
		File[] files = folder.listFiles();
		Arrays.sort(files);

		for (File f : files) {
			String fname = f.getName();
			if (!fname.contains("broadcast_network#thing")) {
				continue;
			}
			// if (fname.startsWith("location#location_sim.txt")) {
			// seenLoc = true;
			// }
			// if (seenLoc) {
			// break;
			// }

			if (!fname.contains(ConstantsGraphs.suffix)) {
				continue;
			}

			// if (gc++==50) {
			// break;
			// }

			System.out.println("fname: " + fname);
			LinProgTransGraphBuilder lpRunner = new LinProgTransGraphBuilder(fname, .2);
			lpRunner.findTransGraph();
			// LinProg linProg = new LinProg(pgraph, .2);
			// linProg.solveILPIncremental();

		}
	}

}
