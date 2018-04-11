package graph;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.List;

import graph.PGraph.TransitiveMethod;

public class EntGraphBuilderRunner implements Runnable {

	String fname;
	List<Float> lmbdas;

	public EntGraphBuilderRunner(String fname, List<Float> lmbdas) {
		this.fname = fname;
		this.lmbdas = lmbdas;
	}

	@Override
	public void run() {
		PGraph pgraph = new PGraph(PGraph.root + fname);
		if (pgraph.nodes.size() == 0) {
			return;
		}

		System.out.println("allEdgesRem, allEdges: " + PGraph.allEdgesRemained + " " + PGraph.allEdges);

		if (!PGraph.formBinaryGraph) {
			return;
		}

		int lastDotIdx = pgraph.fname.lastIndexOf('.');

		// if (!PGraph.checkFrgVio) {
		// PGraph.graphPostFix = "_graphsNoFrg2.txt";
		// }

		String outPath = pgraph.fname.substring(0, lastDotIdx) + PGraph.graphPostFix;
		PrintStream op = null;
		try {
			op = new PrintStream(new File(outPath));
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}

		SpectralClustering specClusterer = null;
		if (PGraph.transMethod == TransitiveMethod.SpectralILPWithin) {
			// if (pgraph.nodes.size() > PGraph.specILPMaxClusterSizeAllowed) {
			int K = (int) Math.ceil((double) (pgraph.nodes.size()) / PGraph.specILPMaxClusterAllowed);
			System.out.println("num clusters: " + K + " num node: " + pgraph.nodes.size());
			specClusterer = new SpectralClustering(pgraph, K, PGraph.specILPMaxClusterSizeAllowed);
			specClusterer.cluster();
			System.out.println("clustering done");
			// }
		}

		for (float lmbda : lmbdas) {
			System.out.println("lambda: " + lmbda);

			// System.out.println("Berant's HTL started");
			if (PGraph.transMethod == TransitiveMethod.BerantTNF) {
				EOPTNF.formEntGraph(pgraph, lmbda, op);
			}
			// System.out.println("Berant's HTL finished");

			else if (PGraph.transMethod == TransitiveMethod.HTLFRG) {

				TransClUtils tnf = new TransClUtils(pgraph, op, lmbda, PGraph.checkFrgVio, null);
				if (PGraph.shouldWrite) {
					tnf.writeSCC();
				}

				double obj = TransClUtils.computeObj(tnf.scc, pgraph, lmbda);
				System.out.println("objective function: " + obj);

			}

			else if (PGraph.transMethod == TransitiveMethod.SpectralILP) {

				if (pgraph.nodes.size() > PGraph.specILPMaxClusterAllowed) {
					specClusterer = new SpectralClustering(pgraph, PGraph.specILPMaxClusterAllowed);
					specClusterer.cluster();
				}

				SpectralClusterILPEntGrBuilder specILP = new SpectralClusterILPEntGrBuilder(pgraph, specClusterer,
						lmbda, op);

				specILP.buildEntGraph();
				specILP.writeEntGraph();
			} else {
				System.out.println("specclusterwithin");

				SpecCluILPWithinEntGrBuilder specILP = new SpecCluILPWithinEntGrBuilder(pgraph, specClusterer, lmbda,
						op);

				specILP.buildEntGraph();
				specILP.writeEntGraph();
				System.out.println("done specILP " +fname+ " "+lmbda);
			}

		}
		op.close();
		System.out.println("entGraph done: "+fname);
	}

}
