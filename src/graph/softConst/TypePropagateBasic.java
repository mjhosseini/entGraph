package graph.softConst;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;

import graph.PGraph;
@Deprecated
public class TypePropagateBasic implements Runnable{

	ArrayList<PGraph> pGraphs;
	int threadIdx;

	// t1#t2#a#tp1#tp2 meaning that does tp1#tp2 extends to t1#t2
	Map<String, Integer> candidateEdges;
	Map<String, Integer> matchedEdges;

	public TypePropagateBasic(ArrayList<PGraph> pGraphs, int threadIdx, Map<String, Integer> candidateEdges,
			Map<String, Integer> matchedEdges) {
		this.pGraphs = pGraphs;
		this.threadIdx = threadIdx;
		this.candidateEdges = candidateEdges;
		this.matchedEdges = matchedEdges;
	}

	void getCompatibleTypelist() {

//		System.out.println("in getCompat");
		// checking whether g2 entailments can be used in g1
		int gc2 = 0;
		for (PGraph pg2 : pGraphs) {
			if (gc2++ % TypePropagateRunnerBasic.numThreads != threadIdx) {
				continue;
			}
			System.out.println("pg2: " + pg2.fname);
			
			DefaultDirectedWeightedGraph<Integer, DefaultWeightedEdge> g2 = pg2.g0;
			String gpt1 = pg2.types.split("#")[0];
			String gpt2 = pg2.types.split("#")[1];
			if (gpt1.equals(gpt2)) {
				gpt1 += "_1";
				gpt1 += "_2";
			}

			for (int i2 = 0; i2 < pg2.nodes.size(); i2++) {
				// if (p % 100 == 0) {
				// System.out.println("p: " + p);
				// }

				String p2 = pg2.nodes.get(i2).id;
				String[] ss = p2.split("#");
				p2 = ss[0];
				String tp2 = ss[1] + "#" + ss[2];

				for (DefaultWeightedEdge e : g2.outgoingEdgesOf(i2)) {
					int j2 = g2.getEdgeTarget(e);
					if (i2 == j2) {
						continue;
					}

					String q2 = pg2.nodes.get(j2).id;
					ss = q2.split("#");
					q2 = ss[0];
					String tq2 = ss[1] + "#" + ss[2];

					boolean aligned = tp2.equals(tq2);// args are aligned or not

					// Now, go over different graphs
					// and check whether the first graph has p2 and q2

					for (PGraph pg1 : pGraphs) {
						if (pg1.equals(pg2)) {
							continue;
						}
//						System.out.println("pg1: " + pg1.fname);
						String gt1 = pg1.types.split("#")[0];
						String gt2 = pg1.types.split("#")[1];
						if (gt1.equals(gt2)) {
							gt1 += "_1";
							gt2 += "_2";
						}

						if (aligned) {

							String tp1 = gt1 + "#" + gt2;
							String tq1 = gt1 + "#" + gt2;

							addCompatibility(p2, tp2, q2, tp1, tq1, aligned, pg1, candidateEdges, matchedEdges);

							tp1 = gt2 + "#" + gt1;
							tq1 = gt2 + "#" + gt1;

							addCompatibility(p2, tp2, q2, tp1, tq1, aligned, pg1, candidateEdges, matchedEdges);

						} else {
							String tp1 = gt1 + "#" + gt2;
							String tq1 = gt2 + "#" + gt1;

							addCompatibility(p2, tp2, q2, tp1, tq1, aligned, pg1, candidateEdges, matchedEdges);

							tp1 = gt2 + "#" + gt1;
							tq1 = gt1 + "#" + gt2;

							addCompatibility(p2, tp2, q2, tp1, tq1, aligned, pg1, candidateEdges, matchedEdges);
						}
					}
				}
			}

		}

	}

	void addCompatibility(String p2, String tp2, String q2, String tp1, String tq1, boolean aligned, PGraph pg1,
			Map<String, Integer> candidateEdges, Map<String, Integer> matchedEdges) {

		String p1Typed = p2 + "#" + tp1;
		String q1Typed = q2 + "#" + tq1;

//		System.out.println(p1Typed);
//		System.out.println(q1Typed);

		DefaultDirectedWeightedGraph<Integer, DefaultWeightedEdge> g1 = pg1.g0;
		if (pg1.pred2node.containsKey(p1Typed) && pg1.pred2node.containsKey(q1Typed)) {
			String propStr = tp1 + "#" + aligned + "#" + tp2;
			if (!candidateEdges.containsKey(propStr)) {
				candidateEdges.put(propStr, 0);
				matchedEdges.put(propStr, 0);
			}
			candidateEdges.put(propStr, candidateEdges.get(propStr) + 1);
//			System.out.println("add compat cand: " + propStr);
			int i1 = pg1.pred2node.get(p1Typed).idx;
			int j1 = pg1.pred2node.get(q1Typed).idx;

			if (g1.containsEdge(i1, j1)) {
				matchedEdges.put(propStr, matchedEdges.get(propStr) + 1);
//				System.out.println("add compat match: " + propStr);
			}
		}
	}

	@Override
	public void run() {
		getCompatibleTypelist();
	}

}
