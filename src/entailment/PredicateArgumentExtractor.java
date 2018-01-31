package entailment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import in.sivareddy.graphparser.ccg.LexicalItem;
import in.sivareddy.graphparser.cli.CcgParseToUngroundedGraphs;
import in.sivareddy.graphparser.parsing.LexicalGraph;
import in.sivareddy.graphparser.util.graph.Edge;
import in.sivareddy.graphparser.util.graph.Type;
import in.sivareddy.graphparser.util.knowledgebase.Property;
import uk.co.flamingpenguin.jewel.cli.ArgumentValidationException;

//This is gonna be called in a Thread
public class PredicateArgumentExtractor implements Runnable {

	public static HashSet<String> acceptableGEStrs;

	static {
		String[] accepteds = new String[] { "GE", "EG", "EE" };//
		acceptableGEStrs = new HashSet<>();
		for (String s : accepteds) {
			acceptableGEStrs.add(s);
		}
	}

	public static final boolean lemmatizePred = true;// eaten.might.1 => eat.might.1

	String line;
	public static CcgParseToUngroundedGraphs parser;

	// static String[] args;
	// ArrayList<String> spotsStr = new ArrayList<>();
	// ArrayList<String> spotsWikiEnt = new ArrayList<>();
	JsonParser jsonParser = new JsonParser();

	public PredicateArgumentExtractor(String line) {
		this.line = line;
	}

	static {
		// creates a StanfordCoreNLP object, with POS tagging, lemmatization,
		// NER, parsing, and coreference resolution
		// Properties props = new Properties();
		// props.put("annotators",
		// "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
		// props.put("annotators", "tokenize,ssplit");

		// pipeline = new StanfordCoreNLP(props);

		// props = new Properties();
		// props.put("annotators", "tokenize,ssplit");
		// pipelineLemma = new StanfordCoreNLP(props);

		try {
			// parser = new CcgParseToUngroundedGraphs("lib_data", "en-ner",
			// true);
			parser = new CcgParseToUngroundedGraphs("lib_data", "en", true);
		} catch (ArgumentValidationException | IOException e) {
			e.printStackTrace();
		}
	}

	public void setLine(String line) {
		this.line = line;
	}

	@Override
	public void run() {
		// HashSet<String> entsSet = null;
		// if (LinesHandler.convToEntityLinked) {
		// entsSet = new HashSet<String>();
		// spotsStr = new ArrayList<String>();
		// spotsWikiEnt = new ArrayList<String>();
		// }
		String mainStr = "";
		String mainStrOnlyNEs = "";

		try {
			JsonObject obj = (JsonObject) jsonParser.parse(line).getAsJsonObject();
			String text = obj.get("s").getAsString();
			// text = Util.normalize(text);
			String date = obj.get("date").getAsString();
			long articleId = obj.get("articleId").getAsLong();
			long lineId = obj.get("lineId").getAsLong();

			mainStr += "#line: " + text + "\n";
			mainStr += "#lineId: " + lineId + "\n";
			mainStr += "#articleId: " + articleId + "\n";
			mainStr += "#date: " + date + "\n";

			mainStrOnlyNEs = mainStr;

			String[] predArgStrs = extractPredArgsStrs(text);
			mainStr += predArgStrs[0];
			mainStrOnlyNEs += predArgStrs[1];

		} catch (Exception e) {
			System.err.println("exception for " + line);
			e.printStackTrace();
		}

		LinesHandler.mainStrs.add(mainStr);
		LinesHandler.mainStrsOnlyNEs.add(mainStrOnlyNEs);

		System.out.println(mainStr);

		// if (LinesHandler.convToEntityLinked) {
		// for (String spot : entsSet) {
		// String wikiName = EntToWikiFinder.getWikiNamedEntity(spot);
		// spotsStr.add(spot);
		// spotsWikiEnt.add(wikiName);
		// }
		//
		// LinesHandler.spots.add(spotsStr);
		// LinesHandler.wikiNames.add(spotsWikiEnt);
		// LinesHandler.lines.add(line);
		// }
	}

	public String extractPredArgsStrsForceFinding(String text, String arg1, String arg2, boolean debug)
			throws ArgumentValidationException, IOException, InterruptedException {
		// parser.nbestParses = 10;
		String[] ret = extractPredArgsStrsForceFinding(text, arg1, arg2, true, debug);
		// parser.nbestParses = 1;
		// if (ret[1].equals("false")) {
		// System.out.println("bad VP "+ text);
		// String[] ret2 = extractPredArgsStrsForceFinding(text, arg1, arg2,
		// true);
		// if (ret2[1].equals("true")) {
		// System.out.println("but replace with NP! "+text);
		// return ret2[0];
		// }
		// else if(ret[0].equals("")){
		// System.out.println("last try with NP! "+text);
		// return ret2[0];
		// }
		// }
		return ret[0];
	}

	// Do your best to find a good one. That means, rel, arg1, arg2 be different
	// indexes
	// dsStr,argMatch?
	public String[] extractPredArgsStrsForceFinding(String text, String arg1, String arg2, boolean acceptNP, boolean debug)
			throws ArgumentValidationException, IOException, InterruptedException {
		// System.out.println(text);
		String ret = "";
		int syntaxIdx = 0;
		boolean partlyMatched = false;
		
		
		text = Util.preprocess(text);
		String sentence = text;
		String mySent = "{\"sentence\" : \"" + sentence + "\"}";
		List<List<LexicalGraph>> allGraphs = parser.processText(mySent);
		
		if (allGraphs.size()==0) {
			return new String[] { ret, "false" };
		}
		
//		System.out.println("num syn parses: "+allGraphs.get(0).size());

		while (syntaxIdx<allGraphs.get(0).size()) {
			String[] predArgsStrs = extractPredArgsStrs(text, syntaxIdx, acceptNP, true, allGraphs);
			String[] dsStrs = predArgsStrs[2].split("\n");// This might have
															// multiple
															// candidates
															// itself. Let's see
															// if any of those
															// are good!
			if (debug) {
				System.out.println("cont: " + predArgsStrs[3]);
			}
			if (dsStrs.length == 0) {
				syntaxIdx++;
				continue;
			}
			boolean argsMatch = false;
			String thisDSStr = "";
			if (debug) {
				System.out.println(text);
				System.out.println("dsStrs: " + predArgsStrs[2]);
			}
			
			arg1 = Util.getLemma(arg1).replace("_", " ");//ADDED 14 Jan 18
			arg2 = Util.getLemma(arg2).replace("_", " ");
			for (String cand : dsStrs) {
				if (debug) {
					System.out.println("cand: " + cand);
				}
				if (cand.equals("")) {
					continue;
				}
				String[] ss = cand.split(" ");
				String[] thisArgs = new String[] { ss[1], ss[2] };
				for (int i = 0; i < thisArgs.length; i++) {
					thisArgs[i] = thisArgs[i].replace("-", " ");
					thisArgs[i] = thisArgs[i].toLowerCase();
					thisArgs[i] = Util.getLemma(thisArgs[i]);
				}

				boolean thisMatch = thisArgs[0].length() > 0 && thisArgs[1].length() > 0
						&& ((arg1.contains(thisArgs[0]) && arg2.contains(thisArgs[1]))
								|| (arg1.contains(thisArgs[1]) && arg2.contains(thisArgs[0])));

				if (thisMatch) {
					if (debug) {
						System.out.println("matched for: " + cand + " " + arg1 + " " + arg2+" "+thisArgs[0]);
					}
				} else {
					if (debug) {
						System.out.println("nope: " + cand + " " + arg1 + " " + arg2+" "+thisArgs[0]);
					}
				}

				if (thisDSStr.equals("")) {
					thisDSStr = cand;
					argsMatch = thisMatch;
				} else {
					if (debug) {
						System.out.println("matched for second time");
					}
					if (argsMatch == thisMatch) {// for both cases, it matches
													// or not!
						// if (cand.length() > thisDSStr.length()) {
						// thisDSStr = cand;
						// }

						thisDSStr += "$$" + cand;

					} else {
						if (thisMatch) {// previosly didn't match, this is the
										// first match!
							thisDSStr = cand;
							argsMatch = thisMatch;
						}
					}
				}
			}

			boolean thisPartlyMatch = false;

			if (argsMatch && thisDSStr.length() > 0) {

				if (predArgsStrs[3].equals("true")) {// not same indexes
					// System.out.println("weird: " + thisDSStr + " " + arg1 + "
					// " + arg2);
					return new String[] { thisDSStr, "true" };
				} else {
					thisPartlyMatch = true;
				}
				// else{
				// ret = thisDSStr;
				// }
			}

			if (ret.equals("") || (!partlyMatched && (thisPartlyMatch || thisDSStr.length() > ret.length()))
					|| (argsMatch && thisDSStr.length() > ret.length())) {
				ret = thisDSStr;// At least it's something, but if we find
				// a better one for another parse, we accept that one
			}

			partlyMatched = thisPartlyMatch || partlyMatched;

			if (predArgsStrs[3].equals("none")) {// already seen all the
													// syn trees.
				break;
			}
			syntaxIdx++;
		}
		if (!acceptNP) {
			if (debug) {
				System.out.println("not matched: " + text);
			}
		}
		return new String[] { ret, "false" };
	}

	public String[] extractPredArgsStrs(String text)
			throws ArgumentValidationException, IOException, InterruptedException {
		return extractPredArgsStrs(text, 0, false, false, null);
	}

	String[] getLeftRightPred(Edge<LexicalItem> edge) {
		String leftPred = edge.getRelation().getLeft().toString();
		if (lemmatizePred) {
			leftPred = leftPred.replace(edge.getMediator().getWord(), edge.getMediator().getLemma()).toLowerCase();
		}
		// System.out.println("leftPred: " + leftPred + " "
		// + leftPred.replaceFirst(edge.getMediator().getWord(),
		// edge.getMediator().getLemma()));
		String rightPred = edge.getRelation().getRight().toString();
		if (lemmatizePred) {
			rightPred = rightPred.replace(edge.getMediator().getWord(), edge.getMediator().getLemma()).toLowerCase();
		}
		// System.out.println("rightPred: " + rightPred + " "
		// + rightPred.replaceFirst(edge.getMediator().getWord(),
		// edge.getMediator().getLemma()));
		return new String[] { leftPred, rightPred };
	}

	// syntaxIdx means what syntactic parse we're interested in. Default is 0
	// (the best one), but sometimes we wanna look at others too!
	public String[] extractPredArgsStrs(String text, int syntaxIdx, boolean acceptNP, boolean acceptGG, List<List<LexicalGraph>> allGraphs)
			throws ArgumentValidationException, IOException, InterruptedException {
		String mainStr = "";
		String mainStrOnlyNEs = "";
		String dsStr = "";// For very simple sentences with only one expected
							// relation
		boolean foundInteresting = false;// true if argIdx!=eventIdx, ...

		// Gparser does the split itself
		// System.out.println("before: " + text);
		
		

		// // long t0 = System.currentTimeMillis();
		if (allGraphs==null) {
			text = Util.preprocess(text);
			String sentence = text;
			String mySent = "{\"sentence\" : \"" + sentence + "\"}";
			allGraphs = parser.processText(mySent);
		}
		// System.out.println("gparse time: "
		// + (System.currentTimeMillis() - t0));

		if (allGraphs.size() == 0) {
			return new String[] { "", "", "", "false" };
		}

		int relCount = 0;
		int sentIdx = 0;
		for (List<LexicalGraph> graphs : allGraphs) {
			if (graphs.size() > 0) {
				// System.out.println("syn ind: "+syntaxIdx + " "+
				// graphs.size());
				if (syntaxIdx >= graphs.size()) {
					return new String[] { "", "", "", "none" };
				}
				LexicalGraph ungroundedGraph = graphs.get(syntaxIdx);

				// System.out.println(ungroundedGraph.getSyntacticParse());

				// System.err.println("# Ungrounded Graphs" + "\n");

				Set<LexicalItem> nodes = ungroundedGraph.getNodes();
				HashMap<Integer, LexicalItem> idx2Node = new HashMap<>();

				for (LexicalItem node : nodes) {
					idx2Node.put(node.getWordPosition(), node);
					// System.out.println(node.getPos());
				}
				// mainStr += ungroundedGraph+"\n";
				// mainStr += ungroundedGraph.getSyntacticParse() + "\n";
				String syntacticParse = ungroundedGraph.getSyntacticParse();
				// System.out.println(syntacticParse);

				if (!acceptNP && syntacticParse.startsWith("(<T NP")) {
					// mainStr += "not interesting: " + "\n";
					continue;
				}

				HashMap<Integer, Edge<LexicalItem>> allEventIdxes = new HashMap<>();
				for (Edge<LexicalItem> edge : ungroundedGraph.getEdges()) {
					allEventIdxes.put(edge.getMediator().getWordPosition(), edge);
				}

				for (Edge<LexicalItem> edge : ungroundedGraph.getEdges()) {
					ArrayList<BinaryRelInfo> relInfos = new ArrayList<>();
					int eventIndex = edge.getMediator().getWordPosition();
					int arg1Index = edge.getLeft().getWordPosition();
					int arg2Index = edge.getRight().getWordPosition();

					// if (1==1){
					// String leftPred =
					// edge.getRelation().getLeft().toString();
					// String rightPred =
					// edge.getRelation().getRight().toString();
					// String arg1 = idx2Node.get(arg1Index).getLemma();
					// String arg2 = idx2Node.get(arg2Index).getLemma();
					// String predArgString = getPredArgString("", leftPred,
					// rightPred, arg1, arg2, false, eventIndex);
					// System.out.println(predArgString);
					// }

					int accepted = acceptableRelation(idx2Node.get(arg1Index), idx2Node.get(arg2Index), acceptGG);
					if (accepted < 0) {
						continue;
					}
					// System.out.println("accepted: "+accepted);
					// Is negated?
					boolean negated = false;
					LexicalItem eventItem = edge.getMediator();
					Set<Property> eventProperties = ungroundedGraph.getProperties(eventItem);
					if (eventProperties != null) {
						for (Property p : eventProperties) {
							// System.out.println(p.getPropertyName());
							if (p.getPropertyName().equals("NEGATION")) {
								negated = true;
							}
						}
					}

					String[] lr = getLeftRightPred(edge);
					String leftPred = lr[0];
					String rightPred = lr[1];

					if ((leftPred.contains(".'s.") && !rightPred.contains(".'s."))
							|| (rightPred.contains(".'s.") && !leftPred.contains(".'s."))) {
						if (eventIndex == arg1Index || eventIndex == arg2Index) {
							continue;
						}
					}

					// System.out.println("main word: "+
					// edge.getMediator().getWord());

					// let's fix the particle verbs
					String[] eventTypeStr = getEventTypeStr(ungroundedGraph, idx2Node, eventIndex);
					String eventTypeStrParticle = eventTypeStr[0];
					String eventTypeStrNeg = eventTypeStr[1];
					if (eventTypeStrNeg.equals("NEG")) {
						negated = !negated;
					}
					if (!eventTypeStrParticle.equals("")) {
						String eventStr = edge.getMediator().getWord();
						// mainStr += "replacing: " + eventStr + " " +
						// eventTypeStr + "\n";
						leftPred = leftPred.replace(eventStr, eventTypeStrParticle);
						rightPred = rightPred.replace(eventStr, eventTypeStrParticle);
					}

					// Let's give these also a chance for now! But I guess
					// they'll have a lot of false positive
					// if (eventIndex == arg1Index || eventIndex ==
					// arg2Index
					// || arg1Index == arg2Index){
					// continue;
					// }

					String arg1 = idx2Node.get(arg1Index).getLemma();
					String arg2 = idx2Node.get(arg2Index).getLemma();

					// //41 shots -> shots
					//
					// if (idx2Node.get(arg1Index).getPos().equals("CD")) {
					// String prev = arg1;
					// String head = getHeadForCounts(ungroundedGraph,
					// idx2Node, arg1Index);
					// if (head!=null){
					// arg1 = Util.getLemma(head);
					// if (!arg1.equals(prev)){
					// mainStr += ("CD, was "+prev)+"\n";
					// mainStr += ("now: "+arg1)+"\n";
					// }
					// }
					//
					// }
					//
					// if (idx2Node.get(arg2Index).getPos().equals("CD")) {
					// String prev = arg2;
					// String head = getHeadForCounts(ungroundedGraph,
					// idx2Node, arg2Index);
					// if (head!=null){
					// arg2 = Util.getLemma(head);
					// if (!arg2.equals(prev)){
					// mainStr += ("CD, was "+prev)+"\n";
					// mainStr += ("now: "+arg2)+"\n";
					// }
					// }
					//
					// }

					String modifierStr = getModifierStr(ungroundedGraph, idx2Node, eventIndex);

					// Check passive!
					String[] ss = makeActive(leftPred, rightPred);
					leftPred = ss[0];
					rightPred = ss[1];

					boolean swapped = !(leftPred.compareTo(rightPred) < 0);

					String predArgStr = getPredArgString("", leftPred, rightPred, arg1, arg2, negated, eventIndex);
					// Now, we have accepted, arg1(Idx), arg2(Idx), predArgStr,
					// swapped

					// adding!
					BinaryRelInfo relInfo0 = getBinaryRelInfo(arg1, arg2, predArgStr, swapped, arg1Index, arg2Index,
							eventIndex, accepted, dsStr.length() > 0, idx2Node, sentIdx);
					// addRelInfo(relInfos, relInfo0, currentArgIdxPairs, arg1Index, arg2Index,
					// true);
					relInfos.add(relInfo0);
					if (!modifierStr.equals("")) {
						predArgStr = getPredArgString("", leftPred, rightPred, arg1, arg2, negated, eventIndex);
						relInfo0 = getBinaryRelInfo(arg1, arg2, predArgStr, swapped, arg1Index, arg2Index, eventIndex,
								accepted, dsStr.length() > 0, idx2Node, sentIdx);
						// addRelInfo(relInfos, relInfo0, currentArgIdxPairs, arg1Index, arg2Index,
						// true);
						relInfos.add(relInfo0);
					}

					// adding

					// Now, check if we find a two-hop relation:
					// (receives.1,receives.2) Libya help
					// (help.from.1,help.from.2) help UN
					// ==> (receives.1,receives.help.from.2) Libya UN

					// Let's swap these (this could have logically be swapped in
					// advance, but we just didn't need it!
					if (swapped) {
						String tmp = arg1;
						arg1 = arg2;
						arg2 = tmp;

						tmp = leftPred;
						leftPred = rightPred;
						rightPred = tmp;

						int tmpi = arg1Index;
						arg1Index = arg2Index;
						arg2Index = tmpi;
					}

					twoHopNP(relInfos, idx2Node, ungroundedGraph.getEdges(), modifierStr, arg1, negated, arg1Index,
							arg2Index, leftPred, rightPred, eventIndex, dsStr, acceptGG, sentIdx);
					twoHopVP(relInfos, idx2Node, ungroundedGraph.getEdges(), modifierStr, arg1, negated, arg1Index,
							arg2Index, leftPred, rightPred, eventIndex, dsStr, acceptGG, sentIdx);

					for (BinaryRelInfo relInfo : relInfos) {
						if (!acceptGG && !acceptableGEStrs.contains(relInfo.GEStr)) {
							continue;
						}

						// if (eventIndex == arg1Index || eventIndex ==
						// arg2Index || arg1Index == arg2Index) {
						//
						// }

						if (relInfo.foundInteresting || !notReallyInteresting(relInfo.mainStr)) {
							mainStr += relInfo.mainStr;
							mainStrOnlyNEs += relInfo.mainStrOnlyNEs;
						}

						dsStr += relInfo.dsStr + "\n";
						foundInteresting = foundInteresting || relInfo.foundInteresting;
					}

				} // end edge
					// System.out.println(mainStr);
			}

			sentIdx++;
		}

		// if (relCount>1){
		// System.out.println("relCount: "+relCount+" "+text+" \n "+mainStr);
		// }

		String[] ret = new String[] { mainStr, mainStrOnlyNEs, dsStr, foundInteresting + "" };
		return ret;
	}

	// (payment.in.1,payment.in.2) payment paris 3: true
	// (with.1,with.2) richards suplex 0: false
	boolean notReallyInteresting(String mainStr) {
		try {
			String[] ss = mainStr.split(" ");
			String pred = ss[0].substring(1, ss[0].length() - 1);
			String cand = pred.split(",")[0].split("\\.")[0];
			if (cand.startsWith(ss[1]) || cand.startsWith(ss[2])) {
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

	}

	// add two-hops with PP attached to VP
	// Bangladesh maintains relations with Japan.
	// (1,0,2) (maintains.1,maintains.2):0.0
	// (1,0,4) (maintains.1,maintains.with.2):0.0
	// (1,4,2) (maintains.with.2,maintains.2):0.0
	// =>(maintains.1,maintains.relationships.with.2)
	void twoHopVP(ArrayList<BinaryRelInfo> relInfos, HashMap<Integer, LexicalItem> idx2Node,
			Set<Edge<LexicalItem>> allEdges, String modifierStr, String arg1, boolean negated, int arg1Index,
			int arg2Index, String leftPred, String rightPred, int eventIndex, String dsStr, boolean acceptGG,
			int sentIdx) {
		HashSet<String> addedPredArgStrs = new HashSet<>();

		if (eventIndex == arg1Index || eventIndex == arg2Index || arg1Index == arg2Index) {
			return;
		}

		try {
			if (!leftPred.endsWith(".1") || !rightPred.endsWith(".2") || !leftPred.substring(0, leftPred.length() - 2)
					.equals(rightPred.substring(0, rightPred.length() - 2))) {
				return;
			}

		} catch (Exception e) {
			return;
		}

		// We're sure, it's X.1,X.2
		// We're looking for X.2,X.sth.2 or X.sth.2,X.2
		String X = leftPred.substring(0, leftPred.length() - 2);
		if (X.contains(".")) {
			return;// still problem!
		}
		for (Edge<LexicalItem> edge2 : allEdges) {
			int eventIdx2 = edge2.getMediator().getWordPosition();
			if (eventIdx2 != eventIndex) {
				continue;
			}
			int lidx = edge2.getLeft().getWordPosition();
			int ridx = edge2.getRight().getWordPosition();

			String[] lr = getLeftRightPred(edge2);
			String lStr = lr[0];
			String rStr = lr[1];

			String arg2 = "";
			int thisArg2Index = -1;
			// ?X.2,X.sth.2
			if (lidx == arg2Index && ridx != arg1Index && !lStr.equals(rStr) && lStr.equals(rightPred)
					&& rStr.startsWith(X) && rStr.endsWith(".2")) {
				// System.err.println("lstr: "+lStr + " "+rStr +" "+
				// idx2Node.get(arg2Index).getLemma() + " "+X);
				rightPred = X + "." + idx2Node.get(arg2Index).getLemma() + rStr.substring(X.length());
				arg2 = edge2.getRight().getLemma();
				thisArg2Index = ridx;
			}
			// ?X.sth.2, X.2
			else if (ridx == arg2Index && lidx != arg1Index && !lStr.equals(rStr) && rStr.equals(rightPred)
					&& lStr.startsWith(X) && lStr.endsWith(".2")) {
				// System.err.println("lstr: "+lStr + " "+rStr +" "+
				// idx2Node.get(arg2Index).getLemma() + " "+X);
				rightPred = X + "." + idx2Node.get(arg2Index).getLemma() + lStr.substring(X.length());
				arg2 = edge2.getLeft().getLemma();
				thisArg2Index = lidx;
			} else {
				continue;
			}

			int accepted = acceptableRelation(idx2Node.get(arg1Index), idx2Node.get(thisArg2Index), acceptGG);

			if (accepted < 0) {
				continue;
			}

			boolean swapped = !(leftPred.compareTo(rightPred) < 0);

			String predArgStr = getPredArgString("", leftPred, rightPred, arg1, arg2, negated, eventIdx2);
			if (addedPredArgStrs.contains(predArgStr)) {
				continue;
			}
			addedPredArgStrs.add(predArgStr);
			// System.out.println("added new relation (VP): " + predArgStr);
			BinaryRelInfo relInfo0 = getBinaryRelInfo(arg1, arg2, predArgStr, swapped, arg1Index, thisArg2Index,
					eventIndex, accepted, dsStr.length() > 0, idx2Node, sentIdx);
			// addRelInfo(relInfos, relInfo0, currentArgIdxPairs, arg1Index, thisArg2Index,
			// false);
			relInfos.add(relInfo0);
			// System.out.println("added relInfo twohop vp: "+relInfo0.mainStr);

			if (!modifierStr.equals("")) {
				predArgStr = getPredArgString(modifierStr, leftPred, rightPred, arg1, arg2, negated, eventIdx2);
				// System.out.println("added new relation (VP): " + predArgStr);
				relInfo0 = getBinaryRelInfo(arg1, arg2, predArgStr, swapped, arg1Index, thisArg2Index, eventIndex,
						accepted, dsStr.length() > 0, idx2Node, sentIdx);
				// addRelInfo(relInfos, relInfo0, currentArgIdxPairs, arg1Index, thisArg2Index,
				// false);
				relInfos.add(relInfo0);
			}
		}

	}

	// add two-hops with PP attached to NP
	// Libya receives help from the United Nations.
	// (2,5,2) (help.from.2,help.from.1):0.0
	// (1,0,2) (receives.1,receives.2):0.0
	void twoHopNP(ArrayList<BinaryRelInfo> relInfos, HashMap<Integer, LexicalItem> idx2Node,
			Set<Edge<LexicalItem>> allEdges, String modifierStr, String arg1, boolean negated, int arg1Index,
			int arg2Index, String leftPred, String rightPred, int eventIndex, String dsStr, boolean acceptGG,
			int sentIdx) {
		HashSet<String> addedPredArgStrs = new HashSet<>();
		if (eventIndex == arg1Index || eventIndex == arg2Index || arg1Index == arg2Index) {
			return;
		}
		// if (!leftPred.endsWith(".1")) {
		// return;
		// }

		for (Edge<LexicalItem> edge2 : allEdges) {
			int eventIdx2 = edge2.getMediator().getWordPosition();
			if (eventIdx2 != arg2Index) {
				continue;
			}
			String thisRightPred = rightPred;
			int lastRightDot = thisRightPred.lastIndexOf('.');
			thisRightPred = thisRightPred.substring(0, lastRightDot + 1);
			String arg2 = "";
			int thisArg2Index = -1;
			String[] lr = getLeftRightPred(edge2);
			boolean shouldAdd = true;
			if (edge2.getLeft().getWordPosition() == arg2Index) {
				thisRightPred += lr[1];
				try {
					if (Util.prepositions.contains(lr[1].split("\\.")[0])) {
						shouldAdd = false;
					}
				} catch (Exception e) {
				}

				arg2 = edge2.getRight().getLemma();
				thisArg2Index = edge2.getRight().getWordPosition();
			} else if (edge2.getRight().getWordPosition() == arg2Index) {
				thisRightPred += lr[0];
				try {
					if (Util.prepositions.contains(lr[0].split("\\.")[0])) {
						shouldAdd = false;
					}
				} catch (Exception e) {
				}

				arg2 = edge2.getLeft().getLemma();
				thisArg2Index = edge2.getLeft().getWordPosition();
			} else {
				// System.out.println("argIdx=eventIdx but it wasn't
				// enough!");
				continue;
			}

			int accepted = acceptableRelation(idx2Node.get(arg1Index), idx2Node.get(thisArg2Index), acceptGG);
			if (accepted < 0) {
				continue;
			}

			boolean swapped = !(leftPred.compareTo(thisRightPred) < 0);

			String predArgStr = getPredArgString("", leftPred, thisRightPred, arg1, arg2, negated, eventIdx2);
			if (addedPredArgStrs.contains(predArgStr)) {
				continue;
			}
			addedPredArgStrs.add(predArgStr);
			// System.out.println("added new relation: " + predArgStr);
			BinaryRelInfo relInfo0 = getBinaryRelInfo(arg1, arg2, predArgStr, swapped, arg1Index, thisArg2Index,
					eventIndex, accepted, dsStr.length() > 0, idx2Node, sentIdx);
			// addRelInfo(relInfos, relInfo0, currentArgIdxPairs, arg1Index, thisArg2Index,
			// false);
			if (shouldAdd) {
				relInfos.add(relInfo0);
//				System.out.println("added relInfo twohop np: " + relInfo0.mainStr);
//				System.out.println(edge2.getMediator().getLemma() + " " + edge2.getMediator().getPos() + " "
//						+ lr[0].equals(lr[1]) + " " + lr[0] + " " + lr[1]);
			} else {
				// System.out.println("not adding: "+relInfo0.mainStr);
			}

			if (!modifierStr.equals("")) {
				predArgStr = getPredArgString(modifierStr, leftPred, thisRightPred, arg1, arg2, negated, eventIdx2);
				// System.out.println("added new relation: " + predArgStr);
				relInfo0 = getBinaryRelInfo(arg1, arg2, predArgStr, swapped, arg1Index, thisArg2Index, eventIndex,
						accepted, dsStr.length() > 0, idx2Node, sentIdx);
				// addRelInfo(relInfos, relInfo0, currentArgIdxPairs, arg1Index, thisArg2Index,
				// false);
				relInfos.add(relInfo0);
			}

		}

	}

	// X.2,X.by.2 or X.by.2,X.2
	String[] makeActive(String leftPred, String rightPred) {
		if (leftPred.endsWith(".by.2")) {
			int idx = leftPred.lastIndexOf(".by.2");
			String X = leftPred.substring(0, idx);
			if (rightPred.equals(X + ".2")) {
				// System.out.println("making active: " + leftPred + " " +
				// rightPred);
				leftPred = X + ".1";
			}
		} else if (rightPred.endsWith(".by.2")) {
			int idx = rightPred.lastIndexOf(".by.2");
			String X = rightPred.substring(0, idx);
			if (leftPred.equals(X + ".2")) {
				// System.out.println("making active: " + leftPred + " " +
				// rightPred);
				rightPred = X + ".1";
			}
		}
		return new String[] { leftPred, rightPred };
	}

	BinaryRelInfo getBinaryRelInfo(String arg1, String arg2, String predArgStr, boolean swapped, int arg1Index,
			int arg2Index, int eventIndex, int accepted, boolean foundNonTrivalDSStr,
			HashMap<Integer, LexicalItem> idx2Node, int sentIdx) {

		String GEStr = "";

		if (accepted == 3) {
			GEStr = "GG";
		} else if (accepted == 2) {
			GEStr = "EE";
		} else {
			if ((accepted == 0 && !swapped) || (accepted == 1 && swapped)) {
				GEStr = "EG";
			} else {
				GEStr = "GE";
			}
		}

		addEnts(arg1Index, arg2Index, idx2Node);
		addGens(accepted, arg1, arg2);
		BinaryRelInfo relInfo = new BinaryRelInfo();
		relInfo.GEStr = GEStr;
		relInfo.mainStr += predArgStr + " " + GEStr + " " + sentIdx + "\n";

		if (accepted == 2) {
			relInfo.mainStrOnlyNEs += predArgStr + " " + sentIdx + "\n";
		}
		relInfo.dsStr = "";
		if (eventIndex == arg1Index || eventIndex == arg2Index || arg1Index == arg2Index) {
			if (!foundNonTrivalDSStr) {
				relInfo.dsStr = predArgStr + " " + GEStr;
			}
			// System.out.println("not interesting: " + eventIndex + " " + arg1Index + "" +
			// arg2Index + " " + arg1 + " "
			// + arg2 + " " + predArgStr);
		} else {
			relInfo.foundInteresting = true;
			relInfo.dsStr = predArgStr + " " + GEStr;

		}
		// if (relInfo.dsStr.equals("")) {
		// System.out.println("no ds str: " + predArgStr + " " + arg1 + " " +
		// arg2);
		// }
		return relInfo;
	}

	// used for particle verbs
	String[] getEventTypeStr(LexicalGraph ungroundedGraph, HashMap<Integer, LexicalItem> idx2Node, int eventIndex) {
		String eventTypeStrParticle = "";
		String eventTypeStrNegation = "";
		if (idx2Node.get(eventIndex).getPos().startsWith("VB")) {
			TreeSet<Type<LexicalItem>> thisEventTypes = ungroundedGraph.getEventTypes(idx2Node.get(eventIndex));
			if (thisEventTypes != null) {
				for (Type<LexicalItem> type : thisEventTypes) {
					if (type.getEntityType().toString().contains("never")) {
						eventTypeStrNegation = "NEG";
					}
					if (!type.getModifierNode().getPos().equals("RP")) {
						continue;
					} else {
						eventTypeStrParticle = type.getEntityType().toString();
						eventTypeStrParticle = eventTypeStrParticle.substring(0, eventTypeStrParticle.length() - 4);
						break;
					}
				}
			}

		}
		return new String[] { eventTypeStrParticle, eventTypeStrNegation };
	}

	// 41 shots: shots
	String getHeadForCounts(LexicalGraph ungroundedGraph, HashMap<Integer, LexicalItem> idx2Node, int argIndex) {

		TreeSet<Type<LexicalItem>> thisEventTypes = ungroundedGraph.getTypes(idx2Node.get(argIndex));
		String ret = null;

		if (thisEventTypes == null) {
			return ret;
		}
		for (Type<LexicalItem> type : thisEventTypes) {
			if (!type.getModifierNode().getPos().startsWith("NN")) {
				continue;
			} else {
				ret = type.getEntityType().toString();
				String tmp = ret;
				int colonIdx = ret.indexOf(':');

				if (colonIdx > 0) {
					ret = tmp.substring(0, colonIdx);
				}

				int dotIdx = ret.indexOf('.');
				if (dotIdx > 0 && dotIdx < colonIdx) {
					ret = tmp.substring(0, dotIdx);
				}
			}
		}

		return ret;
	}

	String getModifierStr(LexicalGraph ungroundedGraph, HashMap<Integer, LexicalItem> idx2Node, int eventIndex) {
		HashMap<LexicalItem, Set<Type<LexicalItem>>> modifiedBy = getModifiedBy(ungroundedGraph);
		String modifierStr = "";
		LexicalItem eventItem = idx2Node.get(eventIndex);

		if (modifiedBy.containsKey(eventItem)) {
			Type<LexicalItem> modifier = null;
			for (Type<LexicalItem> thisModifier : modifiedBy.get(eventItem)) {
				LexicalItem modiferNode = thisModifier.getParentNode();
				// mainStr += thisModifier;
				// mainStr += ("mnode: "+modiferNode);
				if (modiferNode.getPos().startsWith("VB") && modiferNode.getWordPosition() != eventIndex) {
					modifier = thisModifier;
					break;
				}
			}
			if (modifier != null) {
				modifierStr = modifier.getEntityType().toString();
				modifierStr = modifierStr.substring(0, modifierStr.length() - 6);
			}
		}
		return modifierStr;
	}

	HashMap<LexicalItem, Set<Type<LexicalItem>>> getModifiedBy(LexicalGraph ungroundedGraph) {
		HashMap<LexicalItem, Set<Type<LexicalItem>>> modifiedBy = new HashMap<>();
		Map<LexicalItem, TreeSet<Type<LexicalItem>>> eventEventModifiers = ungroundedGraph.getEventEventModifiers();
		for (LexicalItem modifier : eventEventModifiers.keySet()) {
			// System.out.println("modifier: " + modifier);
			Set<Type<LexicalItem>> modifieds = eventEventModifiers.get(modifier);
			if (modifieds != null) {
				for (Type<LexicalItem> tModifieds : modifieds) {
					LexicalItem modified = tModifieds.getModifierNode();
					if (!modifiedBy.containsKey(modified)) {
						modifiedBy.put(modified, new HashSet<>());
					}
					modifiedBy.get(modified).add(tModifieds);
					// System.out.println("modified: " + modified);
					// System.out.println("adding: " + tModifieds);
				}
			}
		}
		return modifiedBy;
	}

	void addGens(int accepted, String arg1, String arg2) {
		// add to allGens
		if (accepted == 0) {
			if (!LinesHandler.allGens.containsKey(arg2)) {
				LinesHandler.allGens.put(arg2, 0);
			}
			LinesHandler.allGens.put(arg2, LinesHandler.allGens.get(arg2) + 1);
		} else if (accepted == 1) {

			if (!LinesHandler.allGens.containsKey(arg1)) {
				LinesHandler.allGens.put(arg1, 0);
			}
			LinesHandler.allGens.put(arg1, LinesHandler.allGens.get(arg1) + 1);

		}
	}

	void addEnts(int arg1Index, int arg2Index, HashMap<Integer, LexicalItem> idx2Node) {
		if (isEntity(idx2Node.get(arg1Index).getPos())) {
			String spot = idx2Node.get(arg1Index).getWord();
			spot = spot.replace("_", " ");
			if (!LinesHandler.allEntities.containsKey(spot)) {
				LinesHandler.allEntities.put(spot, 0);
			}
			LinesHandler.allEntities.replace(spot, LinesHandler.allEntities.get(spot) + 1);
		}

		if (isEntity(idx2Node.get(arg2Index).getPos())) {
			String spot = idx2Node.get(arg2Index).getWord();
			spot = spot.replace("_", " ");
			if (!LinesHandler.allEntities.containsKey(spot)) {
				LinesHandler.allEntities.put(spot, 0);
			}
			LinesHandler.allEntities.put(spot, LinesHandler.allEntities.get(spot) + 1);
		}
		// if (LinesHandler.convToEntityLinked) {
		// if (isEntity(idx2Node.get(arg1Index))) {
		// String spot = idx2Node.get(arg1Index).getWord();
		// // String wikiName = getWikiNamedEntity(spot,
		// // LinesHandler.client);
		// // spotsStr.add(spot);
		// // spotsWikiEnt.add(wikiName);
		// entsSet.add(spot);
		// }
		//
		// if (isEntity(idx2Node.get(arg2Index))) {
		// String spot = idx2Node.get(arg2Index).getWord();
		// // String wikiName = getWikiNamedEntity(spot,
		// // LinesHandler.client);
		// // spotsStr.add(spot);
		// // spotsWikiEnt.add(wikiName);
		// entsSet.add(spot);
		// }
		// }
	}

	String getPredArgString(String modifierStr, String leftPred, String rightPred, String arg1, String arg2,
			boolean negated, int eventIdx) {
		String[] lr = postProcessPossessive(leftPred, rightPred);
		leftPred = lr[0];
		rightPred = lr[1];

		if (leftPred.compareTo(rightPred) < 0) {
			String predicate = "(" + leftPred + "," + rightPred + ")";
			if (!modifierStr.equals("")) {
				predicate = modifierStr + "__" + predicate;
			}
			if (negated) {
				predicate = "NEG" + "__" + predicate;
			}
			return predicate + " " + arg1 + " " + arg2 + " " + eventIdx;
		} else {
			String predicate = "(" + rightPred + "," + leftPred + ")";
			if (!modifierStr.equals("")) {
				predicate = modifierStr + "__" + predicate;
			}
			if (negated) {
				predicate = "NEG" + "__" + predicate;
			}
			return predicate + " " + arg2 + " " + arg1 + " " + eventIdx;
		}
	}

	private String[] postProcessPossessive(String l, String r) {
		if (l.endsWith(".'s.1") && r.endsWith(".'s.2")) {
			return new String[] { "'s.1", "'s.2" };
		} else if (l.endsWith(".'s.2") && r.endsWith(".'s.1")) {
			return new String[] { "'s.2", "'s.1" };
		} else if (l.endsWith(".'.1") && r.endsWith(".'.2")) {
			return new String[] { "'.1", "'.2" };
		} else if (l.endsWith(".'.2") && r.endsWith(".'.1")) {
			return new String[] { "'.2", "'.1" };
		}
		return new String[] { l, r };
	}

	// public static ArrayList<String> parseToSents(String text) {
	// text = text.replace("\n", " ");
	// // text = text.replace(":", ".");
	// // text = text.replace("-", ".");
	// ArrayList<String> ret = new ArrayList<>();
	//
	// // read some text in the text variable
	//
	// // create an empty Annotation just with the given text
	// Annotation document = new Annotation(text);
	//
	// // run all Annotators on this text
	// pipeline.annotate(document);
	//
	// // these are all the sentences in this document
	// // a CoreMap is essentially a Map that uses class objects as keys and
	// // has values with custom types
	// List<CoreMap> sentences = document.get(SentencesAnnotation.class);
	// // System.out.println("sentences");
	// for (CoreMap sentence : sentences) {
	// // traversing the words in the current sentence
	// // a CoreLabel is a CoreMap with additional token-specific methods
	// // System.out.println(sentence);
	// ret.add(sentence.toString());
	//
	// // for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
	// // // this is the text of the token
	// // String word = token.get(TextAnnotation.class);
	// // System.out.println(word);
	// // // this is the POS tag of the token
	// // String pos = token.get(PartOfSpeechAnnotation.class);
	// // System.out.println(pos);
	// // // this is the NER label of the token
	// // String ne = token.get(NamedEntityTagAnnotation.class);
	// // System.out.println(ne);
	// // }
	// //
	// // // this is the parse tree of the current sentence
	// // Tree tree = sentence.get(TreeAnnotation.class);
	// // System.out.println("tree: " + tree);
	// //
	// // // this is the Stanford dependency graph of the current sentence
	// // SemanticGraph dependencies = sentence
	// // .get(CollapsedCCProcessedDependenciesAnnotation.class);
	// // System.out.println("dependencies: " + dependencies);
	//
	// }
	//
	// // This is the coreference link graph
	// // Each chain stores a set of mentions that link to each other,
	// // along with a method for getting the most representative mention
	// // Both sentence and token offsets start at 1!
	// // Map<Integer, CorefChain> graph = document
	// // .get(CorefChainAnnotation.class);
	// //
	// // System.out.println("chains: " + graph);
	// return ret;
	// }

	// Use linux split instead!
	// static void breakFile() {
	// String root = "";
	// BufferedReader br = null;
	//
	// int numFiles = 20;
	// PrintStream[] ops = new PrintStream[numFiles];
	// try {
	// br = new BufferedReader(new InputStreamReader(new FileInputStream(root +
	// "news.txt")));
	// // op1 = new PrintStream(new File("news1.txt"));
	// // op2 = new PrintStream(new File("news2.txt"));
	//
	// for (int i = 0; i < numFiles; i++) {
	// ops[i] = new PrintStream(new File("news" + i + ".txt"));
	// }
	//
	// } catch (FileNotFoundException e) {
	// e.printStackTrace();
	// }
	// int numLines = 12454137;
	// int numLinePerFile = numLines / numFiles;
	// int lineNumber = 0;
	// String l;
	// try {
	// while ((l = br.readLine()) != null) {
	// // String l = sc.nextLine();
	// int fileNum = lineNumber / numLinePerFile;
	// fileNum = Math.min(fileNum, numFiles - 1);
	// ops[fileNum].println(l);
	// lineNumber++;
	// }
	// } catch (IOException e) {
	// e.printStackTrace();
	// }
	// }

	public static void main(String[] args) throws ArgumentValidationException, IOException, InterruptedException {
		PredicateArgumentExtractor prEx = new PredicateArgumentExtractor("");
//		String s = "Barack Obama is not against all wars.";
		String s = "place is celebrating event";
//		String s = "location_1 be combined with location_2";
//		String s = "drug_1 should be taken by drug_2";
//		String s = "disease is increasing in country";
		String[] exPrss = prEx.extractPredArgsStrs(s, 0, true, true, null);
		String mainRels = exPrss[0];
		System.out.println(mainRels);
	}

	public static boolean isEntity(String pos) {
		// System.out.println(node.getLemma());
		// System.out.println(node.getPos());
		return pos.equals("NNP") || pos.equals("NNPS");
	}

	// is noun or CD (which hopefully the CD modifies a noun).
	public static boolean isNoun(String pos) {
		// System.out.println(node.getLemma());
		// System.out.println(node.getPos());
		return pos.equals("NN") || pos.equals("NNS") || pos.equals("CD");
	}

	// -1: not good
	// 0: E G
	// 1: G E
	// 2: E E
	// 3: G G
	static int acceptableRelation(LexicalItem node1, LexicalItem node2, boolean acceptGG) {
		if (!acceptGG) {
			if (isEntity(node1.getPos()) && isEntity(node2.getPos())) {// E E
				return 2;
			} else if (isEntity(node1.getPos()) && isNoun(node2.getPos())) {
				return 0;
			} else if (isEntity(node2.getPos()) && isNoun(node1.getPos())) {
				return 1;
			} else {
				return -1;
			}
		} else {
			if (isEntity(node1.getPos()) && isEntity(node2.getPos())) {// E E
				return 2;
			} else if (isEntity(node1.getPos())) {
				return 0;
			} else if (isEntity(node2.getPos())) {
				return 1;
			} else {
				return 3;
			}
		}

	}

}

class BinaryRelInfo {
	String mainStr = "";
	String mainStrOnlyNEs = "";
	String dsStr;
	boolean foundInteresting = false;
	String GEStr;
}