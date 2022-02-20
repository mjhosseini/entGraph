package entailment.linkingTyping;

import java.io.BufferedReader;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.ibm.icu.util.StringTokenizer;

public class EntToWikiHandler {

	ThreadPoolExecutor threadPool;
	int numThreads = 20;
	static ArrayList<String> mainStrs = new ArrayList<String>();

	public EntToWikiHandler() {
		final BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(
				numThreads);
		threadPool = new ThreadPoolExecutor(numThreads, numThreads, 600,
				TimeUnit.SECONDS, queue);
		// to silently discard rejected tasks. :add new
		// ThreadPoolExecutor.DiscardPolicy()

		threadPool.setRejectedExecutionHandler(new RejectedExecutionHandler() {
			@Override
			public void rejectedExecution(Runnable r,
					ThreadPoolExecutor executor) {
				// this will block if the queue is full
				try {
					executor.getQueue().put(r);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	void runDexterOnSentences() throws IOException{
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("in2.txt")));
		String line;
		System.out.println("started");
		while ((line=br.readLine())!=null){
			Runnable extractor = new EntToWikiFinder(line,1);
			extractor.run();
		}
	}
	
	ArrayList<SimpleSpot> extractSSpots() throws IOException{
		BufferedReader br = new BufferedReader(new InputStreamReader(
				new FileInputStream("ents.txt")));
		String line;
		HashMap<String, Integer> allEnts = new HashMap<String, Integer>();
		while ((line = br.readLine()) != null) {
//			System.out.println("line: "+line);
			try {
				StringTokenizer st = new StringTokenizer(line);
				String spot = st.nextToken("::");
				int count = Integer.parseInt(st.nextToken());
				if (!allEnts.containsKey(spot)) {
					allEnts.put(spot, 0);
				}
				allEnts.replace(spot, allEnts.get(spot) + count);
			} catch (Exception e) {
				continue;
			}
			
		}
		System.err.println("num all entities: " + allEnts.size());
		br.close();

		ArrayList<SimpleSpot> sspots = new ArrayList<SimpleSpot>();
		for (String spot : allEnts.keySet()) {
			int count = allEnts.get(spot);
//			if (count==1){
//				continue;
//			}
			sspots.add(new SimpleSpot(spot, allEnts.get(spot)));
		}
		System.err.println("num all entities with more than one mention: " + sspots.size());
		return sspots;
	}

	void extractAllEntToWiki() throws IOException, InterruptedException {
		
		ArrayList<SimpleSpot> sspots = extractSSpots();
		
		Collections.sort(sspots,Collections.reverseOrder());
		int lineNumber = 0;
		for (SimpleSpot sspot : sspots) {
			String spot = sspot.spot;
			int count = (int) sspot.count;
			try {
				// System.err.println(lineNumber);
				if (lineNumber % 1000 == 0) {
					System.err.println(lineNumber);
				}

				Runnable extractor = new EntToWikiFinder(spot,count);
				threadPool.execute(extractor);

			} catch (Exception e) {
				System.err.println("Could not process spot: ");
				System.err.println(spot);
				// e.printStackTrace();
			}

			if (lineNumber % 10 == 0) {
				writeOutPut();
			}
			lineNumber++;
		}
		
		threadPool.shutdown();
		// Wait until all threads are finished
		while (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
			// pass.
		}
		writeOutPut();

	}

	private void writeOutPut() throws IOException {
		while (mainStrs.size() > 0) {
			String s = mainStrs.remove(0);
			System.out.println(s);
		}
	}

	public static void main(String[] args) throws IOException {
		long t0 = System.currentTimeMillis();
		EntToWikiHandler EWHandlder = new EntToWikiHandler();
//		try {
//			EWHandlder.extractAllEntToWiki();
//		} catch (IOException | InterruptedException e) {
//			e.printStackTrace();
//		}
		EWHandlder.runDexterOnSentences();
		System.out.println("time: "+ (System.currentTimeMillis()-t0));
	}

}
