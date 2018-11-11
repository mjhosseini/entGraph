package graph.selfTrain;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import constants.ConstantsGraphs;
import graph.Edge;
import graph.Node;
import graph.PGraph;

public class ConfidentExampleMaker {
	
	public static float selfTrainPosThreshold = .16f;
	public static float selfTrainNegThreshold = .001f;
	public static int numDesiredPosExamples = 100000;
	static Random randGen = new Random();
	
	public static void main(String[] args) throws FileNotFoundException {
		
		ConstantsGraphs.edgeThreshold = -1;
		
		PrintStream confExamplesOp = new PrintStream(new File("conf_rels.txt"));

		File folder = new File(ConstantsGraphs.root);
		File[] files = folder.listFiles();
		Arrays.sort(files);
		
		List<Float> edgeWeithgs = new ArrayList<Float>();

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
			
			for (Edge e:pgraph.getSortedEdges()) {
				if (e.sim>selfTrainPosThreshold) {
					edgeWeithgs.add(e.sim);
				}
				else {
					break;
				}
			}

			System.out.println("fname: " + fname);
		}
		
		Collections.sort(edgeWeithgs,Collections.reverseOrder());
		
		try {
			selfTrainPosThreshold = edgeWeithgs.get(numDesiredPosExamples);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
//		for (PGraph pgraph:pgraphs) {
//			writeExamples(pgraph, confExamplesOp);
//		}
		
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
			
			writeExamples(pgraph, confExamplesOp);

			System.out.println("fname: " + fname);
		}

		System.out.println("allEdgesRem, allEdges: " + PGraph.allEdgesRemained + " " + PGraph.allEdges);
		

		// Collections.sort(scores, Collections.reverseOrder());
		// System.out.println("higest scoring relations:");
		// for (int i = 0; i < Math.min(1000000, scores.size()); i++) {
		// System.out.println(scores.get(i));
		// }
	}
	
	static String getPlainType(String s) {
		return s.replace("_1", "").replace("_2", "");
	}
	
	static String prettyString(Node node) {
		String[] ss = node.id.split("#");
		return ss[0]+" "+ss[1]+"::"+getPlainType(ss[1])+" "+ss[2]+"::"+getPlainType(ss[2]);
	}
	
	static void writeExamples(PGraph pgraph, PrintStream confExamplesOp) {
		
		List<Edge> edges = pgraph.getSortedEdges();
		for (Edge edge: edges) {
			
			if (edge.sim<=selfTrainPosThreshold) {
				break;
			}
			
			int maxTry = 10;
			int numTry;
			
			for (numTry=0; numTry<maxTry; numTry++) {
				
				//random node as negative!
				int randIdx = randGen.nextInt(pgraph.nodes.size());
				if (randIdx==edge.i || (pgraph.nodes.get(edge.i).idx2oedges.containsKey(randIdx) && pgraph.getW(edge.i, randIdx) >= selfTrainNegThreshold)) {
					continue;
				}

				Node node1 = pgraph.nodes.get(edge.i);
				Node node2 = pgraph.nodes.get(edge.j);
				Node node3 = pgraph.nodes.get(randIdx);
				
				confExamplesOp.println(prettyString(node2)+"\t"+prettyString(node1)+"\tTrue");
				confExamplesOp.println(prettyString(node3)+"\t"+prettyString(node1)+"\tFalse");
				
				break;
				
			}
		}
	}
	
}
