package graph;

public class SimpleScore implements Comparable<SimpleScore>{
	public float score;
	public String pred1;
	public String pred2;
	
	public SimpleScore(String pred1, String pred2, float score) {
		this.pred1 = pred1;
		this.pred2 = pred2;
		this.score = score;
	}
	
	public int compareTo(SimpleScore s2){
		if (score>s2.score){
			return 1;
		}
		else if(score<s2.score){
			return -1;
		}
		return 0;
	}
	
	public String toString(){
		return pred1 +" => " + pred2 + " "+score;
	}
	
}
