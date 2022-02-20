package graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Node {
	public int idx;
	public String id;
	public List<Oedge> oedges;
	public Map<Integer, Oedge> idx2oedges;
	private int numNeighs = -1;

	public Node(int idx, String id) {
		this.idx = idx;
		this.id = id;
		this.oedges = new ArrayList<>();
		this.idx2oedges = new HashMap<>();
	}

	public void addNeighbor(int nIdx, float sim) {
//		System.out.println("adding "+idx+" "+nIdx + " "+this.id +" " );
		if (!idx2oedges.containsKey(nIdx)) {
			Oedge oedge = new Oedge(nIdx, sim);
			oedges.add(oedge);
			idx2oedges.put(nIdx, oedge);
			
		}
	}
	
	public int getNumNeighs() {
		if (numNeighs==-1) {
			numNeighs=this.oedges.size();
		}
		return numNeighs;
	}
	
	public void clean() {
		this.numNeighs = this.oedges.size();
		this.oedges = null;
		this.idx2oedges = null;
	}
	
}
