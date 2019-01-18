package in.sivareddy.graphparser.cli;

import in.sivareddy.graphparser.ccg.CcgAutoLexicon;
import in.sivareddy.graphparser.parsing.GroundedGraphs;
import in.sivareddy.graphparser.parsing.LexicalGraph;
import in.sivareddy.graphparser.util.GroundedLexicon;
import in.sivareddy.graphparser.util.Schema;
import in.sivareddy.graphparser.util.knowledgebase.KnowledgeBaseCached;
import in.sivareddy.others.CcgSyntacticParserCli;
import in.sivareddy.others.EasyCcgCli;
import in.sivareddy.others.EasySRLCli;
import in.sivareddy.others.StanfordCoreNlpDemo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import uk.co.flamingpenguin.jewel.cli.ArgumentValidationException;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import constants.ConstantsParsing;
import entailment.PredicateArgumentExtractor;

public class CcgParseToUngroundedGraphs {
	JsonParser jsonParser;
	Gson gson;
	CcgSyntacticParserCli ccgParser = null;
	EasyCcgCli ccgParserQuestions = null;
	StanfordCoreNlpDemo nlpPipeline;
	GroundedGraphs graphCreator;
	Logger logger;
	public int nbestParses;

	public CcgParseToUngroundedGraphs(String dataFolder, String languageCode, boolean useQuestionsModel)
			throws ArgumentValidationException, IOException {
		jsonParser = new JsonParser();
		gson = new Gson();
		logger = Logger.getLogger(CcgParseToUngroundedGraphs.class);
		nbestParses = ConstantsParsing.nbestParses;
		
		String ccgModelDir = Paths.get(dataFolder, "easyccg_model").toString();

		// ccgParser = new EasyCcgCli(ccgModelDir, nbestParses);
		
		
		ccgParser = new EasyCcgCli(ccgModelDir + " -r S[dcl] S[pss] S[pt] S[b] S[ng] S NP", nbestParses);
//		ccgModelDir =
//		          Paths.get("lib_data", "model_ccgbank_questions").toString();
		
//		ccgParser =
//		          new EasySRLCli(ccgModelDir + " --rootCategories S[q] S[qem] S[wq]",nbestParses);
		
		// Too much: NP S[to] S[em] S[frg] S[for] S[intj] S[inv] N N[b] S[dcl]
		// S[pss] S[pt] S[b] S[ng] S
		// S[em] S[frg] S[for] S[intj] S[inv]
		// S[dcl] S[pss] S[pt] S[b] S[ng] S
		// lib_data/easyccg_model -r S[dcl] S[pss] S[pt] S[b] S[ng] S
		
		if (useQuestionsModel) {
			String ccgModelDirQuestions = Paths.get(dataFolder, "easyccg_model_questions").toString();
			ccgParserQuestions = new EasyCcgCli(ccgModelDirQuestions + " -s -r S[q] S[qem] S[wq]", nbestParses);
		}
		nlpPipeline = new StanfordCoreNlpDemo(languageCode);

		String markupFile = Paths.get(dataFolder, "candc_markedup.modified").toString();
		String unaryRulesFile = Paths.get(dataFolder, "unary_rules.txt").toString();
		String binaryRulesFile = Paths.get(dataFolder, "binary_rules.txt").toString();
		String specialCasesFile;
		if (ConstantsParsing.tenseParseTest) {
			specialCasesFile = Paths.get(dataFolder, "lexicon_specialCases_tensed.txt").toString();
		}
		else {
			specialCasesFile = Paths.get(dataFolder, "lexicon_specialCases.txt").toString();
		}
		String specialCasesQuestionsFile = Paths.get(dataFolder, "lexicon_specialCases_questions_vanilla.txt")
				.toString();

		CcgAutoLexicon normalCcgAutoLexicon = new CcgAutoLexicon(markupFile, unaryRulesFile, binaryRulesFile,
				specialCasesFile);

		CcgAutoLexicon questionCcgAutoLexicon = new CcgAutoLexicon(markupFile, unaryRulesFile, binaryRulesFile,
				specialCasesQuestionsFile);

		String[] relationLexicalIdentifiers = { "word" };
		String[] relationTypingIdentifiers = {};

		Schema schema = null;
		KnowledgeBaseCached kb = new KnowledgeBaseCached(null, null);
		GroundedLexicon groundedLexicon = new GroundedLexicon(null);
		graphCreator = new GroundedGraphs(schema, kb, groundedLexicon, normalCcgAutoLexicon, questionCcgAutoLexicon,
				relationLexicalIdentifiers, relationTypingIdentifiers, null, 1, false, false, false, false, false,
				false, false, false, false, false, false, false, false, false, false, false, false, false, false, false,
				false, false, false, false, false, false, false, false, false, false, false, false, false, 10.0, 1.0,
				0.0, 0.0);
	}

	public List<List<LexicalGraph>> processText(String line)
			throws ArgumentValidationException, IOException, InterruptedException {
		List<List<LexicalGraph>> allGraphs = new ArrayList<>();
		JsonObject jsonSentence = jsonParser.parse(line).getAsJsonObject();
		if (jsonSentence.has("sentence"))
			logger.debug("Input Sentence: " + jsonSentence.get("sentence").getAsString());
		String sentence = jsonSentence.get("sentence").getAsString();

		List<String> processedText = nlpPipeline.processText(sentence);
		// System.out.println("stan process text time: "
		// + (System.currentTimeMillis() - t0));

		for (String processedSentence : processedText) {
			// System.out.println(processedSentence);
			// We don't need questions for our application
			if (!ConstantsParsing.parseQuestions && processedSentence.endsWith("?|.|O")) {
				continue;
			}
			
			List<String> ccgParseStrings;
			
//			System.out.println("processed sentence: "+processedSentence);
			if (ConstantsParsing.parseQuestions) {
				ccgParseStrings = ccgParserQuestions != null && processedSentence.endsWith("?|.|O")
						? ccgParserQuestions.parse(processedSentence)
						: ccgParser.parse(processedSentence);
			}
			else {
				ccgParseStrings = ccgParser.parse(processedSentence);
			}
			
			
//			System.out.println("pr sen: " + processedSentence);
			
			// System.out.println("ccgparser time:
			// "+(System.currentTimeMillis()-t0));
			List<Map<String, String>> ccgParses = new ArrayList<>();
			for (String ccgParseString : ccgParseStrings) {
				Map<String, String> ccgParseMap = new HashMap<>();
				ccgParseMap.put("synPar", ccgParseString);
				ccgParseMap.put("score", "1.0");
				ccgParses.add(ccgParseMap);
			}
			jsonSentence.add("synPars", jsonParser.parse(gson.toJson(ccgParses)));

			String[] wordsString = processedSentence.split("\\s");
			List<Map<String, String>> words = Lists.newArrayList();
			for (String word : wordsString) {
				String[] parts = word.split("\\|");
				Map<String, String> wordMap = new HashMap<>();
				wordMap.put("word", parts[0]);
				wordMap.put("pos", parts[1]);
				wordMap.put("ner", parts[2]);
				words.add(wordMap);
			}

			jsonSentence.add("words", jsonParser.parse(gson.toJson(words)));

			List<LexicalGraph> graphs = graphCreator.buildUngroundedGraph(jsonSentence, "synPars", nbestParses, logger);
			// System.out.println("build graph time:
			// "+(System.currentTimeMillis()-t0));
			allGraphs.add(graphs);
		}
		return allGraphs;
	}

	public void setCCGParser(EasyCcgCli ccgParser) {
		this.ccgParser = ccgParser;
	}

	public static void main(String[] args) throws IOException, ArgumentValidationException, InterruptedException {
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		try {
			CcgParseToUngroundedGraphs parser = new CcgParseToUngroundedGraphs("lib_data", "en-ner", true);
			PatternLayout layout = new PatternLayout("%r [%t] %-5p: %m%n");
			Logger logger = Logger.getLogger(CcgParseToUngroundedGraphs.class);
			logger.setLevel(Level.DEBUG);
			logger.setAdditivity(false);
			Appender stdoutAppender = new ConsoleAppender(layout);
			logger.addAppender(stdoutAppender);

			String line = br.readLine();
			// String line =
			// "{\"sentence\" : \"Obama 's birthplace is Kenya .\"}";
			while (line != null) {
				if (line.trim().equals("") || line.charAt(0) == '#') {
					line = br.readLine();
					continue;
				}

				List<List<LexicalGraph>> allGraphs = parser.processText(line);
				for (List<LexicalGraph> graphs : allGraphs) {
					logger.debug("# Ungrounded Graphs");
					if (graphs.size() > 0) {
						for (LexicalGraph ungroundedGraph : graphs) {
							logger.debug(ungroundedGraph);
							/*-List<LexicalGraph> groundedGraphs = graphCreator
									.createGroundedGraph(ungroundedGraph, 10, 100,
											true, true, true, true, true, false);
							System.out
									.println("# Total number of Grounded Graphs: "
											+ groundedGraphs.size());
							
							int connectedGraphCount = 0;
							for (LexicalGraph groundedGraph : groundedGraphs) {
								// if (groundedGraph.isConnected()) {
								connectedGraphCount += 1;
								System.out.println("# Grounded graph: "
										+ connectedGraphCount);
								System.out.println(groundedGraph);
								System.out.println("Graph Query: "
										+ GraphToSparqlConverter
												.convertGroundedGraph(
														groundedGraph, schema));
								// }
							}
							
							System.out
									.println("# Total number of Grounded Graphs: "
											+ groundedGraphs.size());
							System.out
									.println("# Total number of Connected Grounded Graphs: "
											+ connectedGraphCount);
							System.out.println();*/
						}
					}
				}
				line = br.readLine();
			}
		} finally {
			br.close();
		}
	}
}
