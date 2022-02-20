package graph.trans;

import java.io.PrintStream;
import java.util.ArrayList;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.jgrapht.graph.DefaultEdge;

import constants.ConstantsTrans;
import eu.excitementproject.eop.common.datastructures.BidirectionalMap;
import eu.excitementproject.eop.common.datastructures.SimpleBidirectionalMap;
import eu.excitementproject.eop.globalgraphoptimizer.defs.Pair;
import eu.excitementproject.eop.globalgraphoptimizer.graph.AbstractRuleEdge;
import eu.excitementproject.eop.globalgraphoptimizer.graph.DirectedOneMappingOntologyGraph;
import eu.excitementproject.eop.globalgraphoptimizer.graph.DirectedOntologyGraph;
import eu.excitementproject.eop.globalgraphoptimizer.graph.NodeGraph;
import eu.excitementproject.eop.globalgraphoptimizer.graph.RelationNode;
import eu.excitementproject.eop.globalgraphoptimizer.graph.RuleEdge;
import eu.excitementproject.eop.globalgraphoptimizer.score.MapLocalScorer;
import graph.Node;
import graph.Oedge;
import graph.PGraph;

public class EOPTNF {
	
	static void formEntGraph(PGraph pgraph, double lmbda, PrintStream op) {

		// build the local graph
		DirectedOntologyGraph graph = new DirectedOneMappingOntologyGraph("work graph");
		Map<Pair<Integer, Integer>, Double> rule2ScoreMap = new HashMap<>();

		BidirectionalMap<String, Integer> desc2idMap = new SimpleBidirectionalMap<>();

		// add nodes
		for (Node node : pgraph.nodes) {
			RelationNode rNode = new RelationNode(node.idx, node.id);
			desc2idMap.put(node.id, node.idx);
			graph.addNode(rNode);
		}

		// add edges
		for (Node node : pgraph.nodes) {
			for (Oedge oedge : node.oedges) {
				Double confidence = (double) oedge.sim;
				Node neighNode = pgraph.nodes.get(oedge.nIdx);
				RelationNode sourceNode = new RelationNode(node.idx, node.id);
				RelationNode targetNode = new RelationNode(neighNode.idx, neighNode.id);

				try {
					graph.addEdge(new RuleEdge(sourceNode, targetNode, confidence));
					rule2ScoreMap.put(new Pair<Integer, Integer>(node.idx, neighNode.idx), confidence);
				} catch (Exception e) {
					throw new RuntimeException("Problem when adding edge " + node.id + " -> " + neighNode.id
							+ " with confidence = " + confidence + ".\n" + e);
				}
			}
		}

		MapLocalScorer mapLocalScorer = null;

		try {
			mapLocalScorer = new MapLocalScorer(new HashSet<>(), new HashSet<>(), desc2idMap, rule2ScoreMap, true, 0);
		} catch (Exception e) {
			e.printStackTrace();
		}

		NodeGraph nodeGraph = new NodeGraph(graph);

		if (ConstantsTrans.fullTNF) {
			EfficientlyCorrectHtlLearner edgeLearner = null;

			try {
				edgeLearner = new EfficientlyCorrectHtlLearner(nodeGraph, mapLocalScorer, lmbda, pgraph.name);
				edgeLearner.learn();
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			HighToLowEdgeLearner htlEdgeLearner = null;

			try {
				htlEdgeLearner = new HighToLowEdgeLearner(nodeGraph, mapLocalScorer, lmbda);
				htlEdgeLearner.learn();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// for (RelationNode from : graph.getNodes()) {
		// for (AbstractRuleEdge e : from.outEdges()) {
		// System.out.println(pgraph.nodes.get(from.id()).id + " => " +
		// pgraph.nodes.get(e.to().id()).id);
		// }
		// }

		System.out.println("learned graph! " + pgraph.name);
		writeGraph(pgraph, graph, lmbda, op);

	}

	static void writeGraph(PGraph pgraph, DirectedOntologyGraph graph, double lmbda, PrintStream op) {
		int N = graph.getNodeCount();
		int[] node2comp = new int[N];
		gtGraph scc = new gtGraph(DefaultEdge.class);

		for (int i = 0; i < N; i++) {
			scc.addVertex(i);
			List<Integer> l = new ArrayList<>();
			l.add(i);
			scc.comps.add(l);
			node2comp[i] = i;
		}

		for (RelationNode from : graph.getNodes()) {
			for (AbstractRuleEdge e : from.outEdges()) {
				scc.addEdge(from.id(), e.to().id());
			}
		}

		scc = TransClUtils.updateSCC(scc, node2comp);
		TransClUtils.writeSCC(scc, lmbda, op, pgraph);

		System.out.println("obj: " + TransClUtils.computeObj(scc, pgraph, lmbda));

	}

	// public EntailmentGraphCollapsed optimizeGraph(EntailmentGraphRaw workGraph,
	// Double confidenceThreshold) throws GraphOptimizerException{
	// DirectedOntologyGraph graph = new DirectedOneMappingOntologyGraph("work
	// graph");
	//
	//
	// /* Commented out:
	// * Here we only add nodes connected by edges in the graph, and we consider all
	// existing edges to denote entailment.
	// * We should 1) add "orphan" nodes, not connected with any other node.
	// * 2) check whether edges hold ENTAILMENT relation or not
	// * 3) consider adding edges NOT present in the graph as non-entailment edges
	// with some confidence.
	// * In addition, we should consider all the edges with confidence <
	// confidenceThreshold as non-entailment
	// */
	//
	// HashMap<String, Integer> nodeIndex = new HashMap<String, Integer>();
	// int i = 1;
	// for (EntailmentUnit node : workGraph.vertexSet()) {
	// nodeIndex.put(node.getText(), i);
	// RelationNode rNode = new RelationNode(i++,node.getText());
	// graph.addNode(rNode);
	// }
	// for (EntailmentRelation edge : workGraph.edgeSet()) {
	// Double confidence = detectConfidence(workGraph, edge.getSource(),
	// edge.getTarget(), confidenceThreshold);
	// if (confidence == TEDecision.CONFIDENCE_NOT_AVAILABLE)
	// throw new GraphOptimizerException("Unavaliable score was detected.");
	// RelationNode sourceNode = new
	// RelationNode(nodeIndex.get(edge.getSource().getText()),edge.getSource().getText());
	// RelationNode targetNode = new
	// RelationNode(nodeIndex.get(edge.getTarget().getText()),edge.getTarget().getText());
	// try {
	// graph.addEdge(new RuleEdge(sourceNode, targetNode,confidence));
	// } catch (Exception e) {
	// throw new GraphOptimizerException("Problem when adding edge
	// "+edge.getSource().getText()+" -> "+edge.getTarget().getText()+" with
	// confidence = "+confidence+".\n"+e);
	// }
	// }
	//
	// Set<AbstractOntologyGraph> componnetGraphs;
	// try {
	// componnetGraphs = graphLearner.learn(graph);
	// } catch (Exception e) {
	// throw new GraphOptimizerException("Problem with global
	// optimization.\n"+ExceptionUtils.getFullStackTrace(e));
	// }
	// EntailmentGraphCollapsed ret = new EntailmentGraphCollapsed();
	//
	// /* Commented out:
	// * Here we only add nodes connected by edges in the optimized graph, while we
	// need all the nodes from original graph to be covered in the output.
	// * We should 1) add "orphan" nodes, not connected with any other node.
	// * 2) collapse paraphrasing nodes into equivalence classes
	// */
	// EntailmentGraphRaw tmpRawGraph = new EntailmentGraphRaw();
	// for (EntailmentUnit node : workGraph.vertexSet()){
	// tmpRawGraph.addVertex(node);
	// }
	// for (AbstractOntologyGraph componnetGraph : componnetGraphs) {
	// for (AbstractRuleEdge componentEdge : componnetGraph.getEdges()) {
	// if (componentEdge.score() >= confidenceThreshold) { //TODO: do we need the
	// threshold here? Should it be confidenceThreshold or just 0 (to retain only
	// entailment edges)?
	// EntailmentUnit source =
	// workGraph.getVertex(componentEdge.from().description());
	// EntailmentUnit target= workGraph.getVertex(componentEdge.to().description());
	// EntailmentRelation edge = new EntailmentRelation(source, target, new
	// TEDecisionWithConfidence(componentEdge.score(), DecisionLabel.Entailment));
	// tmpRawGraph.addEdge(source, target, edge);
	// }
	// }
	// }
	//
	// ret = new SimpleGraphOptimizer().optimizeGraph(tmpRawGraph, 0.0); //collapse
	// paraphrasing nodes
	// return ret;
	// }

}
