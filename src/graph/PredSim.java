package graph;

public class PredSim {
	public String pred;
	public float sim;
	
	public PredSim(String pred, float sim) {
		this.pred = pred;
		this.sim = sim;
	}
	
	public boolean equals(Object o){
		try {
			PredSim ps = (PredSim)o;
			return this.pred.equals(ps.pred) && this.sim==ps.sim;
		} catch (Exception e) {
			return false;
		}
	}
	
	public String toString(){
		return pred + " " + sim;
	}
	
}
