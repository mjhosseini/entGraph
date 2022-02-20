package graph.EmbIter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;

import constants.ConstantsMNEmbIter;
import graph.PGraph;
import graph.PredSim;
import graph.SimpleScore;

public class MNEmbIter {

	public static Set<String> targetRels = new HashSet<>();
	public static Map<String, List<PredSim>> rels2Sims;
	public static Map<String, List<PredSim>> invRels2Sims;

	public void iterate(PGraph pgraph) {
		int N = pgraph.nodes.size();
		DefaultDirectedWeightedGraph<Integer, DefaultWeightedEdge> g0 = pgraph.formWeightedGraph(pgraph.sortedEdges, N);
		List<String> prevIds = new ArrayList<>();
		Map<String, Integer> prevPred2Node = new HashMap<>();

		for (int i = 0; i < N; i++) {
			prevIds.add(pgraph.nodes.get(i).id);
			prevPred2Node.put(prevIds.get(i), i);
		}

		DefaultDirectedWeightedGraph<Integer, DefaultWeightedEdge> nextG = g0;

		List<DefaultDirectedWeightedGraph<Integer, DefaultWeightedEdge>> gs = new ArrayList<>();
		List<List<String>> allPredsList = new ArrayList<>();
		gs.add(g0);
		List<String> nextIds = new ArrayList<>();
		for (String s : prevIds) {
			nextIds.add(s);
		}
		allPredsList.add(nextIds);

		for (int k = 0; k < ConstantsMNEmbIter.maxEmbIter; k++) {
			System.out.println("iter: " + (k + 1));
			nextG = getNextEmbeddingGr(nextG, MNEmbIter.invRels2Sims, prevIds, prevPred2Node, targetRels,
					k == ConstantsMNEmbIter.maxEmbIter - 1);
			gs.add(nextG);
			nextIds = new ArrayList<>();
			for (String s : prevIds) {
				nextIds.add(s);
			}
			allPredsList.add(nextIds);
			System.out.println("num preds: " + nextIds.size());
		}
		String fnameEmbeds = pgraph.fname.substring(0, pgraph.fname.lastIndexOf('_')) + ConstantsMNEmbIter.embSuffix;
		writeEmbeddingResults(pgraph, gs, allPredsList, fnameEmbeds);
	}

	void writeEmbeddingResults(PGraph pgraph, List<DefaultDirectedWeightedGraph<Integer, DefaultWeightedEdge>> gs,
			List<List<String>> allPredsList, String fnameEmbeds) {
		// list of all predicates can be found from the last graph. The indexes
		// are also the same (if existing) with previous graphs
		PrintStream op = null;
		try {
			op = new PrintStream(new File(fnameEmbeds));
		} catch (Exception e) {
		}
		List<String> predList = allPredsList.get(allPredsList.size() - 1);
		op.println("types: " + pgraph.name + ", " + " label propagation num preds: " + predList.size());
		DefaultDirectedWeightedGraph<Integer, DefaultWeightedEdge> lastG = gs.get(gs.size() - 1);
		for (int i = 0; i < predList.size(); i++) {
			op.println("predicate: " + predList.get(i));
			op.println("num max neighbors: " + lastG.outgoingEdgesOf(i).size());
			op.println();
			for (int iter = 0; iter < gs.size(); iter++) {
				DefaultDirectedWeightedGraph<Integer, DefaultWeightedEdge> thisG = gs.get(iter);
				op.println("iter " + iter + " sims");
				List<SimpleScore> scores = new ArrayList<>();
				if (thisG.containsVertex(i)) {
					for (DefaultWeightedEdge e : thisG.outgoingEdgesOf(i)) {
						int j = thisG.getEdgeTarget(e);
						double w = thisG.getEdgeWeight(e);
						scores.add(new SimpleScore("", predList.get(j), (float) w));
					}

					Collections.sort(scores, Collections.reverseOrder());
					for (SimpleScore sc : scores) {
						op.println(sc.pred2 + " " + sc.score);
					}
				}
				op.println();
			}
		}
		op.close();
	}

	DefaultDirectedWeightedGraph<Integer, DefaultWeightedEdge> getNextEmbeddingGr(
			DefaultDirectedWeightedGraph<Integer, DefaultWeightedEdge> g0, Map<String, List<PredSim>> invrels2Sims,
			List<String> prevIds, Map<String, Integer> prevPred2Node, Set<String> targetRels, boolean addNewRel) {

		int prevN = g0.vertexSet().size();
		DefaultDirectedWeightedGraph<Integer, DefaultWeightedEdge> g = new DefaultDirectedWeightedGraph<>(
				DefaultWeightedEdge.class);

		Map<String, Integer> curPred2Node = new HashMap<>();
		List<String> curIds = new ArrayList<>();

		for (int i = 0; i < prevN; i++) {
			g.addVertex(i);
			curIds.add(prevIds.get(i));
			curPred2Node.put(prevIds.get(i), i);
		}

		int curN = prevN;

		if (addNewRel) {
			// Now, add nodes from target relations!
			for (int r = 0; r < prevN; r++) {
				// System.out.println(prevIds.get(r));
				List<PredSim> pss = getTypedPredSimList(prevIds.get(r), invrels2Sims, null, null, false);

				for (PredSim ps : pss) {
					String rawPred = ps.pred.substring(0, ps.pred.indexOf("#"));
					// System.out.println("rawPred: " + rawPred);
					if (!curPred2Node.containsKey(ps.pred) && targetRels.contains(rawPred)) {
						g.addVertex(curN);
						curIds.add(ps.pred);
						curPred2Node.put(ps.pred, curN);
						// System.out.println("adding new node: " +
						// curIds.get(curN)
						// + " based on " + prevIds.get(r));
						curN++;
					}
				}
			}

			System.out.println("new nodes added: " + prevN + " " + curN);

		}

		Map<String, List<PredSim>> currentInvPredToSimList = new HashMap<>();

		// sum of similarities of all rs that have p around them...
		// double[] neighWeights = new double[curN];
		// Map<Edge, Double> edgeSumWeights = new HashMap<>();

		int numAllProp = 0;

		for (int r = 0; r < g0.vertexSet().size(); r++) {
			if (r % 100 == 0) {
				System.out.println("r: " + r + " " + numAllProp);
			}

			List<PredSim> pss = getTypedPredSimList(curIds.get(r), invrels2Sims, currentInvPredToSimList, curPred2Node,
					true);

			// for (PredSim ps : pss) {
			//
			// if (!curPred2Node.containsKey(ps.pred)) {
			// // System.out.println("can't find: " + ps.pred);
			// continue;
			// }
			// int p = curPred2Node.get(ps.pred);
			// neighWeights[p] += ps.sim;
			// }

			for (DefaultWeightedEdge e : g0.outgoingEdgesOf(r)) {
				int rp = g0.getEdgeTarget(e);
				// DefaultWeightedEdge e0 = g0.getEdge(r, rp);// It must have
				// the
				// // edge because it's
				// // a superset of
				// // gDits!
				double sim = g0.getEdgeWeight(e);

				List<PredSim> qss = getTypedPredSimList(curIds.get(rp), invrels2Sims, currentInvPredToSimList,
						curPred2Node, true);

				// System.out.println("curId: " + curIds.get(r));
				// System.out.println("neigh: " + curIds.get(rp));

				for (PredSim ps : pss) {
					// if (!curPred2Node.containsKey(ps.pred)) {
					// // System.out.println("no pred " + ps.pred);
					// continue;
					// }
					int p = curPred2Node.get(ps.pred);

					for (PredSim qs : qss) {
						// if (!curPred2Node.containsKey(qs.pred)) {
						// continue;
						// }

						int q = curPred2Node.get(qs.pred);

						// System.out.println("avg for 1: " + ps);
						// System.out.println("avg for 2: " + qs);

						double w;
						DefaultWeightedEdge ee;
						if (!g.containsEdge(p, q)) {
							ee = g.addEdge(p, q);
							numAllProp++;
							g.setEdgeWeight(ee, 0);
							w = 0;
						} else {
							ee = g.getEdge(p, q);
							w = g.getEdgeWeight(ee);
						}
						if (!ConstantsMNEmbIter.weightEdgeSimilarities) {
							g.setEdgeWeight(ee, w + sim * ps.sim * qs.sim);
						} else {
							List<Integer> l = new ArrayList<>();
							// r and rp are always in g0. p and q might be new!
							if (p < prevN) {
								l.add(g0.outDegreeOf(p));
							}
							l.add(g0.outDegreeOf(r));
							if (q < prevN) {
								l.add(g0.inDegreeOf(q));
							}

							l.add(g0.inDegreeOf(rp));

							double beta = Math.max(Collections.min(l), 1);

							g.setEdgeWeight(ee, w + sim * ps.sim * qs.sim * beta);
						}

						// if (sim != 0) {// similar to python. Avg only if
						// r->rp
						// exists!
						// ImmutablePair<Integer, Integer> ip = new
						// ImmutablePair<Integer, Integer>(p, q);
						// Edge mye = new Edge(p, q, -1);
						// if (!edgeSumWeights.containsKey(mye)) {
						// edgeSumWeights.put(mye, 0.0);
						// }
						// // System.out.println("sim1, sim2: " + ps.sim + " "
						// // + qs.sim);
						// edgeSumWeights.put(mye, edgeSumWeights.get(mye) +
						// (ps.sim * qs.sim));
						// }

					}
				}
			}
		}

		Map<String, List<PredSim>> currentPredToSimList = new HashMap<>();

		for (int p = 0; p < curN; p++) {
			if (p % 100 == 0) {
				System.out.println("p: " + p);
			}
			if (!g.containsEdge(p, p)) {
				g.addEdge(p, p);
			}

			List<PredSim> pss = getTypedPredSimList(curIds.get(p), MNEmbIter.rels2Sims, currentPredToSimList,
					curPred2Node, true);

			DefaultWeightedEdge ee = g.getEdge(p, p);
			g.setEdgeWeight(ee, 1);

			for (DefaultWeightedEdge e : g.outgoingEdgesOf(p)) {
				int q = g.getEdgeTarget(e);
				if (p == q) {
					continue;
				}

				List<PredSim> qss = getTypedPredSimList(curIds.get(q), MNEmbIter.rels2Sims, currentPredToSimList,
						curPred2Node, true);

				double wCor = 0;
				// For pairs that both are in the graph, we consider their feats
				// (even if zero)
				for (PredSim ps : pss) {
					int r = curPred2Node.get(ps.pred);
					if (curPred2Node.get(ps.pred) >= prevN) {
						// System.out.println("cont1: " + curIds.get(p) + " " +
						// ps.pred + " " + curPred2Node.get(ps.pred)
						// + " " + prevN);
						continue;
					}
					for (PredSim qs : qss) {

						if (curPred2Node.get(qs.pred) >= prevN) {
							// System.out.println("cont2: " + curIds.get(q) + ""
							// + qs.pred + " "
							// + curPred2Node.get(qs.pred) + " " + prevN);
							continue;
						}
						// else{
						// System.out.println("add w: " + ps +" " + qs);
						// }
						if (!ConstantsMNEmbIter.weightEdgeSimilarities) {
							wCor += ps.sim * qs.sim;
						} else {
							int rp = curPred2Node.get(qs.pred);

							List<Integer> l = new ArrayList<>();
							// r and rp are always in g0. p and q might be new!
							if (p < prevN) {
								l.add(g0.outDegreeOf(p));
							}
							l.add(g0.outDegreeOf(r));
							if (q < prevN) {
								l.add(g0.inDegreeOf(q));
							}

							l.add(g0.inDegreeOf(rp));

							double beta = Math.max(Collections.min(l), 1);
							wCor += ps.sim * qs.sim * beta;
						}
					}
				}

				// double w = g.getEdgeWeight(e) / (neighWeights[p] *
				// neighWeights[q]);

				// ImmutablePair<Integer, Integer> ip = new
				// ImmutablePair<Integer, Integer>(p, q);
				// Edge mye = new Edge(p, q, -1);
				double w;
				// if (edgeSumWeights.containsKey(mye)) {
				// w = g.getEdgeWeight(e) / edgeSumWeights.get(mye);

				w = g.getEdgeWeight(e) / wCor;

				// System.out.println("avg: " + w + " " + g.getEdgeWeight(e) + "
				// " + wCor);
				// System.out.println("avg: "+curIds.get(p) + " " +
				// curIds.get(q) + " ");

				if (w > 1.01) {
					System.out.println("bug: " + w + " " + g.getEdgeWeight(e) + " " + wCor);
					System.out.println(curIds.get(p) + " " + curIds.get(q) + " ");
				}
				if (wCor != 1) {
					g.setEdgeWeight(e, w);
				}

				// }
				// else {
				// // System.out.println("no wight: "+curIds.get(p)+"
				// // "+curIds.get(q));
				// w = g.getEdgeWeight(e);
				// }

				// double prevSim = 0;
				// if (g0.containsEdge(p, q)) {
				// ee = g0.getEdge(p, q);
				// prevSim = g0.getEdgeWeight(ee);
				// }
				// if (w != prevSim) {
				// // System.out.println("emb sim: " + curIds.get(p) + " " +
				// // curIds.get(q) + " " + w);
				// // System.out.println("prev sim: " + prevSim);
				// }
			}
		}

		//
		// List<PredSim> pss = rels2Sims.get(r);
		//
		// for (DefaultWeightedEdge e : g.outgoingEdgesOf(p)) {
		//
		// }
		// }

		// make prevIds and prevPred2Node equal current ones
		prevPred2Node.clear();
		prevIds.clear();
		for (String p : curIds) {
			prevIds.add(p);
			prevPred2Node.put(p, curPred2Node.get(p));
		}
		// System.out.println("here prevId size: "+prevIds.size());
		return g;
	}

	// typed similar ones that are in pgraph. We force the types to be the same
	// (and same order)
	// onlyGraph: get neighbors only from the graph (don't wanna add anymore!)
	List<PredSim> getTypedPredSimList(String rId, Map<String, List<PredSim>> invrels2Sims,
			Map<String, List<PredSim>> currentInvPredToSimList, Map<String, Integer> curPred2Node, boolean onlyGraph) {

		if (onlyGraph && currentInvPredToSimList.containsKey(rId)) {
			return currentInvPredToSimList.get(rId);
		}

		String[] ss = rId.split("#");
		String rawPred = ss[0];// without types
		String type1 = ss[1];
		String type2 = ss[2];

		List<PredSim> pss = invrels2Sims.get(rawPred);

		if (pss == null) {
			pss = new ArrayList<>();
			pss.add(new PredSim(rawPred, 1));
		}

		List<PredSim> ret = new ArrayList<>();

		// System.out.println("returning simlist of " + rId);

		for (PredSim ps : pss) {
			String cand = ps.pred + "#" + type1 + "#" + type2;
			if (onlyGraph && !curPred2Node.containsKey(cand)) {
				continue;
			}
			ret.add(new PredSim(cand, ps.sim));
			// System.out.println(ret.get(ret.size() - 1));

		}

		// if (ret.size() > 1) {
		// System.out.println("OK, found a similar one!");
		// }
		if (onlyGraph) {
			currentInvPredToSimList.put(rId, ret);
		}
		return ret;
	}

	public static void read_rels_sim(String fpath, boolean isCCG, boolean useSims)
			throws NumberFormatException, IOException {
		Map<String, List<PredSim>> rels2Sims = new HashMap<>();

		// The reason is that we're getting top 30 relations. So, if r is in p's
		// neigh, it might not be otherwise.
		Map<String, List<PredSim>> invRels2Sims = new HashMap<>();
		if (!useSims) {
			return;
		}

		String line = null;
		BufferedReader br = new BufferedReader(new FileReader(fpath));

		while ((line = br.readLine()) != null) {
			String[] ss = line.split("\t");
			for (int i = 0; i < ss.length; i++) {
				ss[i] = ss[i].trim();
			}
			String p = ss[0];

			String modifier = "";
			if (isCCG) {
				int ridx = p.lastIndexOf("__");
				if (ridx != -1) {
					modifier = p.substring(0, ridx);
					p = p.substring(ridx + 2);
				}
			}
			List<PredSim> qs = new ArrayList<>();
			Set<String> currentPreds = new HashSet<>();
			int idx = 1;
			while (idx < ss.length && qs.size() < ConstantsMNEmbIter.maxNeighs) {
				String q = ss[idx];
				idx += 1;
				float sim = Float.parseFloat(ss[idx]);

				if (sim < ConstantsMNEmbIter.relMinSim) {
					break;
				}
				idx += 1;

				if (isCCG) {
					int ridx = q.lastIndexOf("__");
					if (ridx != -1) {
						q = q.substring(ridx + 2);
					}

					try {
						if (!PGraph.sameCCGArgs(p, q)) {
							continue;
						}
					} catch (Exception e) {
						// System.out.println("exception for: " + q);

					}

					if (!modifier.equals("")) {
						q = modifier + "__" + q;
					}

				}

				if (!currentPreds.contains(q)) {
					qs.add(new PredSim(q, sim));
					currentPreds.add(q);
				}

			}

			if (isCCG && !modifier.equals("")) {
				p = modifier + "__" + p;
			}
			if (!currentPreds.contains(p)) {
				qs.add(0, new PredSim(p, 1));
			}
			rels2Sims.put(p, qs);

			for (PredSim qsim : qs) {
				if (!invRels2Sims.containsKey(qsim.pred)) {
					invRels2Sims.put(qsim.pred, new ArrayList<>());
				}
				invRels2Sims.get(qsim.pred).add(new PredSim(p, qsim.sim));
			}

			// System.out.println("p: " + p);
			// System.out.println("sims: ");
			// for (PredSim qq : qs) {
			// System.out.print(qq.pred + " ");
			// }
			// System.out.println();
		}

		// make sure invRels2Sims has itself!
		for (String q : invRels2Sims.keySet()) {
			List<PredSim> ql = invRels2Sims.get(q);
			Set<String> qPreds = new HashSet<>();
			for (PredSim ps : ql) {
				qPreds.add(ps.pred);
			}
			if (!qPreds.contains(q)) {
				ql.add(0, new PredSim(q, 1));
			}
		}

	}
	
	static {
		try {
			MNEmbIter.read_rels_sim(ConstantsMNEmbIter.fpath, true, true);
		} catch (NumberFormatException | IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		// TODO: this should be implemented after the refactor on 24 Oct 2018
	}
}
