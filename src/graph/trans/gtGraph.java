package graph.trans;

import java.util.ArrayList;

import java.util.List;

import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

public class gtGraph extends DefaultDirectedGraph<Integer, DefaultEdge> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	List<List<Integer>> comps;

	public gtGraph(Class<? extends DefaultEdge> arg0) {
		super(arg0);
		comps = new ArrayList<>();
	}
}
