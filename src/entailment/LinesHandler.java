package entailment;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import entailment.entityLinking.SimpleSpot;
import it.cnr.isti.hpc.dexter.rest.client.DexterRestClient;
import it.cnr.isti.hpc.dexter.rest.domain.CandidateSpot;
import it.cnr.isti.hpc.dexter.rest.domain.SpottedDocument;

public class LinesHandler {
	final int maxLinesToRun = 100000;// This is because of the memory leak in
										// easyCCG
	final int numThreads = 15;
	final int maxMBallowd = 14000;
	// final int maxMBallowd = 140;
	public static boolean convToEntityLinked = false;// Must be always false, we do linking separately!

	// static String[] accepteds = new String[] { "GE", "EG", "EE" };
	static String[] accepteds = new String[] { "GE", "EG", "EE"};//
	// TODO:// remove

	public static final boolean lemmatizePred = true;// eaten.might.1 => eat.might.1
	public static boolean useQuestionMod = false;// Always set if to false!
	public static boolean writeDebugString = false;
	public static boolean snli = false;//a few hacks for snli ds

	public static int nbestParses = 1;
	

	int numPortionsToSkip;
	BufferedReader br;
	int lineNumber;
	ThreadPoolExecutor threadPool;

	PrintStream opJson;
	BufferedWriter opEnts;
	BufferedWriter opMainStrs;
	BufferedWriter opMainStrsOnlyNEs;
	static ArrayList<String> mainStrs;
	static ArrayList<String> mainStrsOnlyNEs;
	static ArrayList<String> errStrs;

	static ArrayList<String> lines;
	static ArrayList<ArrayList<String>> spots;
	static ArrayList<ArrayList<String>> wikiNames;
	static Map<String, Integer> allEntities = new HashMap<>();
	static Map<String, Integer> allGens = new HashMap<>();
	final int lineOffset;

	public LinesHandler(String[] args) {
		mainStrs = new ArrayList<>();
		mainStrsOnlyNEs = new ArrayList<>();
		errStrs = new ArrayList<>();

		lineNumber = 0;
		if (args == null) {
			try {
				br = new BufferedReader(new InputStreamReader(System.in, "UTF8"));
				opMainStrs = new BufferedWriter(
						new OutputStreamWriter(new FileOutputStream("predArgs_gen.txt"), "UTF-8"));
				opMainStrsOnlyNEs = new BufferedWriter(
						new OutputStreamWriter(new FileOutputStream("predArgs_NEs.txt"), "UTF-8"));
			} catch (Exception e) {
				e.printStackTrace();
			}
			numPortionsToSkip = -1;
		} else {
			try {
				br = new BufferedReader(new InputStreamReader(new FileInputStream(args[0]), "UTF8"));
				String f1, f2;
				if (args.length > 2) {
					f1 = args[2];
					f2 = args[3];
					numPortionsToSkip = Integer.parseInt(args[1]);
				} else {
					f1 = "predArgs_gen.txt";
					f2 = "predArgs_NEs.txt";
					numPortionsToSkip = -1;
				}

				opMainStrs = new BufferedWriter(
						new OutputStreamWriter(new FileOutputStream(f1, numPortionsToSkip > 0), "UTF-8"));
				opMainStrsOnlyNEs = new BufferedWriter(
						new OutputStreamWriter(new FileOutputStream(f2, numPortionsToSkip > 0), "UTF-8"));
				if (convToEntityLinked) {
					int dotIdx = args[0].indexOf('.');
					String jsonName = args[0].substring(0, dotIdx) + ".json";
					opJson = new PrintStream(new File(jsonName));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		int ll = -1;
		try {
			Scanner sc = new Scanner(new File("offset.txt"));
			ll = sc.nextInt();
			sc.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		lineOffset = ll;

		System.err.println("numPortionsToSkip: " + numPortionsToSkip);

		try {
			if (numPortionsToSkip <= 0) {
				opEnts = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("ents.txt")));
			} else {
				opEnts = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("ents.txt", true)));
			}

		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		if (convToEntityLinked) {
			spots = new ArrayList<ArrayList<String>>();
			wikiNames = new ArrayList<ArrayList<String>>();
			lines = new ArrayList<String>();

		}

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

	}

	void extractAll() throws IOException, InterruptedException {
		int mb = 1024 * 1024;
		long time0 = System.currentTimeMillis();

		int lineNumber = 0;
		String line = null;

		lineNumber = -1;
		boolean memoryExceed = false;
		int numAskedForRun = 0;
		while ((line = br.readLine()) != null) {
			lineNumber++;
			try {
				// if (numPortionsToSkip != -1) {
				// if (lineNumber < lineOffset + numPortionsToSkip *
				// maxLinesToRun) {
				// continue;
				// } else if (lineNumber >= lineOffset + (numPortionsToSkip + 1)
				// * maxLinesToRun) {
				// break;
				// }
				if (lineNumber < lineOffset) {
					continue;
				}
				if (line.length() > 100000) {
					System.err.println("very long line, not processing: " + line);
				}
				if (lineNumber % 1000 == 0) {
					System.err.println(lineNumber);
				}

				// check memory and see if you wanna exit
				if (lineNumber % 1000 == 0) {
					long usedMb = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / mb;
					System.err.println("usedMB: " + usedMb);
					if (usedMb >= maxMBallowd) {
						memoryExceed = true;
						break;
					}
				}
				// }
				// if (lineNumber>0 && lineNumber%1000==0){
				// int nbestParses = 1;
				//
				// String ccgModelDir = Paths.get("lib_data",
				// "easyccg_model").toString();
				//
				// EasyCcgCli ccgParser = new EasyCcgCli(ccgModelDir,
				// nbestParses);
				// PredicateArgumentExtractor.parser.setCCGParser(ccgParser);
				//
				// }
				// line.startsWith("#")

				// System.err.println(lineNumber);

				if (line.trim().equals("")) {
					continue;
				}

				Runnable extractor = new PredicateArgumentExtractor(line);
				threadPool.execute(extractor);
				numAskedForRun++;
			} catch (Exception e) {
				System.err.println("Could not process line: ");
				System.err.println(line);
				// e.printStackTrace();
			}

			if (lineNumber % 10 == 0) {
				writeOutPut();
				if (convToEntityLinked) {
					writeConvertedToEntityLinked();
				}
			}
		}

		System.err.println("threadpool: " + threadPool.getActiveCount() + " " + threadPool.getPoolSize() + " "
				+ threadPool.getQueue().size());
		System.err.println("asked for run: " + numAskedForRun);
		System.err.println("num done: " + threadPool.getCompletedTaskCount());
		threadPool.shutdown();
		// Wait hopefully all threads are finished. If not, forget about it!
		threadPool.awaitTermination(200, TimeUnit.SECONDS);

		System.err.println("after await");
		writeOutPut();
		System.err.println("after write output");
		writeEnts();
		writeGens();
		// System.err.println("after write enty");
		if (convToEntityLinked) {
			writeConvertedToEntityLinked();
		}

		System.err.println("all time: " + (System.currentTimeMillis() - time0));

		// See if memory has exceeded and we should run for continue!
		if (memoryExceed) {
			// write the current lineNumber
			PrintStream op = new PrintStream(new File("offset.txt"));
			op.println(lineNumber);
			op.close();
		} else {// delete offset.txt as everything is done :)
			File f = new File("offset.txt");
			f.delete();
		}

		System.exit(0);
	}

	private void writeEnts() {
		System.err.println("in write ents: " + allEntities.size());
		for (String s : allEntities.keySet()) {
			try {
				opEnts.write(s + "::" + allEntities.get(s) + "\n");
				// System.err.println("now writing " + s + "::"
				// + allEntities.get(s) + "\n");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		try {
			opEnts.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void writeGens() throws FileNotFoundException {
		System.err.println("in write gens: " + allGens.size());

		PrintWriter pr = new PrintWriter("gens.txt");

		ArrayList<SimpleSpot> sspots = new ArrayList<>();

		for (String s : allGens.keySet()) {
			sspots.add(new SimpleSpot(s, allGens.get(s)));
		}

		Collections.sort(sspots, Collections.reverseOrder());

		for (SimpleSpot ss : sspots) {
			pr.println(ss.spot + "::" + ss.count);
		}

		pr.close();

	}

	private void writeConvertedToEntityLinked() {
		while (spots.size() > 0) {
			ArrayList<String> thisSpots = spots.remove(0);
			ArrayList<String> thisWikiNames = wikiNames.remove(0);
			String line = lines.remove(0);

			JSONObject jObj = new JSONObject();
			jObj.put("s", line);
			JSONArray jsonArray = new JSONArray();
			for (int i = 0; i < thisSpots.size(); i++) {
				JSONObject candObj = new JSONObject();
				candObj.put(thisSpots.get(i), thisWikiNames.get(i));
				jsonArray.add(i, candObj);
			}
			jObj.put("a", jsonArray);
			opJson.println(jObj);
		}
	}

	private void writeOutPut() throws IOException {
		while (mainStrs.size() > 0) {
			String s = mainStrs.remove(0);
			opMainStrs.write(s + "\n");
		}
		while (mainStrsOnlyNEs.size() > 0) {
			String s = mainStrsOnlyNEs.remove(0);
			opMainStrsOnlyNEs.write(s + "\n");
		}
		while (errStrs.size() > 0) {
			String s = errStrs.remove(0);
			// System.err.println(s);
		}
	}

	// String processNext() {
	// if (!scMain.hasNext()) {
	// return null;
	// }
	//
	// String line;
	// try {
	// while (scMain.hasNext()) {
	// line = scMain.nextLine();
	// lineNumber++;
	//
	// if (line == null || line.trim().equals("")
	// || line.charAt(0) == '#') {
	// if (!scMain.hasNext()) {
	// return null;
	// }
	// continue;
	// }
	//
	// if (lineNumber % 1000 == 0) {
	// System.err.println("################## " + lineNumber);
	// }
	// return line;
	// }
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	//
	// return null;
	// }

	static void breakFile() {
		String root = "";
		Scanner sc = null;
		PrintStream op1 = null;
		PrintStream op2 = null;
		try {
			sc = new Scanner(new File(root + "news.txt"));
			op1 = new PrintStream(new File("news1.txt"));
			op2 = new PrintStream(new File("news2.txt"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		int lineNumber = 0;
		while (sc.hasNext()) {
			String l = sc.nextLine();
			if (lineNumber < 6000000) {
				op1.println(l);
			} else {
				op2.println(l);
			}
			lineNumber++;
		}
	}

	static void testDexter() {
		DexterRestClient client = null;
		try {
			client = new DexterRestClient("http://localhost:8080/dexter-webapp/api/rest");
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		// AnnotatedDocument ad = client
		// .annotate(str);
		// System.out.println(ad);
		Scanner sc = null;
		try {
			sc = new Scanner(new File("../graph-parser/in.txt"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		long t0 = System.currentTimeMillis();
		while (sc.hasNext()) {
			String line = sc.nextLine();
			System.out.println("#line: " + line);
			SpottedDocument sd = client.spot(line);
			System.out.println(sd);
		}
		System.out.println("time: " + (System.currentTimeMillis() - t0));
	}

	static void convertToLinkedEntityAnnotated(String[] args) {
		if (args == null || args.length == 0) {
			args = new String[] { "in.txt" };
		}
		Scanner sc = null;
		DexterRestClient client = null;
		try {
			client = new DexterRestClient("http://localhost:8080/dexter-webapp/api/rest");
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		try {
			sc = new Scanner(new File(args[0]));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		while (sc.hasNext()) {
			String str = sc.nextLine();
			// Let's make the JSON
			JSONObject jObj = new JSONObject();
			jObj.put("s", str);

			try {
				SpottedDocument sd = client.spot(str);
				List<CandidateSpot> spots = sd.getSpots();
				ArrayList<String> spotStr = new ArrayList<>();
				ArrayList<Integer> spotEnt = new ArrayList<>();
				for (CandidateSpot spot : spots) {
					String mention = spot.getMention();
					if (spot.getCandidates().size() == 0) {
						continue;
					}
					int entityNumber = spot.getCandidates().get(0).getEntity();
					spotStr.add(mention);
					spotEnt.add(entityNumber);
				}
				JSONArray jsonArray = new JSONArray();
				for (int i = 0; i < spotStr.size(); i++) {
					JSONObject candObj = new JSONObject();
					candObj.put(spotStr.get(i), spotEnt.get(i));
					jsonArray.add(i, candObj);
				}
				jObj.put("a", jsonArray);
			} catch (Exception e) {
				JSONArray jsonArray = new JSONArray();
				jObj.put("a", jsonArray);
				e.printStackTrace();
			}
			System.out.println(jObj);
		}
	}

	// static HashMap<String, Integer> h = new HashMap<String, Integer>();
	// static BufferedWriter br2;
	//
	// static void teshashMap() throws IOException {
	// br2 = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(
	// "htest.txt")));
	// int numAllObjs = 10000000;
	// for (int i = 0; i < 100000000; i++) {
	// int objNum = i % numAllObjs;
	// if (!h.containsKey(objNum + "")) {
	// h.put(objNum + "", 0);
	// }
	// h.replace(objNum + "", h.get(objNum + "") + 1);
	// if (i % 100000 == 0) {
	// System.err.println(i);
	// // HashMap<String, Integer> currentMap = h;
	// // h = new HashMap<String, Integer>();
	// // for (String s : currentMap.keySet()) {
	// // br2.write(s + "::" + currentMap.get(s) + "\n");
	// // }
	// }
	//
	// }
	// }

	public static void main(String[] args) throws IOException, InterruptedException {
		// breakFile();
		// testDexter();
		LinesHandler.useQuestionMod = false;
		long t0 = System.currentTimeMillis();
		// convertPredArgsToJson(args);
		// teshashMap();
		// convertToLinkedEntityAnnotated(args);
		if (args.length == 0) {
			args = new String[] { "news_raw.json" };
		}
		System.err.println("args:" + args[0]);
		LinesHandler lineHandler = new LinesHandler(args);
		lineHandler.extractAll();
		System.err.println("time: " + (System.currentTimeMillis() - t0));
	}
}
