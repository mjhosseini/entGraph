package entailment.randWalk;

import java.util.List;
@Deprecated
public class RandWalkNode {
	
	String name;
	int idx;
	List<Integer> neighs;
	List<Double> scores;
	double sumScores;
	List<Double> accProbs;
	List<Double> randWalkProbs;

	public RandWalkNode(String name, int idx, List<Integer> neighs, List<Double> scores, double sumScores) {
		this.name = name;
		this.idx = idx;
		this.neighs = neighs;
		this.scores = scores;
		this.sumScores = sumScores;
//		randWalkProbs = 
		setAccProbs();
	}

	// input: neighs, scores and sumScores
	public void setAccProbs() {
		double sumP = 0;
		for (int i = 0; i < neighs.size(); i++) {

			double p = 0;
			if (sumScores != 0) {
				p = scores.get(i) / sumScores;
			}
			sumP += p;
			accProbs.add(sumP);
		}
	}

	int getNextRandNode() {
		double r = Math.random();
		for (int i = 0; i < accProbs.size()-1; i++) {
			if (r>=accProbs.get(i) && r<=accProbs.get(i+1)) {
				return neighs.get(i);
			}
		}
		return neighs.get(accProbs.size()-1);
	}

}
