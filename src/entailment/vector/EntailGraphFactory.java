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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import constants.ConstantsAgg;
import entailment.Util;
import entailment.linkingTyping.DistrTyping;
import entailment.linkingTyping.StanfordNERHandler;
import entailment.vector.EntailGraphFactoryAggregator.TypeScheme;

public class EntailGraphFactory implements Runnable {
	String fName, entTypesFName;
	Map<String, EntailGraph> typesToGraph = new HashMap<>();
	
	// HashMap<String, EntailGraph> typesToGraphX = new HashMap<>();
	// HashMap<String, EntailGraph> typesToGraphY = new HashMap<>();
	// HashMap<String, EntailGraph> typesToGraphUnaryX = new HashMap<>();
	// HashMap<String, EntailGraph> typesToGraphUnaryY = new HashMap<>();
	// static HashMap<String, SimpleEntailGraph> typesToSimpleGraphUnaryX = new
	// HashMap<>();
	// static HashMap<String, SimpleEntailGraph> typesToSimpleGraphUnaryY = new
	// HashMap<>();
	// Set<String> similarityFileTypes = new HashSet<>();

	static Map<String, Integer> allPredCounts = new ConcurrentHashMap<>();
	// static Map<String, String> predToDocument = new ConcurrentHashMap<>();

	// For knowing the ordering: type1, type2 => type1+type2. type2, type1 =>
	// type1 + type2
	HashMap<String, String> typeToOrderedType = new HashMap<>();
	String typedEntGrDir;
	HashSet<String> acceptableTypes = new HashSet<>();
	// static HashSet<String> acceptablePredPairs = new HashSet<>();
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
			// System.out.println(line);

//			if (lineNumbers == 10000) {
//				break;// TODO: remove this
//			}
			//
//			if (lineNumbers > 0 && lineNumbers % 1000000 == 0 && ConstantsAgg.backupToStanNER) {
//				Util.renewStanfordParser();
//			}

			if (line.startsWith("exception for") || line.contains("nlp.pipeline")) {
				continue;
			}
			try {
				int lineId = -1;
				List<String> relStrs = new ArrayList<>();
				List<Integer> counts = new ArrayList<>();
				String newsLine = null;
				String datestamp = "";

				if (ConstantsAgg.GBooksCCG) {
					lineId++;
					line = line.split("$$")[0];
					String[] ss = line.split("\t");
					int count = Integer.parseInt(ss[3]);
					newsLine = ss[0] + " " + ss[2] + " " + ss[1] + ".";
					String rel = ss[4];
					ss = rel.split(" ");
					String relStr = "(" + ss[0] + "::" + ss[1] + "::" + ss[2] + "::" + ss[4] + "::0::" + ss[3] + ")";
					relStrs.add(relStr);
					counts.add(count);

				} else if (!ConstantsAgg.rawExtractions) {
					JsonObject jObj = jsonParser.parse(line).getAsJsonObject();
					lineId = jObj.get("lineId").getAsInt();
					if (ConstantsAgg.addTimeStampToFeats) {
						datestamp = jObj.get("date").getAsString();
						String[] ds_ss = datestamp.split(" ");
						// datestamp = ds_ss[0] + "_" + ds_ss[1] + "_" + ds_ss[2];
						datestamp = Util.getWeek(ds_ss[0] + " " + ds_ss[1] + " " + ds_ss[2]);
						// System.out.println("datestamp: " + datestamp);
					}

					newsLine = jObj.get("s").getAsString();
					// typedOp.println("line: " + newsLine);
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

				// t0 = 0;

				for (int i = 0; i < relStrs.size(); i++) {
					// if (lineNumbers%100==0){
					// t0 = System.currentTimeMillis();
					// }

					String relStr = relStrs.get(i);
					int count = counts.get(i);
					String timeInterval = null;
					if (ConstantsAgg.useTimeEx) {
						// System.out.println("relStr: "+relStr);
						String[] ss = relStr.split("\\)::\\[");// TODO: fix this
						timeInterval = "[" + ss[1];
						relStr = ss[0] + ")";
						// System.out.println("now relStr: "+relStr);
					}
					relStr = relStr.substring(1, relStr.length() - 1);
					String[] parts = relStr.split("::");

					// TODO: remove this, it's just for sanity check!
					// if (ConstantsAgg.removeGGFromTopPairs && parts[3].charAt(0) == 'G' &&
					// parts[3].charAt(1) == 'G') {
					// // System.out.println("continue, both GG: " + pred + " " + arg1 + " " + arg2
					// // +" "+type1+" "+type2);
					// continue;
					// }

					String pred = parts[0];
					// System.out.println("now pred: "+pred);

					if (!Util.acceptablePredFormat(pred, ConstantsAgg.isCCG)) {
						continue;
					}

					String[] predicateLemma;
					if (!ConstantsAgg.rawExtractions && !ConstantsAgg.isForeign) {
						predicateLemma = Util.getPredicateNormalized(pred, ConstantsAgg.isCCG);
						if (predicateLemma == null) {
							System.err.println(pred);
							System.err.println("predlemma is null");
							predicateLemma = Util.getPredicateNormalized(pred, ConstantsAgg.isCCG);
						}
					} else {
						predicateLemma = new String[] { pred, "false" };
					}
					if (predicateLemma == null) {
						System.err.println(pred);
						System.err.println("predlemma is null");
					}
					pred = predicateLemma[0];

					if (ConstantsAgg.onlyDSPreds && !EntailGraphFactoryAggregator.dsPreds.contains(pred)) {
						// System.out.println("continue: " + pred);
						continue;
					}

					if (ConstantsAgg.removeStopPreds && Util.stopPreds.contains(pred)) {
						continue;
					}

					if (pred.equals("")) {
						continue;
					}

					if (ConstantsAgg.maxPredsTotal > 0
							&& !EntailGraphFactoryAggregator.acceptablePreds.contains(pred)) {
						continue;
					}

					// Now, we might need to to backuptoStan. We do this down here to prevent
					// unnecessary overhead!
					// let's see if we have NER ed the line, otherwise, do it
					// This was moved to a new class (StanfordNERHandler and finder) and will be
					// done in advance
					// if (ConstantsAgg.backupToStanNER && EntailGraphFactoryAggregator.typeScheme
					// == TypeScheme.FIGER
					// && lineIdSeen.get(lineId) == 1) {
					// // System.err.println("lid: "+lineId+" "+lineIdSeen.get(lineId)+"
					// "+threadNum);
					// Map<String, String> tokenToType = Util.getSimpleNERTypeSent(newsLine);
					// lineIdToStanTypes.put(lineId, tokenToType);
					// }

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

					if (ConstantsAgg.isForeign) {
						if (ConstantsAgg.isTyped) {
							type1 = parts[3];// .substring(1);
							type2 = parts[4];// .substring(1);
							// System.out.println("types foreign: " + type1 + " " + type2);
						} else {
							type1 = "thing";
							type2 = "thing";
						}

					} else if (!ConstantsAgg.useTimeEx) {
						try {
//							if (StanfordNERHandler.lineIdToStanTypes.get(lineId) == null) {
//								System.out.println("couldn't prepare types in time!" + lineId);
//							} else {
//								System.out.println("could prepare types on time!");
//							}
							type1 = Util.getType(parts[1], parts[3].charAt(0) == 'E',
									StanfordNERHandler.lineIdToStanTypes.get(lineId));
							type2 = Util.getType(parts[2], parts[3].charAt(1) == 'E',
									StanfordNERHandler.lineIdToStanTypes.get(lineId));
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

					if (ConstantsAgg.removeGGFromTopPairs
							&& EntailGraphFactoryAggregator.type2RankNS.containsKey(type1 + "#" + type2)
							&& EntailGraphFactoryAggregator.type2RankNS
									.get(type1 + "#" + type2) < ConstantsAgg.numTopTypePairs
							&& parts[3].charAt(0) == 'G' && parts[3].charAt(1) == 'G') {
						// System.out.println("continue, both GG: " + pred + " " + arg1 + " " + arg2 +"
						// "+type1+" "+type2);
						continue;
					}

					// TODO: be careful, added on 14/04/19
					if (ConstantsAgg.removeGGFromTopPairs && parts[3].charAt(0) == 'G' && parts[3].charAt(1) == 'G'
							&& (type1.equals("thing") || type2.equals("thing")) ) {
						continue;
					}
					
					// TODO: move this to parsing, added on 25/04/19
					if (ConstantsAgg.removePronouns) {
						if (Util.pronouns.contains(arg1) || Util.pronouns.contains(arg2)) {
							continue;
						}
					}

					// if (ConstantsAgg.maxPredsTotalTypeBased > 0
					// && EntailGraphFactoryAggregator.typesToAcceptablePreds.containsKey(type1 +
					// "#" + type2)) {
					//
					// Set<String> thisAcceptablePreds =
					// EntailGraphFactoryAggregator.typesToAcceptablePreds
					// .get(type1 + "#" + type2);
					// Set<String> thisAcceptableArgPairs =
					// EntailGraphFactoryAggregator.typesToAcceptableArgPairs
					// .get(type1 + "#" + type2);
					//
					// if (type1.equals(type2)) {
					//
					// String typeD = type1 + "_1" + "#" + type1 + "_2";
					// String typeR = type1 + "_2" + "#" + type1 + "_1";
					//
					// String predD = pred + "#" + typeD;
					// String predR = pred + "#" + typeR;
					//
					// String argPairD = arg1 + "#" + arg2;
					// String argPairR = arg2 + "#" + arg1;
					//
					// if (!thisAcceptablePreds.contains(predD) &&
					// !thisAcceptablePreds.contains(predR)) {
					// // System.out.println(pred + " not accepable for " + type1 + "#" + type2);
					// continue;
					// }
					//
					// if (!thisAcceptableArgPairs.contains(argPairD)
					// && !thisAcceptableArgPairs.contains(argPairR)) {
					// // System.out.println(pred + " not accepable for " + type1 + "#" + type2);
					// continue;
					// }
					//
					// } else {
					// String predD = pred + "#" + type1 + "#" + type2;
					//
					// if (!thisAcceptablePreds.contains(predD)) {
					// // System.out.println(pred + " not accepable for " + type1 + "#" + type2);
					// continue;
					// }
					//
					// String argPairD = arg1 + "#" + arg2;
					// if (!thisAcceptableArgPairs.contains(argPairD)) {
					// // System.out.println(pred + " not accepable for " + type1 + "#" + type2);
					// continue;
					// }
					//
					// }
					// }

					// System.out.println("pred args: "+pred+" "+arg1+" "+arg2);//TODO: remove

					// Now we have pred, arg1 and arg2 and type1 and type2

					// typedOp.println(pred + "#" + type1 + "#" + type2 + "::" + arg1 + "::" +
					// arg2);

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

								addRelationToEntGraphs(typesToGraph, pred, arg1, arg2, type1, type2, timeInterval,
										datestamp, prob, false, false);
								if (type1.equals(type2)) {
									// the main one! (arg1-arg2)
									// String featName = arg1 + "#" + arg2;
									// String thisType = type1 + "#" + type2;
									addRelationToEntGraphs(typesToGraph, pred, arg1, arg2, type1, type2, timeInterval,
											datestamp, prob, true, false);
								}
							}
						}

					} else {
						// the main one! (arg1-arg2)
						// String featName = arg1 + "#" + arg2;
						// String thisType = type1 + "#" + type2;
						// boolean rev =

						EntailGraphFactoryAggregator.numAllTuples++;

						addRelationToEntGraphs(typesToGraph, pred, arg1, arg2, type1, type2, timeInterval, datestamp,
								count, false, false);

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

						if (type1.equals(type2) && !arg1.equals(arg2)) {// TODO: be careful: the second condition added
																		// on 31 March 2019, but ablation shows it's
																		// useful!
							// the main one! (arg1-arg2)
							// String featName = arg1 + "#" + arg2;
							// String thisType = type1 + "#" + type2;
							addRelationToEntGraphs(typesToGraph, pred, arg1, arg2, type1, type2, timeInterval,
									datestamp, count, true, false);

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
					System.err.println(threadNum + ": " + lineNumbers + " time(s): "
							+ ((System.currentTimeMillis() - startTime) / 1000) + " memory (mb):" + usedMb
							+ " alltuples: " + EntailGraphFactoryAggregator.numAllTuples + " alltuplesPlusRev: "
							+ EntailGraphFactoryAggregator.numAllTuplesPlusReverse + " allNNZ: "
							+ EntailGraphFactoryAggregator.allNonZero);
				}
			} catch (Exception e) {
				System.err.println("exception for: " + line);
				e.printStackTrace();
			}
			// typedOp.println();
		}

		System.err.println("allNNZ: " + EntailGraphFactoryAggregator.allNonZero);

		br.close();
	}

	// First, finds the related graph based on the type(s), then inserts the
	// pred. forceRev is because for unaryX you might have previously reversed
	// a pred, so you must do it again!
	boolean addRelationToEntGraphs(Map<String, EntailGraph> thisTypesToGraph, String pred, String arg1, String arg2,
			String type1, String type2, String timeInterval, String datestamp, float count, boolean forceRev,
			boolean unary) {

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
				entGraph = new EntailGraph(thisType, opName, ConstantsAgg.minPredForArg, unary);
			} else {
				entGraph = new EntailGraph(thisType, opName, ConstantsAgg.minPredForArgPair, unary);
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
		if (ConstantsAgg.addTimeStampToFeats) {
			thisArg += "#" + datestamp;
		}
		thisEntailGraph.addBinaryRelation(typedPred, thisArg, timeInterval, count, -1, -1);

		return rev;
	}

	String getTypedPred(String pred, String type1, String type2, boolean rev) {
		if (type1.equals("") || type2.equals("")) {
			return pred;
		}

		if (!type1.equals(type2)) {
			if (!ConstantsAgg.isCCG || !Util.swapParts(pred).equals(pred)) {
				return pred + "#" + type1 + "#" + type2;
			} else {
				return pred + "#" + getThisType(type1, type2);
			}

		} else {
			String typeD = type1 + "_1" + "#" + type1 + "_2";
			String typeR = type1 + "_2" + "#" + type1 + "_1";
			if (rev && (!ConstantsAgg.isCCG || !Util.swapParts(pred).equals(pred))) {
				return pred + "#" + typeR;
			} else {
				return pred + "#" + typeD;
			}
		}

	}

	Set<String> getAllPossiblePredPairsForEntGraph(SimpleEntailGraph entGraph) {
		HashSet<String> ret = new HashSet<>();
		for (SimplePredicateVector pvec : entGraph.getPvecs()) {
			for (SimilaritiesInfo simInfo : pvec.similarityInfos.values()) {
				String predPair = pvec.predicate + "#" + simInfo.predicate;
				// System.err.println(simInfo.predicate);
				// System.err.println(pvec.predicate);
				String predPairUntyped = pvec.predicate.split("#")[0] + "#" + simInfo.predicate.split("#")[0];
				ret.add(predPair);
				ret.add(predPairUntyped);
			}
		}
		return ret;
	}

	Set<String> getAllPossiblePredPairs(Map<String, SimpleEntailGraph> typesToGraph) {
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
			if (ConstantsAgg.isTyped) {
				simpleEntGraph = new SimpleEntailGraph(entGraph);
			}

			Set<String> pps = getAllPossiblePredPairsForEntGraph(simpleEntGraph);
			// for (String s : pps) {
			// acceptablePredPairs.add(s);
			// }
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

	// void processEntGraphs(HashMap<String, EntailGraph> typesToGraph,
	// HashMap<String, SimpleEntailGraph> typesToSimpleGraph, boolean shouldWrite) {
	// int i = 0;
	// long usedMb;
	// int mb = 1024 * 1024;
	// for (String types : typesToGraph.keySet()) {
	// System.out.println("processing: " + types + " " + threadNum);
	// EntailGraph entGraph = typesToGraph.get(types);
	// entGraph.writeSims = shouldWrite && entGraph.getPvecs().size() > 1;
	// entGraph.writeInfo = entGraph.writeSims;
	// entGraph.processGraph();
	//
	// // Now, let's summarize the info!
	// SimpleEntailGraph simpleEntGraph = new SimpleEntailGraph(entGraph);
	// typesToSimpleGraph.put(types, simpleEntGraph);
	// typesToGraph.put(types, null);
	// // typesToGraph.remove(types);
	//
	// if (i % 20 == 0) {
	// System.err.println("typed count: " + i);
	// usedMb = (Runtime.getRuntime().totalMemory() -
	// Runtime.getRuntime().freeMemory()) / mb;
	// System.err.println("usedMB: " + usedMb + " allEdges: " +
	// EntailGraphFactoryAggregator.allEdgeCounts);
	// }
	// i++;
	// }
	// usedMb = (Runtime.getRuntime().totalMemory() -
	// Runtime.getRuntime().freeMemory()) / mb;
	// if (usedMb > 10000) {
	// usedMb = (Runtime.getRuntime().totalMemory() -
	// Runtime.getRuntime().freeMemory()) / mb;
	// System.err.println("usedMb: " + usedMb);
	// }
	// }

	void writeSimilaritiesBinary(SimpleEntailGraph entGraph, SimpleEntailGraph entGraphX, SimpleEntailGraph entGraphY) {

		if (!entGraph.writeSims) {
			return;
		}

		String types = entGraph.types;
		// similarityFileTypes.add(types);

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
			ArrayList<Similarity> probELList = new ArrayList<Similarity>();

			for (SimilaritiesInfo simInfo : pvec.similarityInfos.values()) {
				String neighPred = simInfo.predicate;
				if (!ConstantsAgg.onlyBinc) {
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
					probELList.add(new Similarity(neighPred, simInfo.probELSim));
				} else {
					BIncList.add(new Similarity(neighPred, simInfo.BIncSim));
				}

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

			if (!ConstantsAgg.onlyBinc) {
				if (ConstantsAgg.useTimeEx) {
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

				if (ConstantsAgg.computeProbELSims) {
					PredicateVector.writeSims(entGraph.graphOp2, probELList, "probEL sim");
				}
			} else {
				PredicateVector.writeSims(entGraph.graphOp2, BIncList, "BInc sims");
			}

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
