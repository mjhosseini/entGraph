package entailment;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ArrayUtils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import constants.ConstantsParsing;
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
		acceptableGEStrs = new HashSet<>();
		for (String s : ConstantsParsing.accepteds) {
			acceptableGEStrs.add(s);
		}
	}

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

			if (!ConstantsParsing.writeDebugString) {
				mainStr += "#lineId: " + lineId + "\n";
				mainStr += "#articleId: " + articleId + "\n";
				mainStr += "#date: " + date + "\n";
			}

			mainStrOnlyNEs = mainStr;

			String[] predArgStrs = extractPredArgsStrs(text);
			mainStr += predArgStrs[0];
			if (ConstantsParsing.writeUnaryRels && !predArgStrs[4].equals("")) {
				mainStr += "#unary rels:\n";
				mainStr += predArgStrs[4];
			}
			mainStrOnlyNEs += predArgStrs[1];

		} catch (Exception e) {
			System.err.println("exception for " + line);
			e.printStackTrace();
		}

		LinesHandler.mainStrs.add(mainStr);
		LinesHandler.mainStrsOnlyNEs.add(mainStrOnlyNEs);

		// System.out.println(mainStr);

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

	public String extractPredArgsStrsForceFinding(String text, String arg1, String arg2, boolean longestRel,
			boolean debug) throws ArgumentValidationException, IOException, InterruptedException {
		// parser.nbestParses = 10;
		String[] ret = extractPredArgsStrsForceFinding(text, arg1, arg2, true, longestRel, debug);
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
	public String[] extractPredArgsStrsForceFinding(String text, String arg1, String arg2, boolean acceptNP,
			boolean longestRel, boolean debug) throws ArgumentValidationException, IOException, InterruptedException {
		// System.out.println(text);
		String ret = "";
		int syntaxIdx = 0;
		boolean partlyMatched = false;

		text = Util.preprocess(text);
		System.out.println("preprocessed text: " + text);
		String sentence = text;
		String mySent = "{\"sentence\" : \"" + sentence + "\"}";
		List<List<LexicalGraph>> allGraphs = parser.processText(mySent);

		if (allGraphs.size() == 0) {
			return new String[] { ret, "false" };
		}

		// System.out.println("num syn parses: "+allGraphs.get(0).size());

		while (syntaxIdx < allGraphs.get(0).size()) {
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

			arg1 = Util.getLemma(arg1).replace("_", " ");// ADDED 14 Jan 18
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
						System.out.println("matched for: " + cand + " " + arg1 + " " + arg2 + " " + thisArgs[0]);
					}
				} else {
					if (debug) {
						System.out.println("nope: " + cand + " " + arg1 + " " + arg2 + " " + thisArgs[0]);
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
					if (longestRel) {// to be used for Zeichner's data that wants to trick with implicative verbs,
										// etc
						thisDSStr = getLongestRel(thisDSStr);
					}
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

		if (longestRel) {// to be used for Zeichner's data that wants to trick with implicative verbs,
							// etc
			ret = getLongestRel(ret);
		}

		return new String[] { ret, "false" };
	}

	public String getLongestRel(String ret) {
		String[] ss = ret.split("\\$\\$");
		System.out.println("num $$ split: " + ss.length);
		String l = ss[0];
		for (int i = 1; i < ss.length; i++) {
			if (ss[i].length() > l.length()) {
				System.out.println("using longer rel: " + ss[i]);
				l = ss[i];
			}
		}
		ret = l;
		return ret;
	}

	public String[] extractPredArgsStrs(String text)
			throws ArgumentValidationException, IOException, InterruptedException {
		return extractPredArgsStrs(text, 0, false, acceptableGEStrs.contains("GG"), null);
	}

	String[] getLeftRightPred(Edge<LexicalItem> edge) {
		String leftPred = edge.getRelation().getLeft().toString();
		if (ConstantsParsing.lemmatizePred) {
			leftPred = leftPred.replace(edge.getMediator().getWord(), edge.getMediator().getLemma()).toLowerCase();
		}

		// System.out.println("leftPred: " + leftPred + " "
		// + leftPred.replaceFirst(edge.getMediator().getWord(),
		// edge.getMediator().getLemma()));
		String rightPred = edge.getRelation().getRight().toString();
		if (ConstantsParsing.lemmatizePred) {
			rightPred = rightPred.replace(edge.getMediator().getWord(), edge.getMediator().getLemma()).toLowerCase();
		}

		// System.out.println("rightPred: " + rightPred + " "
		// + rightPred.replaceFirst(edge.getMediator().getWord(),
		// edge.getMediator().getLemma()));
		return new String[] { leftPred, rightPred };
	}

	// syntaxIdx means what syntactic parse we're interested in. Default is 0
	// (the best one), but sometimes we wanna look at others too!
	public String[] extractPredArgsStrs(String text, int syntaxIdx, boolean acceptNP, boolean acceptGG,
			List<List<LexicalGraph>> allGraphs) throws ArgumentValidationException, IOException, InterruptedException {
		String mainStr = "";
		String mainStrOnlyNEs = "";
		List<String> semanticParses = new ArrayList<>();
		String dsStr = "";// For very simple sentences with only one expected
							// relation
		boolean foundInteresting = false;// true if argIdx!=eventIdx, ...

		// Gparser does the split itself
		// System.out.println("before: " + text);

		// // long t0 = System.currentTimeMillis();

		if (allGraphs == null) {
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

		List<String> sentences = null;
		if (ConstantsParsing.writeDebugString) {
			sentences = Util.getSentences(text);
		}

		LinkedHashSet<String> unaryRels = new LinkedHashSet<>();
		Set<Integer> notInterestingEventIdxes = new HashSet<>();// e.g., cigarettes at the bar: cigarettes as an event

		for (List<LexicalGraph> graphs : allGraphs) {// each graph is for one sentence
			if (graphs.size() > 0) {

				// System.out.println("syn ind: "+syntaxIdx + " "+
				// graphs.size());
				if (syntaxIdx >= graphs.size()) {
					return new String[] { "", "", "", "none" };
				}

				LexicalGraph ungroundedGraph = graphs.get(syntaxIdx);

				if (ConstantsParsing.writeDebugString) {
					System.out.println(ungroundedGraph);

				}

				// System.out.println(ungroundedGraph.getSyntacticParse());

				// System.err.println("# Ungrounded Graphs" + "\n");

				Set<LexicalItem> nodes = ungroundedGraph.getNodes();

				// int i=0;
				// if (ConstantsParsing.writeDebugString) {
				// for (LexicalItem node: nodes) {
				// System.out.println(i+": "+node);
				// i++;
				// }
				// }

				HashMap<Integer, LexicalItem> idx2Node = new HashMap<>();

				for (LexicalItem node : nodes) {
					idx2Node.put(node.getWordPosition(), node);
					// System.out.println(node.getPos());
				}

				List<LexicalItem> actualNodes = ungroundedGraph.getActualNodes();
				// it should be that nodes are a subset of the actualNodes, but I won't risk it
				// since I used to work with nodes
				// and actual nodes are only needed for lemmatizing unary relations.
				Map<Integer, LexicalItem> idx2ActualNode = new HashMap<>();

				for (LexicalItem node : actualNodes) {
					idx2ActualNode.put(node.getWordPosition(), node);
				}

				// mainStr += ungroundedGraph+"\n";
				// mainStr += ungroundedGraph.getSyntacticParse() + "\n";
				String syntacticParse = ungroundedGraph.getSyntacticParse();

				Set<String> semanticParse = ungroundedGraph.getSemanticParse();
				// System.out.println(syntacticParse);
				// System.out.println(semanticParse);

				if (ConstantsParsing.writeDebugString) {
					mainStr += sentences.get(sentIdx);
					mainStr += "\nSyntactic Parse:\n";
					mainStr += syntacticParse + "\n\n";
					mainStr += "Semantic Parse:\n";
					mainStr += semanticParse + "\n\n";

				}

				if (ConstantsParsing.writeSemParse) {
					semanticParses.add(semanticParse + "");
				}

				boolean first = true;

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

					// if (LinesHandler.writeDebugString) {
					// System.out.println(edge.getMediator());
					// System.out.println(edge.getLeft());
					// System.out.println(edge.getRight());
					// }

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
					String[] eventTypeStr = getEventTypeStr(ungroundedGraph, idx2Node, eventIndex, edge);
					String eventTypeStrParticle = eventTypeStr[0];
					String eventTypeStrNeg = eventTypeStr[1];
					if (eventTypeStrNeg.equals("NEG")) {
						negated = !negated;
					}

					if (ConstantsParsing.writeDebugString) {
						System.out.println("negated: " + negated);
					}

					if (!eventTypeStrParticle.equals("")) {
						String eventStr = edge.getMediator().getWord();
						if (ConstantsParsing.lemmatizePred) {
							eventStr = edge.getMediator().getLemma();
						}

						// mainStr += "replacing: " + eventStr + " " +
						// eventTypeStr + "\n";
						// System.out.println("replacing: " + eventStr + " " + eventTypeStrParticle +
						// "\n");
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

					// if (LinesHandler.writeDebugString) {
					// System.out.println(arg1 + " " + arg2);
					// }

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

					BinaryRelInfo relInfo0;
					String predArgStr;
					if (modifierStr.equals("") || !ConstantsParsing.removebasicEvnetifEEModifer) {
						predArgStr = getPredArgString("", leftPred, rightPred, arg1, arg2, negated, eventIndex);
						// Now, we have accepted, arg1(Idx), arg2(Idx), predArgStr,
						// swapped

						// adding!
						relInfo0 = getBinaryRelInfo(arg1, arg2, predArgStr, swapped, arg1Index, arg2Index, eventIndex,
								accepted, dsStr.length() > 0, idx2Node, sentIdx);
						// addRelInfo(relInfos, relInfo0, currentArgIdxPairs, arg1Index, arg2Index,
						// true);
						relInfos.add(relInfo0);//
						// System.out.println("adding rel info0: "+relInfo0.mainStr);
					}

					if (!modifierStr.equals("")) {
						predArgStr = getPredArgString(modifierStr, leftPred, rightPred, arg1, arg2, negated,
								eventIndex);
						relInfo0 = getBinaryRelInfo(arg1, arg2, predArgStr, swapped, arg1Index, arg2Index, eventIndex,
								accepted, dsStr.length() > 0, idx2Node, sentIdx);
						// addRelInfo(relInfos, relInfo0, currentArgIdxPairs, arg1Index, arg2Index,
						// true);
						// System.out.println("adding rel info1: "+relInfo0.mainStr);
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
							if (ConstantsParsing.writeDebugString) {
								if (first) {
									mainStr += "binary rels:\n";
								}
							}

							first = false;

							mainStr += relInfo.mainStr;
							mainStrOnlyNEs += relInfo.mainStrOnlyNEs;
						} else {
							notInterestingEventIdxes.add(relInfo.eventIdx);
							if (ConstantsParsing.snli) {

								String rMainStr = postProcessSameIndexMainStr(relInfo.mainStr);
								if (rMainStr.equals(relInfo.mainStr)) {
									continue;
								} else {
									// System.out.println(relInfo.mainStr +" replaced with "+rMainStr);
								}

								relInfo.mainStr = rMainStr;

								if (ConstantsParsing.writeDebugString) {
									if (first) {
										mainStr += "binary rels:\n";
									}
								}

								first = false;

								mainStr += relInfo.mainStr;
								mainStrOnlyNEs += relInfo.mainStrOnlyNEs;
							}
						}

						dsStr += relInfo.dsStr + "\n";
						foundInteresting = foundInteresting || relInfo.foundInteresting;
					}

				} // end edge
					// System.out.println(mainStr);

				LinkedHashSet<String[]> thisUnaryRels = new LinkedHashSet<>();
				Map<String, Integer> eIdx2Count = new HashMap<>();

				for (String rel : semanticParse) {
					if (ConstantsParsing.writeDebugString) {
						System.out.println(rel);
					}

					if (!rel.contains(":e ,")) {// TODO: be careful, bug fix by removing || !rel.contains(":x)")
						continue;
					}

					// "arms.around.2 neck G", 2
					// where 11 is the event idx number
					String[] unaryRel = getUnariesFromSemParse(rel, idx2ActualNode, sentIdx, notInterestingEventIdxes);
					if (unaryRel != null) {
						thisUnaryRels.add(unaryRel);
						eIdx2Count.putIfAbsent(unaryRel[1], 0);
						eIdx2Count.put(unaryRel[1], eIdx2Count.get(unaryRel[1]) + 1);
					}
				}

				// just add the ones that aren't recoverable from binaries
				for (String[] unaryRel : thisUnaryRels) {
					if (eIdx2Count.get(unaryRel[1]) > 1) {// we'll see this in binary rels (maybe a refined version,
															// e.g., particle verbs)
						continue;
					}
					if (ConstantsParsing.writeDebugString) {
						System.out.println("adding unary rel: " + unaryRel[0] + " " + unaryRel[1] + ": "
								+ eIdx2Count.get(unaryRel[1]));
					}
					// System.out.println(semanticParse);
					unaryRels.add(unaryRel[0]);
				}

			} // end one sentence
			sentIdx++;
		}

		// if (relCount>1){
		// System.out.println("relCount: "+relCount+" "+text+" \n "+mainStr);
		// }

		
		Set<String> unaryRelsFromBinary = getUnaryRelsFromBinary(mainStr);
		for (String unaryRel : unaryRelsFromBinary) {
			
			if (ConstantsParsing.splitBinary2Unary) {
				if (!unaryRels.contains(unaryRel)) {
					unaryRels.add(unaryRel);
				}
			}
			else {
				//make sure splits are removed even if the index of the event is the same as arg.
				//example: The Avalanche were zero of four against the Coyotes. "against"
				if (unaryRels.contains(unaryRel)) {
					unaryRels.remove(unaryRel);
				}
			}			
		}
		// unaryRels.addAll(unaryRelsFromBinary);

		String unaryRelsStr = "";

		if (ConstantsParsing.writeDebugString) {
			System.out.println("unary rels: ");
		}
		for (String s : unaryRels) {
			unaryRelsStr += s + "\n";
			if (ConstantsParsing.writeDebugString) {
				System.out.println(s);
			}
		}

		if (ConstantsParsing.writeDebugString) {
			System.out.println("\n");
		}

		if (ConstantsParsing.writeSemParse) {
			mainStr += "semantic parses:\n";
			for (String s : semanticParses) {
				mainStr += s + "\n";
			}
		}

		if (ConstantsParsing.filterUntensed) {
			mainStr = filterUntensedRels(mainStr);
		}

		String[] ret = new String[] { mainStr, mainStrOnlyNEs, dsStr, foundInteresting + "", unaryRelsStr };
		return ret;

	}

	// Prevents complex tenses from being doubled. Eg returns only
	// [(receiving.2,receiving.will.1) gift obama 4 GE 0]
	// instead of also (receiving.1,receiving.2) obama gift 4 EG 0.
	// Then turns receiving.will.2, receiving.have.2 into receiving.will.have.2
	String filterUntensedRels(String mainStr) {
		System.out.println("Initial mainStr" + mainStr);

		String[] mainRels = mainStr.split("\n");
		String[] tenseSignals = new String[] { ".will", ".have", ".has", ".had", ".is", ".was" };

		Pattern p = Pattern.compile("(\\(.+\\.)([\\d]+)(,.+\\.)([\\d]+)\\) (\\S+) (\\S+)(.*)([EG])([EG])(.*)");

		for (String rel : mainRels) {
			if (rel.startsWith("semantic parses:")) {
				break;
			}
			if (stringContainsItemFromList(rel, tenseSignals)) {

				for (String str : tenseSignals) {
					rel = rel.replace(str, "");
				}

				Matcher m = p.matcher(rel);
				m.matches();
				String swappedRel = m.group(1) + m.group(4) + m.group(3) + m.group(2) + ") " + m.group(6) + " "
						+ m.group(5) + m.group(7) + m.group(9) + m.group(8) + m.group(10);
				if (Arrays.asList(mainRels).contains(swappedRel)) {
					mainRels = ArrayUtils.removeElement(mainRels, swappedRel);
				}

			}
		}
		mainStr = String.join("\n", mainRels);
		System.out.println("Final mainstr:\n" + mainStr);
		return mainStr;
	}

	public static boolean stringContainsItemFromList(String inputStr, String[] items) {
		return Arrays.stream(items).parallel().anyMatch(inputStr::contains);
	}

	Set<String> getUnaryRelsFromBinary(String mainStr) {
		String[] rels = mainStr.split("\n");
		LinkedHashSet<String> unaryRels = new LinkedHashSet<>();
		for (String rel : rels) {
			String[] thisUnaries = getUnariesFromBinary(rel);
			if (thisUnaries != null) {
				unaryRels.add(thisUnaries[0]);
				unaryRels.add(thisUnaries[1]);
			}
		}
		return unaryRels;

	}

	// A man is running fast.
	// [running.fast(4:s , 3:e), man(1:s , 1:x), running.1(3:e , 1:x)]
	// => running.1 man G

	// TODO: problem with "The cervical cancer vaccine , approved in 2006 , is
	// recommended for girls around 11 or 12 ."
	// twice approve.2
	String[] getUnariesFromSemParse(String rel, Map<Integer, LexicalItem> idx2Node, int sentIdx,
			Set<Integer> notInterestingEventIdxes) {

		int firstIdx = rel.indexOf("(") + 1;
		int lastIdx = rel.indexOf(":e ,");

		int eIdx = Integer.parseInt(rel.substring(firstIdx, lastIdx));
		// when we don't split binary to unary, we don't care at all about being
		// interested, which is only based on binaries
		if (ConstantsParsing.splitBinary2Unary && notInterestingEventIdxes.contains(eIdx)) {
			return null;
		}

		String pred = rel.substring(0, rel.indexOf("("));
		if (ConstantsParsing.lemmatizePred) {
			LexicalItem predNode = idx2Node.get(eIdx);
			pred = pred.replace(predNode.getWord(), predNode.getLemma()).toLowerCase();
		}

		firstIdx = rel.indexOf(", ") + 2;
		lastIdx = rel.lastIndexOf(":");

		int argIdx = Integer.parseInt(rel.substring(firstIdx, lastIdx));

		if (argIdx == eIdx) {
			return null;
		}

		String arg = idx2Node.get(argIdx).getLemma();
		String pos = idx2Node.get(argIdx).getPos();
		boolean isEnt = isEntity(pos);
		String ret = pred + " " + arg + " " + eIdx + " " + (isEnt ? "E" : "G") + " " + sentIdx;
		if (ConstantsParsing.writeDebugString) {
			System.out.println("eIdx: " + eIdx);
		}
		return new String[] { ret, eIdx + "" };
	}

	// (visit.1,visit.2) O H => [visit.1 O, visit.2 H]
	String[] getUnariesFromBinary(String rel) {
		String modifierStr = "";
		if (rel.contains("__")) {
			int modIdx = rel.indexOf("__");
			modifierStr = rel.substring(0, modIdx);
			rel = rel.substring(modIdx + 2);
		}
		try {
			// System.out.println("rel is: "+rel);
			String[] ss = rel.split(" ");
			String[] unaryPreds = ss[0].substring(1, ss[0].length() - 1).split(",");
			String u1 = unaryPreds[0] + " " + ss[1] + " " + ss[3] + " " + ss[4].charAt(0) + " " + ss[5];
			String u2 = unaryPreds[1] + " " + ss[2] + " " + ss[3] + " " + ss[4].charAt(1) + " " + ss[5];
			if (!modifierStr.equals("")) {
				u1 = modifierStr + "__" + u1;
				u2 = modifierStr + "__" + u2;
			}
			return new String[] { u1, u2 };
		} catch (Exception e) {
			// e.printStackTrace();
			return null;
		}
	}

	// (man.with.1,man.with.2) man shirt => (with.1,with.2) man,shirt
	String postProcessSameIndexMainStr(String mainStr) {
		String[] ss = mainStr.split(" ");
		String pred = ss[0].substring(1, ss[0].length() - 1);
		String cand = pred.split(",")[0].split("\\.")[0];
		if (cand.startsWith(ss[1])) {
			mainStr = mainStr.replace(ss[1] + ".", "");
		} else if (cand.startsWith(ss[2])) {
			mainStr = mainStr.replace(ss[2] + ".", "");
		}
		return mainStr;
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

			// Added on 27 Apr 2018
			String pos = idx2Node.get(arg2Index).getPos();

			// Added on 27 Apr 2018

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

			if (pos.startsWith("NNP")) {
				// System.out.println("bad pos VP: " + pos + " " + predArgStr + " " +
				// idx2Node.get(arg2Index).getWord());
				continue;
			}

			if (addedPredArgStrs.contains(predArgStr)) {
				continue;
			}
			addedPredArgStrs.add(predArgStr);
			// System.out.println("added new relation (VP): " + predArgStr);
			if (modifierStr.equals("") || !ConstantsParsing.removebasicEvnetifEEModifer) {
				BinaryRelInfo relInfo0 = getBinaryRelInfo(arg1, arg2, predArgStr, swapped, arg1Index, thisArg2Index,
						eventIndex, accepted, dsStr.length() > 0, idx2Node, sentIdx);
				// addRelInfo(relInfos, relInfo0, currentArgIdxPairs, arg1Index, thisArg2Index,
				// false);
				// System.out.println("adding relinfo4: "+relInfo0.mainStr);
				relInfos.add(relInfo0);
				// System.out.println("added relInfo twohop vp: " + relInfo0.mainStr);
			}

			if (!modifierStr.equals("")) {
				predArgStr = getPredArgString(modifierStr, leftPred, rightPred, arg1, arg2, negated, eventIdx2);
				// System.out.println("added new relation (VP): " + predArgStr);
				BinaryRelInfo relInfo0 = getBinaryRelInfo(arg1, arg2, predArgStr, swapped, arg1Index, thisArg2Index,
						eventIndex, accepted, dsStr.length() > 0, idx2Node, sentIdx);
				// addRelInfo(relInfos, relInfo0, currentArgIdxPairs, arg1Index, thisArg2Index,
				// false);
				// System.out.println("adding relinfo5: "+relInfo0.mainStr);
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

			// Added on 27 Apr 2018
			String pos = edge2.getMediator().getPos(); // idx2Node.get(arg2Index).getPos();
			// Added on 27 Apr 2018

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
					if (Util.prepositions.contains(lr[1].split("\\.")[0]) || lr[1].contains(".'s.")
							|| lr[1].contains(".'.")) {
						// System.out.println("shouldCont: " + lr[1]);
						shouldAdd = false;
					}
				} catch (Exception e) {
				}

				arg2 = edge2.getRight().getLemma();
				thisArg2Index = edge2.getRight().getWordPosition();
			} else if (edge2.getRight().getWordPosition() == arg2Index) {
				thisRightPred += lr[0];
				try {
					if (Util.prepositions.contains(lr[0].split("\\.")[0]) || lr[0].contains("'s.")
							|| lr[0].contains("'.")) {
						// System.out.println("shouldCont: " + lr[0]);
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

			if (modifierStr.equals("") || !ConstantsParsing.removebasicEvnetifEEModifer) {
				// System.out.println("added new relation: " + predArgStr);
				BinaryRelInfo relInfo0 = getBinaryRelInfo(arg1, arg2, predArgStr, swapped, arg1Index, thisArg2Index,
						eventIndex, accepted, dsStr.length() > 0, idx2Node, sentIdx);
				// addRelInfo(relInfos, relInfo0, currentArgIdxPairs, arg1Index, thisArg2Index,
				// false);

				if (shouldAdd) {
					// System.out.println("adding relinfo2: "+relInfo0.mainStr);
					if (pos.startsWith("NNP")) {
						// System.out
						// .println("bad pos NP: " + pos + " " + predArgStr + " " +
						// idx2Node.get(arg2Index).getWord());
						continue;
					}

					relInfos.add(relInfo0);
				}

			}

			// System.out.println("added relInfo twohop np: " + relInfo0.mainStr);
			// System.out.println(edge2.getMediator().getLemma() + " " +
			// edge2.getMediator().getPos() + " "
			// + lr[0].equals(lr[1]) + " " + lr[0] + " " + lr[1]);

			if (!modifierStr.equals("") && shouldAdd && !pos.startsWith("NNP")) {
				predArgStr = getPredArgString(modifierStr, leftPred, thisRightPred, arg1, arg2, negated, eventIdx2);
				// System.out.println("added new relation: " + predArgStr);
				BinaryRelInfo relInfo0 = getBinaryRelInfo(arg1, arg2, predArgStr, swapped, arg1Index, thisArg2Index,
						eventIndex, accepted, dsStr.length() > 0, idx2Node, sentIdx);
				// addRelInfo(relInfos, relInfo0, currentArgIdxPairs, arg1Index, thisArg2Index,
				// false);
				// System.out.println("adding relinfo3: "+relInfo0.mainStr);
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
		relInfo.eventIdx = eventIndex;
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
	String[] getEventTypeStr(LexicalGraph ungroundedGraph, HashMap<Integer, LexicalItem> idx2Node, int eventIndex,
			Edge<LexicalItem> edge) {
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

						if (ConstantsParsing.lemmatizePred) {
							eventTypeStrParticle = eventTypeStrParticle
									.replace(edge.getMediator().getWord(), edge.getMediator().getLemma()).toLowerCase();
						}
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
		ConstantsParsing.nbestParses = 1;// TODO: be careful
		if (ConstantsParsing.tenseParseTest) {
			tenseMain(args);
			System.exit(0);
		}

		PredicateArgumentExtractor prEx = new PredicateArgumentExtractor("");
		// String s = "Every European can travel freely within Europe.";
		// String s = "Cleveland works at The White House.";
		// String s = "Cleveland works at The White House.";
//		String s = "The Avalanche were zero of four against the Coyotes.";
//		String s = "Smith came up big and made one sprawling save after another.";
		String s = "Pluto’s moon is beautiful.";
		// String s = "Barack Obama visited Hawaii.";
		// String s = "President Barack Obama intends to nominate B. Todd Jones as his
		// choice to be the next leader of the U.S. Bureau of Alcohol, Tobacco, Firearms
		// and Explosives. Cameron said the coalition's main aim was to stay ahead in
		// the \\\"global race\\\" and namechecked India, China, Indonesia, Malaysia,
		// Brazil, Mexico and Turkey as examples of countries that Britain would fall
		// behind without reforms.";
		// String s = "Cameron said the coalition's main aim was to stay ahead in the
		// \"global race\" and namechecked India, China, Indonesia, Malaysia, Brazil,
		// Mexico and Turkey as examples of countries that Britain would fall behind
		// without reforms.";
		// String s = "Two women having drinks and smoking cigarettes at the bar";
		// String s = "Stay in contact with friends – in person. Put down the
		// electronics and call a friend. Host a game night. Set “dates” for the winter
		// to meet friends for dinner and a movie. Go shopping together. Meet for
		// coffee. Take a social dance class with friends. Loneliness can also lead to
		// depression. Having a highly active social life can decrease Alzheimer’s
		// disease risk by a surprisingly high 70 percent, according to new findings
		// published in the Journal of the International Neuropsychological Society. So
		// put yourself out there and find a group to share and laugh with!";
		// String s = "A man wearing glasses and a ragged costume is playing a Jaguar
		// electric guitar and singing with the accompaniment of a drummer.";
		// String s = "A man is walking and he is talking to his friend";
		// String s = "The man is outdoors.";
		// String s = "Man on bike with female standing on rear of back with arms around
		// his neck.";
		// String s = "A woman in a black coat eats dinner while her dog looks on.";

		// String s = "Tom managed to pass the exam.";
		// String s = "Tom just visited Harry at home.";
		// String s = "Tom visited Harry at home.";
		// String s = "Tom laughed.";
		// String s = "Tom gave up his dream";
		// String s = "John ate a banana.";
		// String s = "Chris sees Asha pay Boyang.";
		// String s = "The cervical cancer vaccine , approved in 2006 , is recommended
		// for girls around 11 or 12 .";
		// String s = "what is the first book Sherlock Holmes appeared in?";
		// String s = "what type of car does michael weston drive?";
		// String s = "location_1 be combined with location_2";
		// String s = "drug_1 should be taken by drug_2";
		// String s = "disease is increasing in country";

		s = Util.preprocess(s);
		// System.out.println("pre processed s: " + s);

		String[] exPrss = prEx.extractPredArgsStrs(s, 0, true, true, null);
		String mainRels = exPrss[0];
		System.out.println(mainRels);
		// System.out.println(exPrss[4]);
	}

	public static void tenseMain(String[] args) throws ArgumentValidationException, IOException, InterruptedException {
		PredicateArgumentExtractor prEx = new PredicateArgumentExtractor("");

		TensePair[] tenseSentences = new TensePair[] {
				// new TensePair("Past Simple","Obama received a gift."),
				// new TensePair("Past Passive","A gift was received by Obama"),
				// new TensePair("Present Simple","Obama receives a gift."),
				// new TensePair("Future Simple","Obama will receive gift."),
				// new TensePair("Past Perfect","Obama had received a gift."),
				// new TensePair("Present Perfect","Obama has received a gift."),
				// new TensePair("Future Perfect","Obama will have received a gift."),
				// new TensePair("Future Passive", "A gift will be received by Obama"),
				// new TensePair("Past Progressive","Obama was receiving a gift"),
				// new TensePair("Present Progressive","Obama is receiving a gift."),
				// new TensePair("Future Progressive","Obama will be receiving a gift."),
				// new TensePair("Past Perfect Progressive","Obama had been receiving a gift"),
				// new TensePair("Present Perfect Progressive","Obama has been receiving a
				// gift"),
				// new TensePair("Future Perfect Progressive", "Barack Obama will have been
				// receiving a gift on Monday")
				// new TensePair("Future Perfect Progressive", "John did not manage to arrive in
				// London on Monday.") };
				new TensePair("Future Perfect Progressive", "Barcelona did not manage to win the game.") };

		for (TensePair s : tenseSentences) {
			System.out.println("before Util" + s.tenseSentence());
			String sent = Util.preprocess(s.tenseSentence());
			System.out.println("after Util" + sent);
			String[] exPrss = prEx.extractPredArgsStrs(sent, 0, true, true, null);

			String mainRels = exPrss[0];
			System.out.println(mainRels);
			System.out.println("Finished parsing");
			// System.out.println("pre-processed s: " + s);

			// System.out.println(exPrss[4]);
		}
	}

	static class TensePair {
		private final String tense;
		private final String tenseSentence;

		public TensePair(String aTense, String aTenseSentence) {
			tense = aTense;
			tenseSentence = aTenseSentence;
		}

		public String tense() {
			return tense;
		}

		public String tenseSentence() {
			return tenseSentence;
		}
	}

	public static boolean isEntity(String pos) {
		// System.out.println(node.getLemma());
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

			if (!ConstantsParsing.onlyNounOrNE) {
				if (isEntity(node1.getPos()) && isEntity(node2.getPos())) {// E E
					return 2;
				} else if (isEntity(node1.getPos())) {
					return 0;
				} else if (isEntity(node2.getPos())) {
					return 1;
				} else {
					return 3;
				}
			} else {
				if (isEntity(node1.getPos()) && isEntity(node2.getPos())) {// E E
					return 2;
				} else if (isEntity(node1.getPos()) && isNoun(node2.getPos())) {
					return 0;
				} else if (isEntity(node2.getPos()) && isNoun(node1.getPos())) {
					return 1;
				} else if (isNoun(node1.getPos()) && isNoun(node2.getPos())) {
					return 3;
				} else {
					return -1;
				}
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
	int eventIdx;
}