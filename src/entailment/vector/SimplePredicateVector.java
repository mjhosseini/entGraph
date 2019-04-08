package entailment.vector;

import java.util.HashMap;
import java.util.Map;

public class SimplePredicateVector {
	String predicate;
	int uniqueId;
	Map<Integer, SimilaritiesInfo> similarityInfos;// NeighborPredIdx to
	// similarity
	
	//only because of the extension
	public SimplePredicateVector() {
		
	}
	
	public SimplePredicateVector(String predicate, int uniqueId, Map<Integer, SimilaritiesInfo> similarityInfos) {
		this.predicate = predicate;
		this.uniqueId = uniqueId;
		this.similarityInfos = similarityInfos;
	}

	public Map<Integer, SimilaritiesInfo> getSimilarityInfos() {
		return similarityInfos;
	}

	public void setSimilarityInfos(HashMap<Integer, SimilaritiesInfo> similarityInfos) {
		this.similarityInfos = similarityInfos;
	}
}
