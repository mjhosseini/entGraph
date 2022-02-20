package graph.trans;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.List;

import constants.ConstantsGraphs;
import constants.ConstantsTrans;
import graph.PGraph;
import graph.PGraph.TransitiveMethod;

public class EntGraphBuilderRunner implements Runnable {

	List<Float> lmbdas;
	PGraph pgraph;

	public EntGraphBuilderRunner(PGraph pgraph, List<Float> lmbdas) {
		this.pgraph = pgraph;
		this.lmbdas = lmbdas;
	}

	@Override
	public void run() {

		System.out.println("allEdgesRem, allEdges: " + PGraph.allEdgesRemained + " " + PGraph.allEdges);

		if (!ConstantsTrans.formBinaryGraph) {
			return;
		}

		int lastDotIdx = pgraph.fname.lastIndexOf('.');

		// if (!PGraph.checkFrgVio) {
		// PGraph.graphPostFix = "_graphsNoFrg2.txt";
		// }

		String outPath = pgraph.fname.substring(0, lastDotIdx) + ConstantsTrans.graphPostFix;
		PrintStream op = null;
		try {
			op = new PrintStream(new File(outPath));
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}

		SpectralClustering specClusterer = null;
		if (ConstantsTrans.transMethod == TransitiveMethod.SpectralILPWithin) {
			// if (pgraph.nodes.size() > PGraph.specILPMaxClusterSizeAllowed) {
			int K = (int) Math.ceil((double) (pgraph.nodes.size()) / ConstantsTrans.specILPMaxClusterAllowed);
			System.out.println("num clusters: " + K + " num node: " + pgraph.nodes.size());
			specClusterer = new SpectralClustering(pgraph, K, ConstantsTrans.specILPMaxClusterSizeAllowed);
			specClusterer.cluster();
			System.out.println("clustering done");
			// }
		}

		for (float lmbda : lmbdas) {
			System.out.println("lambda: " + lmbda);

			// System.out.println("Berant's HTL started");
			if (ConstantsTrans.transMethod == TransitiveMethod.BerantTNF) {
				EOPTNF.formEntGraph(pgraph, lmbda, op);
			}
			// System.out.println("Berant's HTL finished");

			else if (ConstantsTrans.transMethod == TransitiveMethod.HTLFRG) {

				TransClUtils tnf = new TransClUtils(pgraph, op, lmbda, ConstantsTrans.checkFrgVio, null);
				if (ConstantsTrans.shouldWrite) {
					tnf.writeSCC();
				}

				double obj = TransClUtils.computeObj(tnf.scc, pgraph, lmbda);
				System.out.println("objective function: " + obj);

			}

			else if (ConstantsTrans.transMethod == TransitiveMethod.SpectralILP) {

				if (pgraph.nodes.size() > ConstantsTrans.specILPMaxClusterAllowed) {
					specClusterer = new SpectralClustering(pgraph, ConstantsTrans.specILPMaxClusterAllowed);
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
				System.out.println("done specILP " +pgraph.fname+ " "+lmbda);
			}

		}
		op.close();
		System.out.println("entGraph done: "+pgraph.fname);
	}

}
