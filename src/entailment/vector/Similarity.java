package entailment.vector;

public class Similarity implements Comparable<Similarity> {
	String pred;
	double sim;
	
	public Similarity(String pred, double sim) {
		this.pred = pred;
		this.sim = sim;
	}

	public int compareTo(Similarity dp2) {
		return (new Double(sim)).compareTo(new Double(dp2.sim));
		// if (sim > dp2.sim) {
		// return 1;
		// } else if (sim < dp2.sim) {
		// return -1;
		// }
		//
		// return 0;
	}

}
