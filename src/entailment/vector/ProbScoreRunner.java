package entailment.vector;

import java.util.List;

public class ProbScoreRunner implements Runnable{
	
	EntailGraph entGraph;
	String predPair;
	int idx;
	String[] typess;
	List<String> argPairs;
	double nTuples;
	
	public ProbScoreRunner(EntailGraph entGraph, String predPair, int idx, String[] typess, List<String> argPairs, double nTuples){
		this.entGraph = entGraph;
		this.predPair = predPair;
		this.idx = idx;
		this.typess = typess;
		this.argPairs = argPairs;
		this.nTuples = nTuples;
	}

	@Override
	public void run() {
		System.out.println(idx + " " + predPair);
		
		String[] ss = predPair.split("#");
		if (ss.length < 2) {
			return;
		}
		String rawPred1 = ss[0];
		String rawPred2 = ss[1];
		boolean aligned = ss[2].equals("true");

		// Now, compute the actual similarity!
		
		
		
		String rel11 = rawPred1 + "#" + typess[0] + "#" + typess[1];
		String rel12 = rawPred1 + "#" + typess[1] + "#" + typess[0];
		String rel21 = rawPred2 + "#" + typess[0] + "#" + typess[1];
		String rel22 = rawPred2 + "#" + typess[1] + "#" + typess[0];
		
		if (aligned) {
			double p1 = entGraph.computeProbScore(rel11, rel21, -1, argPairs, nTuples);
			entGraph.computeProbScore(rel12, rel22, p1, argPairs, nTuples);
		}
		else {
			double p2 = entGraph.computeProbScore(rel11, rel22, -1, argPairs, nTuples);
			entGraph.computeProbScore(rel12, rel21, p2, argPairs, nTuples);
		}


		//TODO: be careful
//		String dummyStr = "dummyy#dummyy" + idx;
//		entGraph.addBinaryRelation(rel11, dummyStr, null, 1e-40f, -1, 1);
//		entGraph.addBinaryRelation(rel12, dummyStr, null, 1e-40f, -1, 1);
//		entGraph.addBinaryRelation(rel21, dummyStr, null, 1e-40f, -1, 1);
//		entGraph.addBinaryRelation(rel22, dummyStr, null, 1e-40f, -1, 1);
		
	}

}
