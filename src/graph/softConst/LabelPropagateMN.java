package graph.softConst;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;

import constants.ConstantsSoftConst;
import graph.PGraph;
import graph.SimpleScore;

public class LabelPropagateMN implements Runnable {

	List<PGraph> allpGraphs;
	List<PGraph> thispGraphs;
	int runIdx;// 0: propagate, 1: average
	int threadIdx;

	public LabelPropagateMN(List<PGraph> allpGraphs, int threadIdx, int numThreads, int runIdx) {
		if (runIdx == 0) {
			this.allpGraphs = allpGraphs;
		}
		thispGraphs = new ArrayList<>();
		for (int i = 0; i < allpGraphs.size(); i++) {
			if (i % numThreads == threadIdx) {
				thispGraphs.add(allpGraphs.get(i));
			}
		}
		this.runIdx = runIdx;
		this.threadIdx = threadIdx;
	}

	static long numOperations = 0;
	static int numPassedEdges = 0;

	// label propagate inside a graph!
	void propagateLabelWithinGraphs() {

		for (PGraph pgraph : thispGraphs) {
			System.out.println("mn prop within graphs: " + pgraph.fname + " " + threadIdx);

			DefaultDirectedWeightedGraph<Integer, DefaultWeightedEdge> gPrev = pgraph.g0;
			double[] reflexSumWeights = new double[gPrev.vertexSet().size()];

			// Parallelize inside a graph (we only parallelize for large graphs!)
			int sortIdx = pgraph.sortIdx;
			int numThreadsWithinGraph = (sortIdx < 10) ? 10 : (sortIdx < 15 ? 5 : 1);
			System.out.println("numThreadsWithingGraph: " + pgraph.name + " " + numThreadsWithinGraph);

			final BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(numThreadsWithinGraph);
			ThreadPoolExecutor threadPool = new ThreadPoolExecutor(numThreadsWithinGraph, numThreadsWithinGraph, 600,
					TimeUnit.SECONDS, queue);
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

			// int batchSize = pgraph.g0.vertexSet().size() / numThreadsWithinGraph;
			for (int threadIdx = 0; threadIdx < numThreadsWithinGraph; threadIdx++) {
				// int start = threadIdx * batchSize;
				// int end = (threadIdx == numThreadsWithinGraph - 1) ?
				// pgraph.g0.vertexSet().size()
				// : ((threadIdx + 1) * batchSize);

				LabelPropagationMNWithinGraph lpmwg = new LabelPropagationMNWithinGraph(threadIdx,
						numThreadsWithinGraph, pgraph, reflexSumWeights);

				threadPool.execute(lpmwg);
			}

			threadPool.shutdown();
			// Wait hopefully all threads are finished. If not, forget about it!
			try {
				threadPool.awaitTermination(200, TimeUnit.HOURS);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}

			// Now, handle the rest of the denoms for 1 and 2! We saved it until here,
			// because we didn't know which numerators are zero
			// until now
			for (int i = 0; i < pgraph.gMN.vertexSet().size(); i++) {
				if (i % 100 == 0) {
					System.out.println("1,2 k: " + i + " " + pgraph.name);
				}
				for (DefaultWeightedEdge e : pgraph.gMN.outgoingEdgesOf(i)) {
					int j = gPrev.getEdgeTarget(e);
					double denom = reflexSumWeights[i] + reflexSumWeights[j];
					// if (denom == 0 && nzijs.contains(i + "#" + j)) {
					// System.err.println("here denom 0" + i + " " + j);
					// }

					LabelPropagationMNWithinGraph.addNumeratorDenom(pgraph, i, j, 0, denom);
				}
			}

			System.out.println("done within graphs: " + pgraph.fname + " " + threadIdx);
			System.out.println("num operations: " + numOperations);

		}
	}

	// label propagate inside a graph!
	void propagateLabelWithinGraphsTrans() {

		for (PGraph pgraph : thispGraphs) {
			System.out.println("mn prop within graphs trans: " + pgraph.fname + " " + threadIdx);
			DefaultDirectedWeightedGraph<Integer, DefaultWeightedEdge> gPrev = pgraph.g0;

			// Parallelize inside a graph (we only parallelize for large graphs!)
			int sortIdx = pgraph.sortIdx;
			int numThreadsWithinGraph = (sortIdx < 10) ? 10 : (sortIdx < 15 ? 5 : 1);
//			int numThreadsWithinGraph = 1;
			System.out.println("numThreadsWithingGraph trans: " + pgraph.name + " " + numThreadsWithinGraph);

			final BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(numThreadsWithinGraph);
			ThreadPoolExecutor threadPool = new ThreadPoolExecutor(numThreadsWithinGraph, numThreadsWithinGraph, 600,
					TimeUnit.SECONDS, queue);
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

			// int batchSize = pgraph.g0.vertexSet().size() / numThreadsWithinGraph;
			for (int threadIdx = 0; threadIdx < numThreadsWithinGraph; threadIdx++) {
				// int start = threadIdx * batchSize;
				// int end = (threadIdx == numThreadsWithinGraph - 1) ?
				// pgraph.g0.vertexSet().size()
				// : ((threadIdx + 1) * batchSize);

				LabelPropagationMNWithinGraphTrans lpmwg = new LabelPropagationMNWithinGraphTrans(threadIdx,
						numThreadsWithinGraph, pgraph);

				threadPool.execute(lpmwg);
			}

			threadPool.shutdown();
			// Wait hopefully all threads are finished. If not, forget about it!
			try {
				threadPool.awaitTermination(200, TimeUnit.HOURS);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}

			// Now, add 1 to denom for the edges that are newly affected by transitivity 
			// because we didn't know which numerators are zero
			// until now
			for (int i = 0; i < pgraph.gMN.vertexSet().size(); i++) {
				if (i % 100 == 0) {
					System.out.println("1,2 k: " + i + " " + pgraph.name);
				}
				for (DefaultWeightedEdge e : pgraph.gMN.outgoingEdgesOf(i)) {
					int j = gPrev.getEdgeTarget(e);
					
//					double denom = pgraph.edgeToMNWeight.get(p + "#" + q);

					LabelPropagationMNWithinGraph.addDenomNewEdge(pgraph, i, j);//TODO: maybe we can do more efficient here
				}
			}

			System.out.println("done within graphs trans: " + pgraph.fname + " " + threadIdx);
			System.out.println("num operations: " + numOperations);

		}
	}

	// typePropagate
	void propagateLabelBetweenGraphs() {

		// r => rp is used to update p=>q. propagate similarities of pgraph to all its
		// neighbors
		for (PGraph pgraph : thispGraphs) {
			System.out.println("MN prop between graphs: " + pgraph.fname + " " + threadIdx);

			// Parallelize inside a graph (we only parallelize for large graphs!)
			int sortIdx = pgraph.sortIdx;
			int numThreadsBetweenGraph = (sortIdx < 10) ? 10 : (sortIdx < 15 ? 5 : 1);
			// int numThreadsBetweenGraph = 1;
			System.out.println("numThreadsBetweenGraph: " + pgraph.name + " " + numThreadsBetweenGraph);

			final BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(numThreadsBetweenGraph);
			ThreadPoolExecutor threadPool = new ThreadPoolExecutor(numThreadsBetweenGraph, numThreadsBetweenGraph, 600,
					TimeUnit.SECONDS, queue);
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

			// int batchSize = pgraph.g0.vertexSet().size() / numThreadsWithinGraph;
			for (int threadIdx = 0; threadIdx < numThreadsBetweenGraph; threadIdx++) {
				// int start = threadIdx * batchSize;
				// int end = (threadIdx == numThreadsWithinGraph - 1) ?
				// pgraph.g0.vertexSet().size()
				// : ((threadIdx + 1) * batchSize);

				LabelPropagationBetweenGraphs lpbg = new LabelPropagationBetweenGraphs(threadIdx,
						numThreadsBetweenGraph, pgraph, allpGraphs);

				threadPool.execute(lpbg);
			}

			threadPool.shutdown();
			// Wait hopefully all threads are finished. If not, forget about it!
			try {
				threadPool.awaitTermination(200, TimeUnit.HOURS);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}

			// pgraph.g0 = null;//TODO: make this null? need it for distance
			System.out.println("all prop edges: " + TypePropagateMN.allPropEdges);
		}
	}

	// private static String removeUnderlines(String t) {
	// return t.replace("_1", "").replace("_2", "");
	// }

	void writeTPropResults(PGraph pgraph, List<DefaultDirectedWeightedGraph<Integer, DefaultWeightedEdge>> gs,
			String fnameTProp) {
		// list of all predicates can be found from the last graph. The indexes
		// are also the same (if existing) with previous graphs
		PrintStream op = null;
		try {
			op = new PrintStream(new File(fnameTProp));
		} catch (Exception e) {
			e.printStackTrace();
		}
		// List<String> predList = allPredsList.get(allPredsList.size() - 1);

		int N = pgraph.idx2node.size();
		op.println(pgraph.name + " " + " type propagation num preds: " + N);

		DefaultDirectedWeightedGraph<Integer, DefaultWeightedEdge> lastG = gs.get(gs.size() - 1);
		for (int i = 0; i < N; i++) {
			String pred = pgraph.idx2node.get(i).id;
			op.println("predicate: " + pred);
			op.println("num max neighbors: " + lastG.outgoingEdgesOf(i).size());
			op.println();
			for (int iter = 0; iter < gs.size(); iter++) {
				DefaultDirectedWeightedGraph<Integer, DefaultWeightedEdge> thisG = gs.get(iter);
				op.println("iter " + iter + " sims");
				List<SimpleScore> scores = new ArrayList<>();
				if (thisG.containsVertex(i)) {
					for (DefaultWeightedEdge e : thisG.outgoingEdgesOf(i)) {
						int j = thisG.getEdgeTarget(e);
						String pred2 = pgraph.idx2node.get(j).id;
						double w = thisG.getEdgeWeight(e);
						scores.add(new SimpleScore("", pred2, (float) w));
					}

					Collections.sort(scores, Collections.reverseOrder());
					for (SimpleScore sc : scores) {
						op.println(sc.pred2 + " " + sc.score);
					}
				}
				op.println();
			}
		}
		op.close();
	}

	void getAvg() {

		for (PGraph pgraph : thispGraphs) {
			// Get the average
			DefaultDirectedWeightedGraph<Integer, DefaultWeightedEdge> gMN = pgraph.gMN;
			int curN = gMN.vertexSet().size();
			List<DefaultWeightedEdge> removableEdges = new ArrayList<>();
			for (int p = 0; p < curN; p++) {
				for (DefaultWeightedEdge e : gMN.outgoingEdgesOf(p)) {
					int q = gMN.getEdgeTarget(e);
					if (ConstantsSoftConst.forceSelfEdgeOne && p == q) {
						gMN.setEdgeWeight(e, 1);
					} else {
						double c = gMN.getEdgeWeight(e);

						// if (c<0) {
						// System.err.println("neg c: "+c);
						// }

						// System.out.println("c: "+c+" "+p+" "+q);

						if (c <= 0 || (c <= ConstantsSoftConst.lmbda && c >= -ConstantsSoftConst.lmbda)) {
							removableEdges.add(e);
							continue;
						} else {
							// if (!pgraph.edgeToMNWeight.containsKey(p + "#" + q)) {
							// System.err.println("doesn't have: " + p + "#" + q + " " + c + " " +
							// pgraph.name);
							// }
							double denom = pgraph.edgeToMNWeight.get(p + "#" + q);
							double w;
							if (c > 0) {
								w = (c - ConstantsSoftConst.lmbda) / denom;
							} else {// this never happens as everything in c is pos, and so is c!
								w = (c + ConstantsSoftConst.lmbda) / denom;
							}

							// System.out.println(
							// "avg: " + pgraph.idx2node.get(p).id + " " + pgraph.idx2node.get(q).id + " ");
							// System.out.println("avg: " + w + " " + gMN.getEdgeWeight(e) + " " + denom);
							if (ConstantsSoftConst.lmbda3 == 0) {
								if (w > 1.01) {
									System.out.println("bug: " + w + " " + gMN.getEdgeWeight(e) + " " + denom);
									System.out.println(pgraph.nodes.get(p).id + " " + pgraph.nodes.get(q).id + " ");
								}
							} else {
								w = Math.min(1, Math.max(w, 0));
							}
							gMN.setEdgeWeight(e, w);

							double w0 = 0;
							if (pgraph.g0.containsEdge(p, q)) {
								DefaultWeightedEdge e0 = pgraph.g0.getEdge(p, q);
								w0 = pgraph.g0.getEdgeWeight(e0);
							}

							TypePropagateMN.objChange += Math.pow(w - w0, 2);

						}

						//
					}
				}
			}

			gMN.removeAllEdges(removableEdges);// TODO: you can do better here, by changing the order of the stuff

			// now, g0 is null, gMN is the next one

		}

	}

	// write the output of all of thispGraphs
	void writeResults() {
		while (thispGraphs.size() > 0) {
			PGraph pgraph = thispGraphs.remove(0);

			List<DefaultDirectedWeightedGraph<Integer, DefaultWeightedEdge>> gs = new ArrayList<>();
			pgraph.g0 = pgraph.formWeightedGraph(pgraph.sortedEdges, pgraph.nodes.size());
			gs.add(pgraph.g0);
			gs.add(pgraph.gMN);

			String fnameTProp = pgraph.fname.substring(0, pgraph.fname.lastIndexOf('_'))
					+ ConstantsSoftConst.tPropSuffix;
			writeTPropResults(pgraph, gs, fnameTProp);
			System.out.println("results written for: " + fnameTProp);
		}
	}

	@Override
	public void run() {
		if (runIdx == 0) {
			propagateLabelBetweenGraphs();
			System.out.println("between prop done!");
			allpGraphs = null;
		} else if (runIdx == 1) {
			LabelPropagationMNWithinGraphTrans.numVio = 0;
			propagateLabelWithinGraphsTrans();
//			ConstantsSoftConst.lmbda3 *= .9;
			System.out.println("within prop trans done!");
		} else if (runIdx == 2) {
			propagateLabelWithinGraphs();
			System.out.println("within prop done!");
			System.out.println("thread Idx +" + threadIdx + " done");
		} else if (runIdx == 3) {
			getAvg();
		} else if (runIdx == 4) {
			writeResults();
		}
	}

}
