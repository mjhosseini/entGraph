package graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Node {
	int idx;
	String id;
	List<Oedge> oedges;
	Map<Integer, Oedge> idx2oedges;

	public Node(int idx, String id) {
		this.idx = idx;
		this.id = id;
		this.oedges = new ArrayList<>();
		this.idx2oedges = new HashMap<>();
	}

	void addNeighbor(int nIdx, float sim) {
//		System.out.println("adding "+idx+" "+nIdx + " "+this.id +" " );
		if (!idx2oedges.containsKey(nIdx)) {
			Oedge oedge = new Oedge(nIdx, sim);
			oedges.add(oedge);
			idx2oedges.put(nIdx, oedge);
			
		}
	}
	
	public void clean() {
		this.oedges = null;
		this.idx2oedges = null;
	}
	
}
