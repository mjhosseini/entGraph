package graph;

import java.util.HashMap;
import java.util.HashSet;

import org.apache.commons.lang3.tuple.ImmutablePair;

public class Edge implements Comparable<Edge>{
	int i;
	int j;
	float sim;
	int hashCode = -1;
	public Edge(int i, int j, float sim) {
		this.i = i;
		this.j = j;
		this.sim = sim;
	}
	
	
	public int compareTo(Edge e) {
		if (this.sim>e.sim){
			return 1;
		}
		else if(this.sim<e.sim){
			return -1;
		}
		return 0;
	}
	
	public boolean equals(Object o){
		Edge e = (Edge)o;
		return e.i==i && e.j==j;
	}
	
	public int hashCode(){
		if (hashCode!=-1){
			return hashCode;
		}
		int h = new ImmutablePair<Integer, Integer>(i, j).hashCode();
		this.hashCode = h;
		return h;
	}
	
	public static void main(String[] args) {
		Edge e1 = new Edge(5, 10, -1);
		Edge e2 = new Edge(5, 10, 2);
		System.out.println(e1.hashCode());
		System.out.println(e2.hashCode());
		System.out.println(e1.equals(e2));
		
		HashMap<Edge,Float> s = new HashMap<>();
		s.put(e1,2f);
		System.out.println(s.containsKey(e2));
		
	}
	
}
