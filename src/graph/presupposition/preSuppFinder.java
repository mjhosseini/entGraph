package graph.presupposition;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import constants.ConstantsGraphs;
import constants.ConstantsMNEmbIter;
import entailment.vector.PredicateVector;
import entailment.vector.Similarity;
import graph.Edge;
import graph.Node;
import graph.Oedge;
import graph.PGraph;

public class preSuppFinder {
	
	static String prepSuffix = "_prep.txt";
	
	public static void main(String[] args) throws FileNotFoundException {

		ConstantsGraphs.edgeThreshold = -1;

		File folder = new File(ConstantsGraphs.root);
		File[] files = folder.listFiles();
		Arrays.sort(files);

		int gc = 0;
		for (File f : files) {
			String fname = f.getName();

			if (!fname.contains(ConstantsGraphs.suffix)) {
				continue;
			}

			PGraph pgraph = new PGraph(ConstantsGraphs.root + fname);
			if (pgraph.nodes.size() == 0) {
				continue;
			}

			System.out.println("allEdgesRem, allEdges: " + PGraph.allEdgesRemained + " " + PGraph.allEdges);

			// if (gc++==50) {
			// break;
			// }

			gc++;

			System.out.println("fname: " + fname);
		}


		System.out.println("allEdgesRem, allEdges: " + PGraph.allEdgesRemained + " " + PGraph.allEdges);

	}
	
	static void findPreSupps(PGraph pgraph) throws FileNotFoundException {
		
		Map<String,List<Similarity>> id2SimList = new LinkedHashMap<>();
		
		for (Node node: pgraph.nodes) {
			if (node.id.startsWith("NEG__")) {
				continue;
			}
			
			List<Similarity> prepSims = new ArrayList<>();
			
			for (Oedge edge: node.oedges) {
				
				String negId = "NEG__"+ node.id;
				Node negNode = pgraph.pred2node.get(negId);
				if (negNode==null) {
					continue;
				}
				
				if (negNode.idx2oedges.containsKey(negNode.idx)) {
					Oedge negEdge = negNode.idx2oedges.get(negNode.idx);
					double sim = edge.sim;
					double negSim = negEdge.sim;
					double prepScore = Math.sqrt(sim*negSim);
					String id2 = pgraph.idx2node.get(edge.nIdx).id;
					prepSims.add(new Similarity(id2, prepScore));
				}
				
			}
			
			if (prepSims.size()>0) {
				id2SimList.put(node.id, prepSims);
			}
			
		}
		
		if (id2SimList.size()>0) {
			String oFname = pgraph.fname.substring(0, pgraph.fname.lastIndexOf('_')) + prepSuffix;
			
			PrintStream op = new PrintStream(new File(oFname));
			op.println("types: " + pgraph.types + ", num preds: " + id2SimList.size());
			
			for (String id: id2SimList.keySet()) {
				List<Similarity> prepSims = id2SimList.get(id);
				op.println("predicate: " + id);
				op.println("num neighbors: " + prepSims.size());
				PredicateVector.writeSims(op, prepSims, "presupposition sims");
				op.println();
			}
			op.close();
		}
	}
	
}
