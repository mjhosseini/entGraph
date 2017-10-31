package entailment.vector;

import java.util.HashMap;

public class SimplePredicateVector {
	String predicate;
	int uniqueId;
	HashMap<Integer, SimilaritiesInfo> similarityInfos;// NeighborPredIdx to
	// similarity
	
	//only because of the extension
	public SimplePredicateVector() {
		
	}
	
	public SimplePredicateVector(String predicate, int uniqueId, HashMap<Integer, SimilaritiesInfo> similarityInfos) {
		this.predicate = predicate;
		this.uniqueId = uniqueId;
		this.similarityInfos = similarityInfos;
	}

	public HashMap<Integer, SimilaritiesInfo> getSimilarityInfos() {
		return similarityInfos;
	}

	public void setSimilarityInfos(HashMap<Integer, SimilaritiesInfo> similarityInfos) {
		this.similarityInfos = similarityInfos;
	}
}
