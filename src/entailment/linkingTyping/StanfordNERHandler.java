package entailment.linkingTyping;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class StanfordNERHandler {
	public static Map<Integer, Map<String, String>> lineIdToStanTypes = new ConcurrentHashMap<>();
	public static int numThreads = 60;
	public static String fname = "news_genC_GG.json";
	public static String NERAddress = "data/stan_NER/news_genC_stanNER.json";
	static int maxLineId = 0;
	
	public static void loadNER(String NERAddress) throws IOException {
		lineIdToStanTypes = new ConcurrentHashMap<>();
		BufferedReader br = new BufferedReader(new FileReader(NERAddress));
		String line = null;
		JsonParser parser = new JsonParser();
		while ((line=br.readLine())!=null) {
			JsonObject jobj = parser.parse(line).getAsJsonObject();
			int lineId = jobj.get("lineId").getAsInt();
			Map<String,String> token2type = new HashMap<>();
			JsonArray jar = jobj.get("et").getAsJsonArray();
			for (int i=0; i<jar.size(); i++) {
				JsonObject et = jar.get(i).getAsJsonObject();
				String e = et.get("e").getAsString();
				String t = et.get("t").getAsString();
				token2type.put(e, t);
			}
			lineIdToStanTypes.put(lineId, token2type);
		}
		br.close();
	}
	
	public static void performNER(PrintStream op) {
		
		final BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(numThreads);
		ThreadPoolExecutor threadPool = new ThreadPoolExecutor(numThreads, numThreads, 600,
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

		for (int threadIdx = 0; threadIdx < numThreads; threadIdx++) {

			StanfordNERRunner nerHandler = new StanfordNERRunner(threadIdx, fname, op);
			threadPool.execute(nerHandler);
		}

		threadPool.shutdown();
		// Wait hopefully all threads are finished. If not, forget about it!
		try {
			threadPool.awaitTermination(200, TimeUnit.HOURS);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
	}
	
	
	//run this function to get data/stan_NER/news_genC_stanNER.json. Set the parameters above
	//java -Xmx100G -cp lib/*:bin entailment.linkingTyping.StanfordNERHandler 1>out_stan_ner.txt 2>&1 &
	
	public static void main(String[] args) throws FileNotFoundException {
		PrintStream op = new PrintStream(new File(NERAddress));
		performNER(op);
		
		//flush!
		synchronized (StanfordNERHandler.lineIdToStanTypes) {
			if (StanfordNERHandler.lineIdToStanTypes.size() > 0) {
//				System.err.println("writing stuff" + currentOuts.size());
				while (StanfordNERHandler.lineIdToStanTypes.size() > 0) {
					for (int this_lineId: StanfordNERHandler.lineIdToStanTypes.keySet()) {
						JsonObject this_jObj = getJsonObject(this_lineId);
						op.println(this_jObj);
						StanfordNERHandler.lineIdToStanTypes.remove(this_lineId);
					}
					op.flush();
				}
				
			}
		}
		
//		for (int i=0; i<maxLineId; i++) {
//			JsonObject jObj = new JsonObject();
//			jObj.addProperty("lineId", i);
//			
//			JsonArray tokenToTypeArr = new JsonArray();
//			Map<String, String> token2Type = lineIdToStanTypes.get(i);
//			
//			if (token2Type==null) {
//				continue;
//			}
//			
//			for (String tok: token2Type.keySet()) {
//				String type = token2Type.get(tok);
//				JsonObject tt = new JsonObject();
//				tt.addProperty("e", tok);
//				tt.addProperty("t", type);
//				tokenToTypeArr.add(tt);
//			}
//			jObj.add("et", tokenToTypeArr);
//			op.println(jObj);
//		}
//		op.close();
	}

	static JsonObject getJsonObject(int lineId){
		JsonObject jObj = new JsonObject();
		jObj.addProperty("lineId", lineId);
		
		JsonArray tokenToTypeArr = new JsonArray();
		Map<String, String> token2Type = lineIdToStanTypes.get(lineId);
		
		if (token2Type==null) {
			return null;
		}
		
		for (String tok: token2Type.keySet()) {
			String type = token2Type.get(tok);
			JsonObject tt = new JsonObject();
			tt.addProperty("e", tok);
			tt.addProperty("t", type);
			tokenToTypeArr.add(tt);
		}
		jObj.add("et", tokenToTypeArr);
		return jObj;
	}
	
}
