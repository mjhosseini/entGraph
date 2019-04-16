package graph.trans;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import constants.ConstantsGraphs;
import constants.ConstantsTrans;
import graph.PGraph;

public class EntGraphBuilder {

	public static void main(String[] args) {
		PGraph.pGraphs = new ArrayList<>();
		List<PGraph> pGraphs = PGraph.pGraphs;
		System.err.println("start!");
		ConstantsTrans.setPGraphParams();
		BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.WARN);
		// String root = "../../python/gfiles/typedEntGrDir_aida/";
		// PGraph pgraph = new PGraph(root+"location#person_sim.txt");

		// TODO: be careful
		List<Float> lmbdas = EntGraphBuilder.getLambdas1();// (.08f);

		// List<Float> lmbdas = new ArrayList<>();
		// // lmbdas.add(.04f);
		// lmbdas.add(.12f);// was .06
		// lmbdas.add(.1f);
		// lmbdas.add(.2f);

		// List<Float> lmbdas = new ArrayList<>();
		// lmbdas.add(.04f);
		// lmbdas.add(.08f);
		// lmbdas.add(.12f);

		File folder = new File(ConstantsGraphs.root);
		File[] files = folder.listFiles();
		Arrays.sort(files);

		int gc = 0;
		for (File f : files) {
			String fname = f.getName();

//			if (gc == 50) {//TODO: be careful
//				break;
//			}

			// if (!fname.contains("thing#person")) {
			// continue;
			// }

			// if (!fname.contains("thing#location") &&
			// !fname.contains("location#location")) {
			// continue;
			// }

			// if (fname.startsWith("location#location_sim.txt")) {
			// seenLoc = true;
			// }
			// if (seenLoc) {
			// break;
			// }

			if (!fname.contains(ConstantsGraphs.suffix)) {
				continue;
			}

			System.out.println(fname);

			// if (gc++==50) {
			// break;
			// }
			String outPath = "";
			if (!ConstantsTrans.shouldReplaceOutputs) {
				String fname2 = ConstantsGraphs.root + fname;
				int lastDotIdx = fname2.lastIndexOf('.');
				outPath = fname2.substring(0, lastDotIdx) + ConstantsTrans.graphPostFix;
				System.out.println("out: " + outPath);
				File candF = new File(outPath);
				if (candF.exists() && candF.length() > 0) {
					continue;
				} else {
					System.out.println("not exist");

				}
			}

			System.out.println("accepted out:: " + outPath);

			System.out.println("fname: " + fname);

			PGraph pgraph = new PGraph(ConstantsGraphs.root + fname);
			if (pgraph.nodes.size() == 0) {
				continue;
			}

			pGraphs.add(pgraph);
			gc++;
			System.out.println("allEdgesRem, allEdges: " + PGraph.allEdgesRemained + " " + PGraph.allEdges);
		}
		
		System.out.println("allEdgesRem, allEdges: " + PGraph.allEdgesRemained + " " + PGraph.allEdges);
		
//		if (1==1) {
//			System.exit(0);//TODO: remove this
//		}
		
		PGraph.setRawPred2PGraphs(pGraphs);
		for (PGraph pgraph: pGraphs) {
			pgraph.setSortedEdges();
		}

		final BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(ConstantsTrans.numTransThreads);
		ThreadPoolExecutor threadPool = new ThreadPoolExecutor(ConstantsTrans.numTransThreads,
				ConstantsTrans.numTransThreads, 600, TimeUnit.HOURS, queue);
		// to silently discard rejected tasks. :add new
		// ThreadPoolExecutor.DiscardPolicy()

		threadPool.setRejectedExecutionHandler(new RejectedExecutionHandler() {
			@Override
			public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
				// this will block if the queue is full
				try {
					executor.getQueue().put(r);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});

		for (PGraph pgraph : pGraphs) {
			EntGraphBuilderRunner tnfR = new EntGraphBuilderRunner(pgraph, lmbdas);
			threadPool.execute(tnfR);
		}

		threadPool.shutdown();
		// Wait hopefully all threads are finished. If not, forget about it!
		try {
			threadPool.awaitTermination(200, TimeUnit.HOURS);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}

		

		// Collections.sort(scores, Collections.reverseOrder());
		// System.out.println("higest scoring relations:");
		// for (int i = 0; i < Math.min(1000000, scores.size()); i++) {
		// System.out.println(scores.get(i));
		// }

	}

	public static List<Float> getLambdas1() {
		List<Float> lmbdas = new ArrayList<>();
		float maxLmbda = .05f;
		int numLmbdas = 10;
		float minLambda = maxLmbda / numLmbdas;
		for (float lmbda = minLambda; lmbda <= maxLmbda; lmbda += (maxLmbda - minLambda) / (numLmbdas - 1)) {
			lmbdas.add(lmbda);
		}
		lmbdas.add(.06f);
		lmbdas.add(.1f);
		lmbdas.add(.2f);
		// lmbdas.remove(0);//TODO: remove this
		return lmbdas;
	}

	public static List<Float> getLambdas2() {
		List<Float> lmbdas = new ArrayList<>();
		// lmbdas.add(.025f);
		lmbdas.add(.03f);
		lmbdas.add(.04f);
		lmbdas.add(.05f);
		lmbdas.add(.06f);
		lmbdas.add(.1f);
		lmbdas.add(.2f);
		return lmbdas;
	}

	static List<Float> getLambdas3(float lmbda) {
		List<Float> lmbdas = new ArrayList<>();
		lmbdas.add(lmbda);
		return lmbdas;
	}

}
