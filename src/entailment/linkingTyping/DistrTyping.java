package entailment.linkingTyping;

import java.io.BufferedReader;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.maltparser.core.helper.HashMap;

public class DistrTyping {

	static Map<String, float[]> wordToTypeProbs;
	static Map<String, float[]> docToTypeProbs;
	public static int numTopics = 15;
	static String topicWeightsPath = "LDATypes/topic-weights-norm-"+numTopics;
	static String docWeightsPath = "LDATypes/doc-topic"+numTopics;
	static float[] defaultProbs;
	public static final float typePropThresh = 1e-3f;


	static void loadTopicWeights() throws IOException {
		wordToTypeProbs = new HashMap<>();
		defaultProbs = new float[numTopics];
		for (int i = 0; i < defaultProbs.length; i++) {
			defaultProbs[i] = (float) 1 / numTopics;
		}
		BufferedReader br = new BufferedReader(new FileReader(topicWeightsPath));
		String line = null;
		while ((line = br.readLine()) != null) {
			String[] ss = line.split("\t");
			String word = ss[1];
			if (word.contains("(") && !word.contains("")) {
				word += ")";
			}
			if (!wordToTypeProbs.containsKey(word)) {
				wordToTypeProbs.put(word, new float[numTopics]);
			}
			int thisTopic = Integer.parseInt(ss[0]);
			float prob = Float.parseFloat(ss[2]);
			wordToTypeProbs.get(word)[thisTopic] = prob;
		}
		br.close();
	}

	static void loadDocWeights() throws IOException {
		System.err.println("loading doc weights");
		docToTypeProbs = new HashMap<>();
		BufferedReader br = new BufferedReader(new FileReader(docWeightsPath));
		String line = null;

		while ((line = br.readLine()) != null) {
			String[] ss = line.split("\t");
			assert ss.length == numTopics + 2;
			String doc = ss[1];

			docToTypeProbs.put(doc, new float[numTopics]);

			for (int i = 0; i < numTopics; i++) {
				float prob = Float.parseFloat(ss[i + 2]);
				if (prob > typePropThresh) {
					docToTypeProbs.get(doc)[i] = prob;
				}
			}

		}
		br.close();
	}

	static float[] getArgProb(String arg) {
		if (wordToTypeProbs.containsKey(arg)) {
			return wordToTypeProbs.get(arg);
		}
		return defaultProbs;
	}

	static float[] getPredProb(String pred) {
		if (docToTypeProbs.containsKey(pred)) {
			return docToTypeProbs.get(pred);
		}
		return defaultProbs;
	}
	
	public static void loadLDATypes(){
		try {
			System.err.println("loadig topic weights");
			loadTopicWeights();
			System.err.println("done with topic weights");
			loadDocWeights();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static List<float[]> getType(String pred, String arg1, String arg2) {
		if (wordToTypeProbs==null){
			try {
				System.err.println("loadig topic weights");
				loadTopicWeights();
				System.err.println("done with topic weights");
				loadDocWeights();
			} catch (IOException e) {
				e.printStackTrace();
				e.printStackTrace();
			}
		}
		arg1 = arg1.replace(" ", "_");
		arg2 = arg2.replace(" ", "_");
		if (pred.contains("__")){
			int idx = pred.indexOf("__");
			pred = pred.substring(idx+2);
		}
//		System.err.println("word to type probs:"+wordToTypeProbs);
//		System.err.println(docToTypeProbs);
		
		List<float[]> ret = new ArrayList<>();

		pred = pred.substring(1, pred.length() - 1);
		String[] ss = pred.split(",");
		if (ss.length!=2){
			System.err.println("bad len: "+pred);
		}
		String pred1 = ss[0];
		String pred2 = ss[1];

		float[] arg1Probs = getArgProb(arg1);
		float[] pred1Probs = getPredProb(pred1);

		float[] arg2Probs = getArgProb(arg2);
		float[] pred2Probs = getPredProb(pred2);

		float[] ret1 = new float[numTopics];
		float[] ret2 = new float[numTopics];

		double sum = 0;
		for (int i = 0; i < numTopics; i++) {
			ret1[i] = arg1Probs[i] * pred1Probs[i];
			sum += ret1[i];
		}
		
		if (sum==0){
			ret1 = arg1Probs;
		}
		else{
			for (int i = 0; i < numTopics; i++) {
				ret1[i] /= sum;
				if (ret1[i]<DistrTyping.typePropThresh){
					ret1[i]=0;
				}
			}
		}

		

		sum = 0;
		for (int i = 0; i < numTopics; i++) {
			ret2[i] = arg2Probs[i] * pred2Probs[i];
			sum += ret2[i];
		}

		if (sum==0){
			ret2 = arg2Probs;
		}
		else{
			for (int i = 0; i < numTopics; i++) {
				ret2[i] /= sum;
				if (ret2[i]<DistrTyping.typePropThresh){
					ret2[i]=0;
				}
			}
		}
		
		ret.add(ret1);
		ret.add(ret2);
		return ret;

	}

}
