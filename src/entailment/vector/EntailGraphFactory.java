package entailment.vector;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import entailment.Util;
import entailment.entityLinking.DistrTyping;
import entailment.vector.EntailGraphFactoryAggregator.TypeScheme;

public class EntailGraphFactory implements Runnable {
	String fName, entTypesFName;
	HashMap<String, EntailGraph> typesToGraph = new HashMap<>();
	// HashMap<String, EntailGraph> typesToGraphX = new HashMap<>();
	// HashMap<String, EntailGraph> typesToGraphY = new HashMap<>();
	// HashMap<String, EntailGraph> typesToGraphUnaryX = new HashMap<>();
	// HashMap<String, EntailGraph> typesToGraphUnaryY = new HashMap<>();
	// static HashMap<String, SimpleEntailGraph> typesToSimpleGraphUnaryX = new
	// HashMap<>();
	// static HashMap<String, SimpleEntailGraph> typesToSimpleGraphUnaryY = new
	// HashMap<>();
	HashSet<String> similarityFileTypes = new HashSet<>();

	// TODO: remove
	static PrintStream typedOp;
	static ConcurrentHashMap<String, Integer> allPredCounts = new ConcurrentHashMap<>();
	static ConcurrentHashMap<String, String> predToDocument = new ConcurrentHashMap<>();
	static ConcurrentHashMap<Integer, Map<String, String>> lineIdToStanTypes = new ConcurrentHashMap<>();
	static List<Integer> lineIdSeen = Collections.synchronizedList(new ArrayList<>());// assuming we don't have more
																						// than 20m lines. NS has 11m
																						// lines

	static {
		try {

			for (int i = 0; i < 20000000; i++) {
				lineIdSeen.add(0);
			}

			typedOp = new PrintStream(new File("typedOP.txt"), "UTF-8");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	// For knowing the ordering: type1, type2 => type1+type2. type2, type1 =>
	// type1 + type2
	HashMap<String, String> typeToOrderedType = new HashMap<>();
	String typedEntGrDir;
	HashSet<String> acceptableTypes = new HashSet<>();
	static HashSet<String> acceptablePredPairs = new HashSet<>();
	// static boolean entGenTypesLoaded = false;

	int threadNum;

	int runPart = -1;

	long startTime = System.currentTimeMillis();

	public EntailGraphFactory(String typedEntGrDir) {
		this.typedEntGrDir = typedEntGrDir;
	}

	public EntailGraphFactory(String fName, String entTypesFName, String genTypesFName, String typedEntGrDir) {
		this.fName = fName;
		this.entTypesFName = entTypesFName;
		this.typedEntGrDir = typedEntGrDir;

		// if (!entGenTypesLoaded) {
		// Util.loadEntGenTypes(entTypesFName, genTypesFName);
		// entGenTypesLoaded = true;
		// }

	}

	@Override
	public void run() {
		try {
			if (runPart == 0) {
				// build the graphs
				buildGraphs();
				// process all the entailmentGraphs
				processAllEntGraphsBinary();
			}
			// else if (runPart == 1) {
			// processAllEntGraphsUnary();
			// } else if (runPart==2){
			// for (String types:similarityFileTypes){
			// writeSimilaritiesUnary(types);
			// }
			// }
			else {
				System.out.println("wrong runPart");
				System.exit(0);
			}

		} catch (JsonSyntaxException | IOException e) {
			e.printStackTrace();
		}

	}

	void buildGraphs() throws JsonSyntaxException, IOException {

		// BufferedReader br = new BufferedReader(new FileReader(fName));
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fName), "UTF-8"));

		int lineNumbers = 0;
		JsonParser jsonParser = new JsonParser();

		// long t0;
		// long sharedTime = 0;

		String line;
		while ((line = br.readLine()) != null) {
			// if (lineNumbers==100000){
			// break;
			// }
			if (line.startsWith("exception for") || line.contains("nlp.pipeline")) {
				continue;
			}
			try {
				int lineId = -1;
				ArrayList<String> relStrs = new ArrayList<>();
				ArrayList<Integer> counts = new ArrayList<>();
				if (!EntailGraphFactoryAggregator.rawExtractions) {
					JsonObject jObj = jsonParser.parse(line).getAsJsonObject();
					lineId = jObj.get("lineId").getAsInt();

					if (EntailGraphFactoryAggregator.backupToStanNER) {
						lineIdSeen.set(lineId, lineIdSeen.get(lineId) + 1);
						if (lineIdSeen.get(lineId) == EntailGraphFactoryAggregator.numThreads) {
							lineIdToStanTypes.remove(lineId);
						}
					}

					String mainLine = jObj.get("s").getAsString();
					typedOp.println("line: " + mainLine);
					JsonArray jar = jObj.get("rels").getAsJsonArray();
					for (int i = 0; i < jar.size(); i++) {
						JsonObject relObj = jar.get(i).getAsJsonObject();
						String relStr = relObj.get("r").getAsString();
						relStrs.add(relStr);
						counts.add(1);
					}
				} else {
					String[] ss = line.split("\t");
					String relStr = "(" + ss[2] + "::" + ss[0] + "::" + ss[1] + "::EE)";
					relStrs.add(relStr);
					counts.add(Integer.parseInt(ss[3]));
				}

				// let's see if we have NER ed the line, otherwise, do it
				if (EntailGraphFactoryAggregator.backupToStanNER && EntailGraphFactoryAggregator.figerTypes && lineIdSeen.get(lineId) == 1) {
					// System.err.println("lid: "+lineId+" "+lineIdSeen.get(lineId)+" "+threadNum);
					Map<String, String> tokenToType = Util.getSimpleNERTypeSent(line);
					lineIdToStanTypes.put(lineId, tokenToType);
				}

				// t0 = 0;

				for (int i = 0; i < relStrs.size(); i++) {
					// if (lineNumbers%100==0){
					// t0 = System.currentTimeMillis();
					// }

					String relStr = relStrs.get(i);
					int count = counts.get(i);
					String timeInterval = null;
					if (EntailGraphFactoryAggregator.useTimeEx) {
						// System.out.println("relStr: "+relStr);
						String[] ss = relStr.split("\\)::\\[");// TODO: fix this
						timeInterval = "[" + ss[1];
						relStr = ss[0] + ")";
						// System.out.println("now relStr: "+relStr);
					}
					relStr = relStr.substring(1, relStr.length() - 1);
					String[] parts = relStr.split("::");
					String pred = parts[0];
					// System.out.println("now pred: "+pred);

					if (!Util.acceptablePredFormat(pred, EntailGraphFactoryAggregator.isCCG)) {
						continue;
					}

					// We also remove "-" here, because sometimes, we have the
					// type
					// without "-". But we didn't remove
					// "-" when we're looking in g kg, because it might help!

					for (int j = 1; j < 3; j++) {

						parts[j] = Util.simpleNormalize(parts[j]);

						// Do some more normalizations!
						// number normalization:

					}
					String type1 = null, type2 = null;

					if (!EntailGraphFactoryAggregator.useTimeEx) {
						try {
							type1 = Util.getType(parts[1], parts[3].charAt(0) == 'E', lineIdToStanTypes.get(lineId));
							type2 = Util.getType(parts[2], parts[3].charAt(1) == 'E', lineIdToStanTypes.get(lineId));
						} catch (Exception e) {
							System.out.println("t exception for: " + line);
						}
					} else {
						try {
							type1 = Util.getType(parts[1], true, null);
							type2 = Util.getType(parts[2], true, null);
						} catch (Exception e) {
							System.out.println("t exception for: " + line);
						}
						// timeInterval = parts[3].split(",")[0].substring(1);
						// System.out.println("right interval: "+timeInterval);
					}

					// if (lineNumbers%100==0){
					// sharedTime += (System.currentTimeMillis() - t0);
					// }
					/*
					 * !acceptableTypes.contains(type1) && !acceptableTypes.contains(type2) &&
					 * 
					 * 
					 */
					if (EntailGraphFactoryAggregator.typeScheme != TypeScheme.LDA
							&& !acceptableTypes.contains(type1 + "#" + type2)
							&& !acceptableTypes.contains(type2 + "#" + type1)) {
						continue;
					}

					// if (!EntailGraphFactoryAggregator.isCCG) {
					// for (int j = 1; j < 3; j++) {
					// parts[j] = Util.normalizeArg(parts[j]);
					// }
					// }
					String[] predicateLemma;
					if (!EntailGraphFactoryAggregator.rawExtractions) {
						predicateLemma = Util.getPredicateLemma(pred, EntailGraphFactoryAggregator.isCCG);
					} else {
						predicateLemma = new String[] { pred, "false" };
					}
					pred = predicateLemma[0];

					if (EntailGraphFactoryAggregator.onlyDSPreds
							&& !EntailGraphFactoryAggregator.dsPreds.contains(pred)) {
						// System.out.println("continue: " + pred);
						continue;
					}

					if (pred.equals("")) {
						continue;
					}
					// System.out.println("normalized: "+predicateLemma[0]);
					// String pred0 = pred;

					// if (pred.contains("(`")){//TODO: check what's going on!
					// System.err.println("weird!: "+pred0+" "+pred);
					// }
					// pred = Util.getPredicateSimple(pred);
					// System.out.println("pred lemma: " + pred);
					// System.out.println(pred);
					String arg1;
					String arg2;

					// false means args are reversed.
					if (predicateLemma[1].equals("false")) {
						arg1 = parts[1];
						arg2 = parts[2];// type1 and type2 are fine
					} else {
						arg1 = parts[2];
						arg2 = parts[1];
						// let's swap type1 and type2
						String tmp = type1;
						type1 = type2;
						type2 = tmp;
					}

					// Now we have pred, arg1 and arg2 and type1 and type2

					typedOp.println(pred + "#" + type1 + "#" + type2 + "::" + arg1 + "::" + arg2);
					if (allPredCounts.containsKey(pred)) {
						allPredCounts.put(pred, allPredCounts.get(pred) + 1);
					} else {
						allPredCounts.put(pred, 1);
					}

					// Added for LDA types
					if (EntailGraphFactoryAggregator.typeScheme == TypeScheme.LDA) {
						List<float[]> types = DistrTyping.getType(pred, arg1, arg2);
						// System.err.println(pred + "," + arg1 + "," + arg2);
						// System.err.println("types:");
						ArrayList<Integer> types1 = new ArrayList<>();// only the non-zero ones
						ArrayList<Integer> types2 = new ArrayList<>();// only the non-zero ones
						for (int k = 0; k < types.get(0).length; k++) {
							if (types.get(0)[k] != 0) {
								types1.add(k);
								// System.err.print(k + ":" + types.get(0)[k] + " ");
							}
						}
						// System.err.println();
						for (int k = 0; k < types.get(1).length; k++) {
							if (types.get(1)[k] != 0) {
								types2.add(k);
								// System.err.print(k + ":" + types.get(1)[k] + " ");
							}
						}
						// System.err.println();

						// Now, find all the likely type-pairs
						for (int t1 : types1) {
							for (int t2 : types2) {
								float prob = types.get(0)[t1] * types.get(1)[t2];
								if (prob < .1) {
									continue;
								}
								type1 = "type" + t1;
								type2 = "type" + t2;
								// System.err.println("adding");

								if (!acceptableTypes.contains(type1 + "#" + type2)
										&& !acceptableTypes.contains(type2 + "#" + type1)) {
									continue;
								}

								addRelationToEntGraphs(typesToGraph, pred, arg1, arg2, type1, type2, timeInterval, prob,
										false, false);
								if (type1.equals(type2)) {
									// the main one! (arg1-arg2)
									// String featName = arg1 + "#" + arg2;
									// String thisType = type1 + "#" + type2;
									addRelationToEntGraphs(typesToGraph, pred, arg1, arg2, type1, type2, timeInterval,
											prob, true, false);
								}
							}
						}

					} else {
						// the main one! (arg1-arg2)
						// String featName = arg1 + "#" + arg2;
						// String thisType = type1 + "#" + type2;
						// boolean rev =
						addRelationToEntGraphs(typesToGraph, pred, arg1, arg2, type1, type2, timeInterval, count, false,
								false);

						// F_X mixed
						// featName = arg1;
						// thisType = type1 + "#" + type2;
						// addRelationToEntGraphs(rev ? typesToGraphY :
						// typesToGraphX, pred, arg1, "", type1, type2,
						// timeInterval, count, rev, true);

						// F_Y mixed
						// featName = arg2;
						// thisType = type1 + "#" + type2;
						// addRelationToEntGraphs(rev ? typesToGraphX :
						// typesToGraphY, pred, "", arg2, type1, type2,
						// timeInterval, count, rev, true);

						// F_X unary
						// featName = arg1;
						// thisType = type1;
						// addRelationToEntGraphs(rev ? typesToGraphUnaryY :
						// typesToGraphUnaryX, pred, arg1, "", type1, "", rev,
						// true);
						// System.out.println("adding to unX: "+pred+" "+arg1+"
						// "+type1);
						// addRelationToEntGraphs(typesToGraphUnaryX, pred,
						// arg1,
						// "", type1, "", timeInterval, count, false,
						// true);

						// F_Y unary
						// featName = arg2;
						// thisType = type2;
						// addRelationToEntGraphs(rev ? typesToGraphUnaryX :
						// typesToGraphUnaryY, pred, "", arg2, "", type2, rev,
						// true);
						// addRelationToEntGraphs(typesToGraphUnaryY, pred, "",
						// arg2, "", type2, timeInterval, count, false,
						// true);

						if (type1.equals(type2)) {
							// the main one! (arg1-arg2)
							// String featName = arg1 + "#" + arg2;
							// String thisType = type1 + "#" + type2;
							addRelationToEntGraphs(typesToGraph, pred, arg1, arg2, type1, type2, timeInterval, count,
									true, false);

							// F_X mixed
							// featName = arg1;
							// thisType = type1 + "#" + type2;
							// addRelationToEntGraphs(typesToGraphX, pred, "",
							// arg2,
							// type1, type2, timeInterval, count, true,
							// true);

							// F_Y mixed
							// featName = arg2;
							// thisType = type1 + "#" + type2;
							// addRelationToEntGraphs(typesToGraphY, pred, arg1,
							// "",
							// type1, type2, timeInterval, count, true,
							// true);

							// F_X unary
							// featName = arg1;
							// thisType = type1;
							// addRelationToEntGraphs(typesToGraphUnaryX, pred,
							// "",
							// arg2, "", type2, true, true);
							// addRelationToEntGraphs(typesToGraphUnaryX, pred,
							// "",
							// arg2, "", type2, true, true);

							// F_Y unary
							// featName = arg2;
							// thisType = type2;
							// addRelationToEntGraphs(typesToGraphUnaryY, pred,
							// arg1,
							// "", type1, "", true, true);
							// addRelationToEntGraphs(typesToGraphUnaryY, pred,
							// arg1,
							// "", type1, "", true, true);

							// // duplicate nodes for equal types
							// if (type1.equals(type2) && !arg1.equals("") &&
							// !arg2.equals("")) {
							// thisEntailGraph.addBinaryRelation(Util.swapParts(pred),
							// thisArg2);
							// }
						}
					}

					// Added for LDA types

					// if (EntailGraphFactoryAggregator.isCCG &&
					// !pred.contains("__")) {
					// String pred0 = pred.substring(1, pred.length() - 1);
					// String[] ss = pred0.split(",");
					// String predX = ss[0];
					// String predY = ss[1];
					//
					// if (!predToDocument.containsKey(predX)) {
					// predToDocument.put(predX, "");
					// }
					// String d = predToDocument.get(predX);
					// predToDocument.put(predX, d + arg1.replace(" ", "_") + "
					// ");
					//
					// if (!predToDocument.containsKey(predY)) {
					// predToDocument.put(predY, "");
					// }
					// d = predToDocument.get(predY);
					// predToDocument.put(predY, d + arg2.replace(" ", "_") + "
					// ");
					// }

				}

				lineNumbers++;
				if (lineNumbers % 10000 == 0) {
					int mb = 1024 * 1024;
					long usedMb = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / mb;
					// System.gc();
					System.err.println(
							threadNum + ": " + lineNumbers + " " + ((System.currentTimeMillis() - startTime) / 1000)
									+ " " + usedMb + " allNNZ: " + EntailGraphFactoryAggregator.allNonZero);
				}
			} catch (Exception e) {
				System.err.println("exception for: " + line);
				e.printStackTrace();
			}
			typedOp.println();
		}

		System.err.println("allNNZ: " + EntailGraphFactoryAggregator.allNonZero);

		br.close();
	}

	// First, finds the related graph based on the type(s), then inserts the
	// pred. forceRev is because for unaryX you might have previously reversed
	// a pred, so you must do it again!
	boolean addRelationToEntGraphs(HashMap<String, EntailGraph> thisTypesToGraph, String pred, String arg1, String arg2,
			String type1, String type2, String timeInterval, float count, boolean forceRev, boolean unary) {

		String thisType = getThisType(type1, type2);

		boolean rev = (forceRev || (!thisType.equals(type1 + "#" + type2)) && !type1.equals("") && !type2.equals(""));

		if (!acceptableTypes.contains(thisType)) {
			// System.out.println("returning because not covered: " + thisType +
			// " " + threadNum);
			return rev;// this is because of
		}

		// if (rev) {
		// System.out.println("reversing for " + pred + " " + arg1 + " " +
		// arg2);
		// }
		String thisArg;

		if (!thisTypesToGraph.containsKey(thisType)) {
			String opName = typedEntGrDir + "/" + thisType;
			EntailGraph entGraph;
			if (type1.equals("") || type2.equals("")) {// unary
				entGraph = new EntailGraph(thisType, opName, EntailGraphFactoryAggregator.minPredForArg, unary);
			} else {
				entGraph = new EntailGraph(thisType, opName, EntailGraphFactoryAggregator.minPredForArgPair, unary);
			}

			thisTypesToGraph.put(thisType, entGraph);

		}

		EntailGraph thisEntailGraph = thisTypesToGraph.get(thisType);

		if (!rev) {// no reverse
			// has happened
			thisArg = arg1 + "#" + arg2;

		} else {
			// pred = Util.swapParts(pred);
			thisArg = arg2 + "#" + arg1;
		}

		String typedPred = getTypedPred(pred, type1, type2, rev);
		// System.out.println("now: "+rev+" "+typedPred+" "+type1+" "+type2+"
		// "+thisArg);
		// System.out.println("prev: "+rev+" "+(rev?Util.swapParts(pred):pred)+"
		// "+type1+" "+type2+" "+thisArg);

		// System.out.println("adding: " + pred + " " + thisArg + " t: " +
		// thisType + " " + rev);
		thisEntailGraph.addBinaryRelation(typedPred, thisArg, timeInterval, count);

		return rev;
	}

	String getTypedPred(String pred, String type1, String type2, boolean rev) {
		if (type1.equals("") || type2.equals("")) {
			return pred;
		}

		if (!type1.equals(type2)) {
			if (!EntailGraphFactoryAggregator.isCCG || !Util.swapParts(pred).equals(pred)) {
				return pred + "#" + type1 + "#" + type2;
			} else {
				return pred + "#" + getThisType(type1, type2);
			}

		} else {
			String typeD = type1 + "_1" + "#" + type1 + "_2";
			String typeR = type1 + "_2" + "#" + type1 + "_1";
			if (rev && (!EntailGraphFactoryAggregator.isCCG || !Util.swapParts(pred).equals(pred))) {
				return pred + "#" + typeR;
			} else {
				return pred + "#" + typeD;
			}
		}

	}

	HashSet<String> getAllPossiblePredPairsForEntGraph(SimpleEntailGraph entGraph) {
		HashSet<String> ret = new HashSet<>();
		for (SimplePredicateVector pvec : entGraph.getPvecs()) {
			for (SimilaritiesInfo simInfo : pvec.similarityInfos.values()) {
				String predPair = pvec.predicate + "#" + simInfo.predicate;
				System.err.println(simInfo.predicate);
				System.err.println(pvec.predicate);
				String predPairUntyped = pvec.predicate.split("#")[0] + "#" + simInfo.predicate.split("#")[0];
				ret.add(predPair);
				ret.add(predPairUntyped);
			}
		}
		return ret;
	}

	HashSet<String> getAllPossiblePredPairs(HashMap<String, SimpleEntailGraph> typesToGraph) {
		HashSet<String> ret = new HashSet<>();
		for (SimpleEntailGraph entGraph : typesToGraph.values()) {
			for (SimplePredicateVector pvec : entGraph.getPvecs()) {
				for (SimilaritiesInfo simInfo : pvec.similarityInfos.values()) {
					String predPair = pvec.predicate + "#" + simInfo.predicate;
					String predPairUntyped = pvec.predicate.split("#")[0] + "#" + simInfo.predicate.split("#")[0];
					ret.add(predPair);
					ret.add(predPairUntyped);
				}
			}
		}
		return ret;
	}

	// void processAllEntGraphs1() {
	// processAllEntGraphsUnary();
	// processAllEntGraphsBinary();
	//
	// System.err.println("processed Ent Graphs for: " + threadNum);
	// }

	void processAllEntGraphsBinary() {

		// long usedMb = (Runtime.getRuntime().totalMemory() - Runtime
		// .getRuntime().freeMemory()) / mb;
		// System.err.println("usedMB: " + usedMb);

		// arg1-arg2
		System.err.println("processing arg1-arg2");
		int mb = 1024 * 1024;
		int i = 0;
		for (String types : typesToGraph.keySet()) {
			EntailGraph entGraph = typesToGraph.get(types);
			entGraph.writeSims = entGraph.getPvecs().size() > 1;
			entGraph.writeInfo = entGraph.writeSims;
			entGraph.processGraph();

			SimpleEntailGraph simpleEntGraph = entGraph;
			if (EntailGraphFactoryAggregator.isTyped) {
				simpleEntGraph = new SimpleEntailGraph(entGraph);
			}

			HashSet<String> pps = getAllPossiblePredPairsForEntGraph(simpleEntGraph);
			for (String s : pps) {
				acceptablePredPairs.add(s);
			}
			typesToGraph.put(types, null);

			// EntailGraph entGraphX = typesToGraphX.get(types);
			// entGraphX.processGraph();
			// SimpleEntailGraph simpleEntGraphX = entGraphX;
			// if (EntailGraphFactoryAggregator.isTyped){
			// simpleEntGraphX = new SimpleEntailGraph(entGraphX);
			// }
			//
			// typesToGraphX.put(types, null);
			//
			// EntailGraph entGraphY = typesToGraphY.get(types);
			// entGraphY.processGraph();
			// SimpleEntailGraph simpleEntGraphY = entGraphY;
			// if (EntailGraphFactoryAggregator.isTyped){
			// simpleEntGraphY = new SimpleEntailGraph(entGraphY);
			// }
			//
			// typesToGraphY.put(types, null);

			if (i == 0) {
				long usedMb = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / mb;
				System.err.println("usedMB: " + usedMb + " allEdges: " + EntailGraphFactoryAggregator.allEdgeCounts);
			}

			// writeSimilaritiesBinary(simpleEntGraph, simpleEntGraphX,
			// simpleEntGraphY);
			writeSimilaritiesBinary(simpleEntGraph, null, null);

			if (i % 20 == 0) {
				System.err.println("typed count: " + i);
				long usedMb = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / mb;
				System.err.println("usedMB: " + usedMb + " allEdges: " + EntailGraphFactoryAggregator.allEdgeCounts);
			}
			i++;
		}
	}

	// void processAllEntGraphsBinary0() {
	//
	// // long usedMb = (Runtime.getRuntime().totalMemory() - Runtime
	// // .getRuntime().freeMemory()) / mb;
	// // System.err.println("usedMB: " + usedMb);
	//
	// // arg1-arg2
	// System.err.println("processing arg1-arg2");
	// int mb = 1024 * 1024;
	// int i = 0;
	// for (String types : typesToGraph.keySet()) {
	// EntailGraph entGraph = typesToGraph.get(types);
	// entGraph.writeSims = entGraph.pvecs.size() > 1;
	// entGraph.writeInfo = entGraph.writeSims;
	// entGraph.processGraph();
	//
	// SimpleEntailGraph simpleEntGraph = new SimpleEntailGraph(entGraph);
	//
	// EntailGraph entGraphX = typesToGraphX.get(types);
	// entGraphX.processGraph();
	// SimpleEntailGraph simpleEntGraphX = new SimpleEntailGraph(entGraphX);
	//
	// EntailGraph entGraphY = typesToGraphY.get(types);
	// entGraphY.processGraph();
	// SimpleEntailGraph simpleEntGraphY = new SimpleEntailGraph(entGraphY);
	//
	// writeSimilaritiesBinary(simpleEntGraph, simpleEntGraphX,
	// simpleEntGraphY);
	//
	// typesToGraph.put(types, null);
	// typesToGraphX.put(types, null);
	// typesToGraphY.put(types, null);
	//
	// if (i % 20 == 0) {
	// System.err.println("typed count: " + i);
	// long usedMb = (Runtime.getRuntime().totalMemory() -
	// Runtime.getRuntime().freeMemory()) / mb;
	// System.err.println("usedMB: " + usedMb + " allEdges: " +
	// EntailGraphFactoryAggregator.allEdgeCounts);
	// }
	// i++;
	// }
	//
	// // HashSet<String> predPairs =
	// // getAllPossiblePredPairs(this.typesToSimpleGraph);
	// // for (String s : predPairs) {
	// // acceptablePredPairs.add(s);
	// // }
	//
	// // // F_X mixed
	// // System.err.println("processing F_X");
	// // processEntGraphs(this.typesToGraphX, this.typesToSimpleGraphX,
	// // false);
	// //
	// // // F_Y mixed
	// // System.err.println("processing F_Y");
	// // processEntGraphs(this.typesToGraphY, this.typesToSimpleGraphY,
	// // false);
	//
	// }

	// Decided not to have unary represenation (c in 2015 paper)
	// void processAllEntGraphsUnary() {
	//
	// // F_X Unary
	// System.err.println("processing F_X Unary");
	// processEntGraphs(this.typesToGraphUnaryX, typesToSimpleGraphUnaryX,
	// false);
	// typesToGraphUnaryX = null;
	//
	// // F_Y Unary
	// System.err.println("processing F_Y Unary");
	// processEntGraphs(this.typesToGraphUnaryY, typesToSimpleGraphUnaryY,
	// false);
	// typesToGraphUnaryY = null;
	//
	// }

	void processEntGraphs(HashMap<String, EntailGraph> typesToGraph,
			HashMap<String, SimpleEntailGraph> typesToSimpleGraph, boolean shouldWrite) {
		int i = 0;
		long usedMb;
		int mb = 1024 * 1024;
		for (String types : typesToGraph.keySet()) {
			System.out.println("processing: " + types + " " + threadNum);
			EntailGraph entGraph = typesToGraph.get(types);
			entGraph.writeSims = shouldWrite && entGraph.getPvecs().size() > 1;
			entGraph.writeInfo = entGraph.writeSims;
			entGraph.processGraph();

			// Now, let's summarize the info!
			SimpleEntailGraph simpleEntGraph = new SimpleEntailGraph(entGraph);
			typesToSimpleGraph.put(types, simpleEntGraph);
			typesToGraph.put(types, null);
			// typesToGraph.remove(types);

			if (i % 20 == 0) {
				System.err.println("typed count: " + i);
				usedMb = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / mb;
				System.err.println("usedMB: " + usedMb + " allEdges: " + EntailGraphFactoryAggregator.allEdgeCounts);
			}
			i++;
		}
		usedMb = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / mb;
		if (usedMb > 10000) {
			usedMb = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / mb;
			System.err.println("usedMb: " + usedMb);
		}
	}

	void writeSimilaritiesBinary(SimpleEntailGraph entGraph, SimpleEntailGraph entGraphX, SimpleEntailGraph entGraphY) {

		if (!entGraph.writeSims) {
			return;
		}

		String types = entGraph.types;
		similarityFileTypes.add(types);

		int numPreds = 0;
		for (SimplePredicateVector pvec : entGraph.getPvecs()) {
			if (pvec.similarityInfos.size() != 0) {
				numPreds++;
			}
		}

		entGraph.graphOp2.println("types: " + types + ", num preds: " + numPreds);

		for (SimplePredicateVector pvec : entGraph.getPvecs()) {
			if (pvec.similarityInfos.size() == 0) {
				continue;
			}

			String pred = pvec.predicate;
			entGraph.graphOp2.println("predicate: " + pred);
			entGraph.graphOp2.println("num neighbors: " + pvec.similarityInfos.size());

			if (pvec.similarityInfos.size() == 0) {
				entGraph.graphOp2.println();
				continue;
			}

			ArrayList<Similarity> cosSimList = new ArrayList<Similarity>();
			ArrayList<Similarity> WeedsProbList = new ArrayList<Similarity>();
			ArrayList<Similarity> WeedsPMIList = new ArrayList<Similarity>();
			ArrayList<Similarity> WeedsPMIPrList = new ArrayList<Similarity>();
			// ArrayList<Similarity> SRList = new ArrayList<Similarity>();
			// ArrayList<Similarity> SRBinaryList = new ArrayList<Similarity>();
			ArrayList<Similarity> LinList = new ArrayList<Similarity>();
			ArrayList<Similarity> BIncList = new ArrayList<Similarity>();
			// ArrayList<Similarity> LinListSep = new ArrayList<Similarity>();
			// ArrayList<Similarity> BIncListSep = new ArrayList<Similarity>();
			ArrayList<Similarity> timeSimList = new ArrayList<Similarity>();

			for (SimilaritiesInfo simInfo : pvec.similarityInfos.values()) {
				String neighPred = simInfo.predicate;
				cosSimList.add(new Similarity(neighPred, simInfo.cosSim));
				WeedsProbList.add(new Similarity(neighPred, simInfo.WeedsProbSim));
				WeedsPMIList.add(new Similarity(neighPred, simInfo.WeedsPMISim));
				WeedsPMIPrList.add(new Similarity(neighPred, simInfo.weedPMIPr));
				// SRList.add(new Similarity(neighPred, simInfo.SRSim));
				// SRBinaryList.add(new Similarity(neighPred,
				// simInfo.SRBinarySim));
				LinList.add(new Similarity(neighPred, simInfo.LinSim));
				BIncList.add(new Similarity(neighPred, simInfo.BIncSim));
				timeSimList.add(new Similarity(neighPred, simInfo.timeSim));

				// Now, let's compute LinSeparate, BIncSeparate, LinUnary,
				// BIncUnary for pred, neighPred
				// System.err.println("pred, neigh: " + pred + " " +
				// neighPred);

				// System.err.println("simInfoX: " + simInfoX);
				// if (simInfoX == null) {
				// for (SimplePredicateVector p : entGraphX.pvecs) {
				// System.err.println(p.predicate + ":");
				//
				// for (SimilaritiesInfo si : p.similarityInfos.values()) {
				// System.err.println(si.predicate);
				// }
				// System.err.println();
				// }
				//
				// }

				// SimilaritiesInfo simInfoX = getSimInfo(entGraphX, pred,
				// neighPred);
				//
				// float LinX = simInfoX.LinSim;
				// float BIncX = simInfoX.BIncSim;
				//
				// SimilaritiesInfo simInfoY = getSimInfo(entGraphY, pred,
				// neighPred);
				// float LinY = simInfoY.LinSim;
				// float BIncY = simInfoY.BIncSim;
				//
				// float DIRTSep = geoAverage(LinX, LinY);
				// float BIncSep = geoAverage(BIncX, BIncY);
				// LinListSep.add(new Similarity(neighPred, DIRTSep));
				// BIncListSep.add(new Similarity(neighPred, BIncSep));

			}

			if (EntailGraphFactoryAggregator.useTimeEx) {
				PredicateVector.writeSims(entGraph.graphOp2, timeSimList, "time preceding sims");
			}
			PredicateVector.writeSims(entGraph.graphOp2, cosSimList, "cos sims");
			PredicateVector.writeSims(entGraph.graphOp2, WeedsProbList, "Weed's probabilistic sim");
			PredicateVector.writeSims(entGraph.graphOp2, WeedsPMIList, "Weed's PMI sim");
			// PredicateVector.writeSims(entGraph.graphOp2, SRList, "SR sims");
			// PredicateVector.writeSims(entGraph.graphOp2, SRBinaryList, "SR
			// Binary sims");
			PredicateVector.writeSims(entGraph.graphOp2, LinList, "Lin sims");
			PredicateVector.writeSims(entGraph.graphOp2, BIncList, "BInc sims");
			PredicateVector.writeSims(entGraph.graphOp2, WeedsPMIPrList, "Weed's PMI Precision sim");
			// PredicateVector.writeSims(entGraph.graphOp2, LinListSep, "DIRT
			// SEP sims");
			// PredicateVector.writeSims(entGraph.graphOp2, BIncListSep, "BINC
			// SEP sims");
			entGraph.graphOp2.println();
		}
		entGraph.graphOp2.close();
	}

	// void writeSimilaritiesUnary(String types) throws IOException {
	// // we already know that types has a file!
	// String fileName = typedEntGrDir + "/" + types + "_sim.txt";
	// // Now, read all the lines!
	// BufferedReader br = new BufferedReader(new FileReader(fileName));
	// ArrayList<String> lines = new ArrayList<>();
	// String line;
	// while ((line = br.readLine()) != null) {
	// lines.add(line);
	// }
	// br.close();
	//
	// if (lines.size() == 0) {
	// return;
	// }
	//
	// // rewrite the file and add the unary scores!
	// PrintStream op = new PrintStream(new File(fileName));
	//
	// // now, go through lines
	// int lineIdx = 0;
	// op.println(lines.get(lineIdx++));
	//
	// String type1 = types.split("#")[0];
	// String type2 = types.split("#")[1];
	// // if (!getThisType(type2, type1).equals(types)) {// type1, type2
	// // // should be swapped
	// // type1 = types.split("#")[1];
	// // type2 = types.split("#")[0];
	// // }
	// SimpleEntailGraph entGraphUnaryX = typesToSimpleGraphUnaryX.get(type1);
	// SimpleEntailGraph entGraphUnaryY = typesToSimpleGraphUnaryY.get(type2);
	//
	// while (lineIdx < lines.size()) {
	// line = lines.get(lineIdx++);// predicate:
	// op.println(line);
	// String pred = line.substring(11);
	// HashSet<String> neighs = new HashSet<>();
	// line = lines.get(lineIdx++);
	// op.println(line);// num neighs
	//
	// while (lineIdx < lines.size() && !(line =
	// lines.get(lineIdx)).startsWith("predicate: ")) {
	// op.println(line);
	// lineIdx++;
	// if (line.equals("") || line.endsWith(" sim") || line.endsWith(" sims")) {
	// continue;
	// } else {
	// // an actual neighbor!
	// String neigh = line.split(" ")[0];
	// neighs.add(neigh);
	// }
	//
	// }
	//
	// // Now, it's the time to write the LinUnary and BIncUnary
	// // similarities
	//
	// ArrayList<Similarity> LinListUnary = new ArrayList<Similarity>();
	// ArrayList<Similarity> BIncListUnary = new ArrayList<Similarity>();
	//
	// for (String neighPred : neighs) {
	//
	// // Now, let's compute LinUnary and BIncUnary for pred, neighPred
	//
	// // System.out.println("pred: " + pred);
	// SimilaritiesInfo simInfoUnaryX = getSimInfo(entGraphUnaryX,
	// pred.split("#")[0],
	// neighPred.split("#")[0]);
	//
	// // SimilaritiesInfo simInfoUnaryX =
	// // getSimInfo(entGraphUnaryX, pred, neighPred);
	//
	// float LinUnaryX = simInfoUnaryX.LinSim;
	// float BIncUnaryX = simInfoUnaryX.BIncSim;
	//
	// SimilaritiesInfo simInfoUnaryY = getSimInfo(entGraphUnaryY,
	// pred.split("#")[0],
	// neighPred.split("#")[0]);
	//
	// float LinUnaryY = simInfoUnaryY.LinSim;
	// float BIncUnaryY = simInfoUnaryY.BIncSim;
	//
	// float DIRTUnary = geoAverage(LinUnaryX, LinUnaryY);
	// float BIncUnary = geoAverage(BIncUnaryX, BIncUnaryY);
	// LinListUnary.add(new Similarity(neighPred, DIRTUnary));
	// BIncListUnary.add(new Similarity(neighPred, BIncUnary));
	//
	// }
	//
	// PredicateVector.writeSims(op, LinListUnary, "DIRT UNARY sims");
	// PredicateVector.writeSims(op, BIncListUnary, "BINC UNARY sims");
	//
	// op.println();
	// op.println();
	// }
	//
	// op.close();
	// }

	// void writeSimilaritiesAll() {
	//
	// for (String types : typesToSimpleGraph.keySet()) {
	// // System.err.println("types: " + types);
	// SimpleEntailGraph entGraph = typesToSimpleGraph.get(types);
	// if (!entGraph.writeSims) {
	// continue;
	// }
	// SimpleEntailGraph entGraphX = typesToSimpleGraphX.get(types);
	// SimpleEntailGraph entGraphY = typesToSimpleGraphY.get(types);
	// String type1 = types.split("#")[0];
	// String type2 = types.split("#")[1];
	// if (!getThisType(type2, type1).equals(types)) {// type1, type2
	// // should be swapped
	// type1 = types.split("#")[1];
	// type2 = types.split("#")[0];
	// }
	// SimpleEntailGraph entGraphUnaryX = typesToSimpleGraphUnaryX.get(type1);
	// SimpleEntailGraph entGraphUnaryY = typesToSimpleGraphUnaryY.get(type2);
	// // System.err.println(entGraph.pvecs.size());
	// // System.err.println(entGraph.graphOp2);
	// // Let's count pves with nnz size
	// int numPreds = 0;
	// for (SimplePredicateVector pvec : entGraph.pvecs) {
	// if (pvec.similarityInfos.size() != 0) {
	// numPreds++;
	// }
	// }
	// entGraph.graphOp2.println("types: " + types + ", num preds: " +
	// numPreds);
	// // System.out.println("types: " + types);
	//
	// for (SimplePredicateVector pvec : entGraph.pvecs) {
	// if (pvec.similarityInfos.size() == 0) {
	// continue;
	// }
	// String pred = pvec.predicate;
	// entGraph.graphOp2.println("predicate: " + pred);
	// entGraph.graphOp2.println("num neighbors: " +
	// pvec.similarityInfos.size());
	//
	// if (pvec.similarityInfos.size() == 0) {
	// entGraph.graphOp2.println();
	// continue;
	// }
	//
	// ArrayList<Similarity> cosSimList = new ArrayList<Similarity>();
	// ArrayList<Similarity> WeedsProbList = new ArrayList<Similarity>();
	// ArrayList<Similarity> WeedsPMIList = new ArrayList<Similarity>();
	// ArrayList<Similarity> SRList = new ArrayList<Similarity>();
	// ArrayList<Similarity> SRBinaryList = new ArrayList<Similarity>();
	// ArrayList<Similarity> LinList = new ArrayList<Similarity>();
	// ArrayList<Similarity> BIncList = new ArrayList<Similarity>();
	// ArrayList<Similarity> LinListSep = new ArrayList<Similarity>();
	// ArrayList<Similarity> BIncListSep = new ArrayList<Similarity>();
	// ArrayList<Similarity> LinListUnary = new ArrayList<Similarity>();
	// ArrayList<Similarity> BIncListUnary = new ArrayList<Similarity>();
	//
	// for (SimilaritiesInfo simInfo : pvec.similarityInfos.values()) {
	// String neighPred = simInfo.predicate;
	// cosSimList.add(new Similarity(neighPred, simInfo.cosSim));
	// WeedsProbList.add(new Similarity(neighPred, simInfo.WeedsProbSim));
	// WeedsPMIList.add(new Similarity(neighPred, simInfo.WeedsPMISim));
	// SRList.add(new Similarity(neighPred, simInfo.SRSim));
	// SRBinaryList.add(new Similarity(neighPred, simInfo.SRBinarySim));
	// LinList.add(new Similarity(neighPred, simInfo.LinSim));
	// BIncList.add(new Similarity(neighPred, simInfo.BIncSim));
	//
	// // Now, let's compute LinSeparate, BIncSeparate, LinUnary,
	// // BIncUnary for pred, neighPred
	// // System.err.println("pred, neigh: " + pred + " " +
	// // neighPred);
	// SimilaritiesInfo simInfoX = getSimInfo(entGraphX, pred, neighPred);
	// // System.err.println("simInfoX: " + simInfoX);
	// // if (simInfoX == null) {
	// // for (SimplePredicateVector p : entGraphX.pvecs) {
	// // System.err.println(p.predicate + ":");
	// //
	// // for (SimilaritiesInfo si : p.similarityInfos.values()) {
	// // System.err.println(si.predicate);
	// // }
	// // System.err.println();
	// // }
	// //
	// // }
	// float LinX = simInfoX.LinSim;
	// float BIncX = simInfoX.BIncSim;
	//
	// SimilaritiesInfo simInfoY = getSimInfo(entGraphY, pred, neighPred);
	// float LinY = simInfoY.LinSim;
	// float BIncY = simInfoY.BIncSim;
	//
	// float DIRTSep = geoAverage(LinX, LinY);
	// float BIncSep = geoAverage(BIncX, BIncY);
	// LinListSep.add(new Similarity(neighPred, DIRTSep));
	// BIncListSep.add(new Similarity(neighPred, BIncSep));
	//
	// // System.out.println("pred: " + pred);
	// SimilaritiesInfo simInfoUnaryX = getSimInfo(entGraphUnaryX,
	// pred.split("#")[0],
	// neighPred.split("#")[0]);
	//
	// // SimilaritiesInfo simInfoUnaryX =
	// // getSimInfo(entGraphUnaryX, pred, neighPred);
	//
	// float LinUnaryX = simInfoUnaryX.LinSim;
	// float BIncUnaryX = simInfoUnaryX.BIncSim;
	//
	// SimilaritiesInfo simInfoUnaryY = getSimInfo(entGraphUnaryY,
	// pred.split("#")[0],
	// neighPred.split("#")[0]);
	//
	// float LinUnaryY = simInfoUnaryY.LinSim;
	// float BIncUnaryY = simInfoUnaryY.BIncSim;
	//
	// float DIRTUnary = geoAverage(LinUnaryX, LinUnaryY);
	// float BIncUnary = geoAverage(BIncUnaryX, BIncUnaryY);
	// LinListUnary.add(new Similarity(neighPred, DIRTUnary));
	// BIncListUnary.add(new Similarity(neighPred, BIncUnary));
	//
	// }
	//
	// PredicateVector.writeSims(entGraph, cosSimList, "cos sims");
	// PredicateVector.writeSims(entGraph, WeedsProbList, "Weed's probabilistic
	// sim");
	// PredicateVector.writeSims(entGraph, WeedsPMIList, "Weed's PMI sim");
	// PredicateVector.writeSims(entGraph, SRList, "SR sims");
	// PredicateVector.writeSims(entGraph, SRBinaryList, "SR Binary sims");
	// PredicateVector.writeSims(entGraph, LinList, "Lin sims");
	// PredicateVector.writeSims(entGraph, BIncList, "BInc sims");
	// PredicateVector.writeSims(entGraph, LinListSep, "DIRT SEP sims");
	// PredicateVector.writeSims(entGraph, BIncListSep, "BINC SEP sims");
	// PredicateVector.writeSims(entGraph, LinListUnary, "DIRT UNARY sims");
	// PredicateVector.writeSims(entGraph, BIncListUnary, "BINC UNARY sims");
	//
	// entGraph.graphOp2.println();
	// entGraph.graphOp2.println();
	//
	// }
	// }
	// }

	float geoAverage(float x, float y) {
		return (float) Math.sqrt(x * y);
	}

	// SimilaritiesInfo getSimInfo(EntailGraph entGraph, String pred, String
	// neighPred) {
	// if (!entGraph.predToIdx.containsKey(pred) ||
	// !entGraph.predToIdx.containsKey(neighPred)){
	// return new SimilaritiesInfo(neighPred);
	// }
	// int predIdx = entGraph.predToIdx.get(pred);
	// int neighIdx = entGraph.predToIdx.get(neighPred);
	// PredicateVector pvec = entGraph.pvecs.get(predIdx);
	// SimilaritiesInfo simInfo = pvec.similarityInfos.get(neighIdx);
	// return simInfo;
	// }

	SimilaritiesInfo getSimInfo(SimpleEntailGraph entGraph, String pred, String neighPred) {
		// System.err.println("entGraph: " + entGraph);
		if (!entGraph.predToIdx.containsKey(pred) || !entGraph.predToIdx.containsKey(neighPred)) {
			// System.out.println("no " + pred + " or " + neighPred + " in " +
			// entGraph.types);
			return new SimilaritiesInfo(neighPred);
		}
		int predIdx = entGraph.predToIdx.get(pred);
		int neighIdx = entGraph.predToIdx.get(neighPred);
		SimplePredicateVector pvec = entGraph.getPvecs().get(predIdx);
		SimilaritiesInfo simInfo = pvec.similarityInfos.get(neighIdx);
		if (simInfo == null) {
			// System.out.println("no connection " + pred + " and " + neighPred
			// + " in " + entGraph.types);
			return new SimilaritiesInfo(neighPred);
		}
		return simInfo;
	}

	String getThisType(String type1, String type2) {
		if (type1.equals("")) {
			return type2;
		} else if (type2.equals("")) {
			return type1;
		}
		String thisType1 = type1 + "#" + type2;
		String thisType2 = type2 + "#" + type1;
		String thisType;

		if (typeToOrderedType.containsKey(thisType1)) {
			thisType = typeToOrderedType.get(thisType1);
		} else {
			typeToOrderedType.put(thisType1, thisType1);
			typeToOrderedType.put(thisType2, thisType1);
			thisType = thisType1;
		}
		return thisType;
	}

	void setAcceptableTypes(HashSet<String> acceptableTypes) {
		this.acceptableTypes = acceptableTypes;
	}

	public static void main(String[] args) throws JsonSyntaxException, IOException {
		String fileName;
		String entTypesFName;
		String genTypesFName;
		String typedEntGrDir;
		if (args.length == 3) {
			fileName = args[0];
			entTypesFName = args[1];
			genTypesFName = args[2];
			typedEntGrDir = args[3];
		} else {
			fileName = "news_NEs_NEL.json";
			entTypesFName = "entTypes.txt";
			genTypesFName = "genTypes.txt";
			typedEntGrDir = "typedEntGrDir_test";
		}

		EntailGraphFactory egFact = new EntailGraphFactory(fileName, entTypesFName, genTypesFName, typedEntGrDir);

		egFact.acceptableTypes = new HashSet<>();

		// build the graphs
		egFact.buildGraphs();

		// process all the entailmentGraphs
		// egFact.processAllEntGraphs();
		egFact.processAllEntGraphsBinary();

		// now, let's just write the similarities!
		// egFact.writeSimilaritiesAll();
	}

}
