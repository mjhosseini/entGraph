package entailment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.maltparser.core.helper.HashSet;

import constants.ConstantsParsing;
import in.sivareddy.graphparser.ccg.LexicalItem;
import in.sivareddy.graphparser.parsing.LexicalGraph;

public class UngroundedSemParseInfo {
	public List<LexicalGraph> graphs;
	public List<String> tokens;
	public List<String> tokenLemmas;
	public int[] idxToTokenIdx; // James_Cameron directed Titanic. => 0=>0 1=>2 2=>3 3=>4
	public String tokensStr;
	int startTokenIdx;
	int windowSize = 10; // left window or right window, including the token.
	
	// startTokenIdx is used to account for previous sentences.
	public UngroundedSemParseInfo(List<LexicalGraph> graphs, String processedSentence, int startTokenIdx,
			boolean tokenize) {
		this.graphs = graphs;
		tokens = new ArrayList<String>();
		tokenLemmas = new ArrayList<>();
		
		if (!tokenize || graphs.size() == 0) {
			return;
		}
		this.startTokenIdx = startTokenIdx;
		LexicalGraph ungroundedGraph = graphs.get(0);
		tokens = new ArrayList<String>();
		tokenLemmas = new ArrayList<>();

		String[] ss = processedSentence.split("\\s");
		idxToTokenIdx = new int[ss.length];

		Map<Integer, String> idxToLemma = new HashMap<>();
		for (LexicalItem node : ungroundedGraph.getNodes()) {
			idxToLemma.put(node.getWordPosition(), node.getLemma().replace("-", "_")); // james-cameron => james_cameron
		}
		
		if (ConstantsParsing.writeDebugString) {
			System.out.println("idxToLemma: " + idxToLemma);
		}

		int tokenIdx = startTokenIdx;
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < ss.length; i++) {
//			System.out.println(ss[i]);
			String s = ss[i];
			String word = s.split("\\|")[0]; // John_Smith
			word = Util.replaceStanfordParserSpecialTokens(word);
			idxToTokenIdx[i] = tokenIdx;
			String[] this_tokens = word.split("_");
			for (String token : this_tokens) {
				tokens.add(token);
				sb.append(token + " ");
			}

			String lemma = idxToLemma.containsKey(i) ? idxToLemma.get(i) : Util.getLemma(word);
			String[] this_lemmas = lemma.split("_");

			if (this_lemmas.length != this_tokens.length) {
				if (ConstantsParsing.writeDebugString) {
					System.out.println("aligning unaligned lemma and word" + word + " " + lemma);
				}
				this_lemmas = word.toLowerCase().split("_");
			}

			for (String this_lemma : this_lemmas) {
				this_lemma = Util.replaceStanfordParserSpecialTokens(this_lemma);
				tokenLemmas.add(this_lemma);
			}

			tokenIdx += this_tokens.length;
		}

		this.tokensStr = sb.toString().trim();

		if (ConstantsParsing.writeDebugString) {
			System.out.println("tokens: " + tokens);
			System.out.println("lemmas: " + tokenLemmas);
			System.out.println("idxToTokenIdxs: " + Arrays.toString(idxToTokenIdx));
			System.out.println("tokensStr: " + tokensStr);
		}
	}

	public String getPredicateTokenIdxes(int predIdx, String pred) {
		int mainIdx = idxToTokenIdx[predIdx];
		String[] predStrs = Util.getPhraseFromCCGRel(pred);
		Set<String> predTokens = new HashSet<>();
		for (String s : predStrs) {
			predTokens.add(s);
		}
		if (ConstantsParsing.writeDebugString) {
			System.out.println("pred: " + pred);
			System.out.println("predTokens: " + predTokens);
		}

		List<String> tokens = this.tokenLemmas;
		if (!ConstantsParsing.lemmatizePred) {
			tokens = this.tokens;
		}

		List<Integer> predIdxes = new ArrayList<>();

		// first check the right window.
		for (int idx = mainIdx - startTokenIdx; idx < Math.min(tokens.size(),
				mainIdx - startTokenIdx + windowSize); idx++) { // TODO: check if 5 is good

			if (predTokens.contains(tokens.get(idx))) {
				predIdxes.add((idx + startTokenIdx));
				predTokens.remove(tokens.get(idx));
			}
		}

		// then check the left window.
		for (int idx = Math.max(0, mainIdx - startTokenIdx - windowSize); idx < mainIdx - startTokenIdx; idx++) { // TODO:
																													// check
																													// if
																													// 5
																													// is
																													// good

			if (predTokens.contains(tokens.get(idx))) {
				predIdxes.add((idx + startTokenIdx));
				predTokens.remove(tokens.get(idx));
			}
		}

		if (predIdxes.size() == 0) {
			predIdxes.add(mainIdx);
		}

		Collections.sort(predIdxes);
		StringBuilder sb = new StringBuilder();
		sb.append(predIdxes.get(0));
		for (int i = 1; i < predIdxes.size(); i++) {
			sb.append("_" + predIdxes.get(i));
		}

		String ret = sb.toString();

		if (ConstantsParsing.writeDebugString) {
			System.out.println("pred idxes: " + ret);
		}

		return ret;
	}

	public int getNumTokens() {
		if (ConstantsParsing.lemmatizePred) {
			return tokenLemmas.size();
		} else {
			return tokens.size();
		}
	}

}
