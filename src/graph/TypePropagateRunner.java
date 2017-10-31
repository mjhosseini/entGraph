package graph;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TypePropagateRunner {
	ThreadPoolExecutor threadPool;
	ArrayList<PGraph> pGraphs;
	public final static float edgeThreshold = .01f;// edgeThreshold
	static int numThreads = 15;

	public TypePropagateRunner(String root) {
		PGraph.emb = false;
		PGraph.suffix = "_sim.txt";
		PGraph.transitive = false;
		PGraph.threshold = edgeThreshold;
		pGraphs = new ArrayList<>();

		File folder = new File(root);
		File[] files = folder.listFiles();
		Arrays.sort(files);

		// boolean seenLoc = false;//TODO: be carful
		int gc = 0;
		for (File f : files) {
			
			String fname = f.getName();
			// if (fname.startsWith("location#title_sim.txt")) {
			// seenLoc = true;
			// }
			// if (!seenLoc) {
			// continue;
			// }

			if (!fname.contains(PGraph.suffix)) {
				continue;
			}

//			if (gc++>20){
//				break;
//			}
			
			System.out.println("fname: " + fname);
			PGraph pgraph = new PGraph(root + fname);
			if (pgraph.nodes.size()==0){
				continue;
			}
			pgraph.g0 = pgraph.formWeightedGraph(pgraph.sortedEdges, pgraph.nodes.size());
			if (pgraph.nodes.size() == 0) {
				continue;
			}

			pGraphs.add(pgraph);

			System.out.println("allEdgesRem, allEdges: " + PGraph.allEdgesRemained + " " + PGraph.allEdges);
		}
	}

	void runAll() throws InterruptedException{
		
		final BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(numThreads);
		threadPool = new ThreadPoolExecutor(numThreads, numThreads, 600, TimeUnit.SECONDS, queue);
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
		
		Map<String, Integer> candidateEdges = new ConcurrentHashMap();
		Map<String, Integer> matchedEdges = new ConcurrentHashMap();
		
		for (int threadIdx = 0; threadIdx<numThreads; threadIdx++){
			TypePropagate tpr = new TypePropagate(pGraphs, threadIdx, candidateEdges, matchedEdges);
			threadPool.execute(tpr);
		}
		
		threadPool.shutdown();
		// Wait hopefully all threads are finished. If not, forget about it!
		threadPool.awaitTermination(200, TimeUnit.HOURS);
		
		System.out.println("compatibles:");

		for (String propStr : candidateEdges.keySet()) {
			System.out.println(propStr + " " + matchedEdges.get(propStr) + " " + candidateEdges.get(propStr));
		}
		
		
	}
	
	public static void main(String[] args) throws InterruptedException {
		String root = "../../python/gfiles/typedEntGrDir_aida_figer_3_3_b/";
		TypePropagateRunner tprRunner = new TypePropagateRunner(root);
		tprRunner.runAll();
	}

}
