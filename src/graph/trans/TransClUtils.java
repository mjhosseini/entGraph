package graph.trans;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.jgrapht.DirectedGraph;
import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.alg.GabowStrongConnectivityInspector;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import constants.ConstantsGraphs;
import constants.ConstantsTrans;
import graph.Edge;
import graph.Node;
import graph.Oedge;
import graph.PGraph;

public class TransClUtils {
	boolean checkFrgVio;
	PGraph pgraph;
	double lmbda;
	gtGraph scc;
	int[] node2comp;
	PrintStream op;
	int N;
	ConnectivityChecker connChecker;

	public TransClUtils(PGraph pgraph, PrintStream op, double lmbda, boolean checkFrgVio,
			ConnectivityChecker connChecker) {
		this.checkFrgVio = checkFrgVio;
		this.op = op;
		this.pgraph = pgraph;
		this.lmbda = lmbda;
		this.connChecker = connChecker;
		List<Edge> sortedEdges = pgraph.getSortedEdges();
		// System.out.println("heree" + pgraph.nodes.size() + " " + pgraph.sortedEdges);
		this.N = pgraph.nodes.size();
		if (ConstantsTrans.transitive) {
			this.HTLFRG(sortedEdges, this.lmbda);
		} else {
			this.formGraphPlain(sortedEdges, lmbda);
		}
	}

	static boolean isConnectedScc(DirectedGraph<Integer, DefaultEdge> scc, int[] node2comp, int i, int j) {
		int idx1 = node2comp[i];
		int idx2 = node2comp[j];
		return idx1 == idx2 || scc.containsEdge(idx1, idx2);
	}

	static void writeComponent(PGraph pgraph, gtGraph scc, int idx) {
		System.out.println("component: " + idx);
		List<Integer> nodesIdxes = scc.comps.get(idx);
		for (int i : nodesIdxes) {
			Node n = pgraph.nodes.get(i);
			System.out.println(n.id);
		}
	}

	boolean check_FRG_vio_edge(gtGraph scc, int[] node2comp, int i, int j, boolean checkFrgVio) {
		if (!checkFrgVio) {
			return false;
		}
		int idx1 = node2comp[i];
		int idx2 = node2comp[j];
		assert idx1 != idx2;
		int numVio = 0;

		for (DefaultEdge e : scc.outgoingEdgesOf(idx1)) {
			int idx3 = scc.getEdgeTarget(e);
			if (idx3 == idx1 || idx3 == idx2) {
				continue;
			}
			if (!scc.containsEdge(idx3, idx2) && !scc.containsEdge(idx2, idx3)) {
				if (ConstantsTrans.writeDebug) {
					System.out.println("frg vio: " + idx1 + " " + idx2 + " " + idx3);
					writeComponent(pgraph, scc, idx1);
					writeComponent(pgraph, scc, idx2);
					writeComponent(pgraph, scc, idx3);
				}
				numVio++;
				if (checkFrgVio) {
					return true;
				} else if (numVio > 1) {
					return true;
				}
			}
		}
		return false;
	}

	// 0: nothing added
	// 1: added, but no loop
	// 2: loop added
	int get_tr_cl_edges_scc(gtGraph scc, int[] node2comp, int i, int j) {
		PGraph pgraph = this.pgraph;
		LinkedList<Edge> q = new LinkedList<>();
		int idx1 = node2comp[i];
		int idx2 = node2comp[j];
		assert idx1 != idx2;

		q.add(new Edge(idx1, idx2, 0, pgraph));
		Set<Long> seenEdges = new HashSet<>();
		seenEdges.add((long) (idx1) + (long) N * idx2);

		double sumSims = 0;

		while (q.size() > 0) {
			Edge e = q.poll();
			idx1 = e.i;
			idx2 = e.j;

			assert idx1 != idx2;

			if (this.lmbda > 0) {
				List<Integer> nodes1 = scc.comps.get(idx1);
				List<Integer> nodes2 = scc.comps.get(idx2);
				for (int ii : nodes1) {
					for (int jj : nodes2) {
						float sim = pgraph.getW(ii, jj);
						if (ConstantsTrans.writeDebug) {
							System.out.println(
									"potentially adding: " + pgraph.nodes.get(ii).id + " " + pgraph.nodes.get(jj).id
											+ " " + (sim - this.lmbda) + " " + Edge.getConfidence(ii, jj, pgraph));
						}
						if (connChecker != null) {
							if (connChecker.isConnected(ii, jj) == 0) {
								return 0;// We can't do this!
							}
						}
						// if (ConstantsGraphs.sortEdgesConfidenceBased) {// TODO: maybe remove below
						// sumSims += (sim - this.lmbda) * Edge.getConfidence(ii, jj, pgraph);
						// } else {
						sumSims += (sim - this.lmbda);
						// ConstantsTrans.discountNegScoresHTL *
						// }
					}
				}
			}

			// Do transitive closure
			for (DefaultEdge ee : scc.incomingEdgesOf(idx1)) {
				int k = scc.getEdgeSource(ee);
				if (k != idx2 && !scc.containsEdge(k, idx2) && !seenEdges.contains((long) k + (long) N * idx2)) {
					q.add(new Edge(k, idx2, 0, pgraph));
					seenEdges.add((long) k + (long) N * idx2);
				}
			}

			for (DefaultEdge ee : scc.outgoingEdgesOf(idx2)) {
				int k = scc.getEdgeTarget(ee);
				if (k != idx1 && !scc.containsEdge(idx1, k) && !seenEdges.contains((long) idx1 + (long) N * k)) {
					q.add(new Edge(idx1, k, 0, pgraph));
					seenEdges.add((long) idx1 + (long) N * k);
				}
			}
		}
		if (ConstantsTrans.writeDebug) {
			System.out.println("sumSims: " + sumSims);
		}

		if (this.lmbda > 0 && sumSims <= 0) {
			return 0;
		} else {
			boolean loopAdded = false;
			for (long x : seenEdges) {
				if (ConstantsTrans.writeDebug) {
					System.out.println("set to true");
				}
				idx1 = (int) (x % N);
				idx2 = (int) (x / N);
				if (ConstantsTrans.writeDebug) {
					System.out.println("adding edge: " + idx1 + " " + idx2);
				}
				scc.addEdge(idx1, idx2);
				if (scc.containsEdge(idx2, idx1)) {
					if (ConstantsTrans.writeDebug) {
						System.out.println("loop added");
					}
					loopAdded = true;
				}
			}
			if (loopAdded) {
				return 2;
			} else if (seenEdges.size() > 0) {
				return 1;
			} else {
				return 0;
			}
		}

	}

	void formGraphPlain(List<Edge> sortedEdges, double lmbda) {
		int N = pgraph.nodes.size();
		this.node2comp = new int[N];
		this.scc = new gtGraph(DefaultEdge.class);

		for (int i = 0; i < N; i++) {
			scc.addVertex(i);

			List<Integer> l = new ArrayList<>();
			l.add(i);
			scc.comps.add(l);
			node2comp[i] = i;

		}

		for (Edge e : sortedEdges) {
			float sim = e.sim - (float) lmbda;
			if (sim <= lmbda) {
				break;
			}
			int i = e.i;
			int j = e.j;

			scc.addEdge(i, j);

		}

		updateSCC();

	}

	void HTLFRG(List<Edge> sortedEdges, double lmbda) {
		int N = pgraph.nodes.size();
		this.node2comp = new int[N];
		this.scc = new gtGraph(DefaultEdge.class);
		for (int i = 0; i < N; i++) {
			scc.addVertex(i);
			List<Integer> l = new ArrayList<>();
			l.add(i);
			scc.comps.add(l);
			node2comp[i] = i;
		}

		if (connChecker != null) {
			for (int i = 0; i < N; i++) {
				for (int j = 0; j < N; j++) {
					if (connChecker.isConnected(i, j) == 1) {
						scc.addEdge(i, j);
					}
				}
			}
			updateSCC();
		}

		int idx = 0;
		System.out.println("num all edges: " + pgraph.name + " " + sortedEdges.size());

		for (Edge e : sortedEdges) {
			float sim = e.sim - (float) lmbda;
			if (sim <= 0) {
				continue;
			}
			int i = e.i;
			int j = e.j;

			if (connChecker != null && connChecker.isConnected(i, j) == 0) {// These are not supposed to be connected
				continue;
			}

			if (idx % 1000 == 0) {
				System.out.println(idx + " " + pgraph.name + " " + sim);
			}

			idx++;

			if (isConnectedScc(scc, node2comp, i, j) || (check_FRG_vio_edge(scc, node2comp, i, j, checkFrgVio))) {
				continue;
			}

			if (ConstantsTrans.writeDebug) {
				System.out.println("checking: " + idx + " " + i + "=>" + j + " " + sim + " " + (e.sim * e.confidence)
						+ " " + pgraph.nodes.get(i).id + " " + pgraph.nodes.get(j).id);
			}

			if (ConstantsTrans.writeDebug) {
				System.out.println("no frg vio");
			}
			int tr = get_tr_cl_edges_scc(scc, node2comp, i, j);
			boolean loopAdded = tr == 2;
			if (ConstantsTrans.writeDebug) {
				System.out.println("anyAdded: " + (tr > 0));
			}

			if (loopAdded) {
				if (ConstantsTrans.writeDebug) {
					System.out.println("loop added");
				}
				updateSCC();
			}
		}
	}

	void updateSCC() {
		this.scc = updateSCC(scc, node2comp);
	}

	static gtGraph updateSCC(gtGraph scc, int[] node2comp) {
		GabowStrongConnectivityInspector<Integer, DefaultEdge> insp = new GabowStrongConnectivityInspector<>(scc);
		List<Set<Integer>> comps = insp.stronglyConnectedSets();

		gtGraph scc0 = scc;
		scc = new gtGraph(DefaultEdge.class);
		int[] scc0Node2comp = new int[scc0.vertexSet().size()];

		for (int i = 0; i < comps.size(); i++) {
			scc.addVertex(i);
		}

		for (int i = 0; i < comps.size(); i++) {
			Set<Integer> c = comps.get(i);
			ArrayList<Integer> cnodes = new ArrayList<>();
			for (int j : c) {
				List<Integer> l = scc0.comps.get(j);

				for (int k : l) {
					cnodes.add(k);
				}
				scc0Node2comp[j] = i;
			}

			Collections.sort(cnodes);
			scc.comps.add(cnodes);
			for (int k : cnodes) {
				node2comp[k] = i;
			}
		}

		for (DefaultEdge e : scc0.edgeSet()) {
			int v = scc.getEdgeSource(e);
			int nbr = scc.getEdgeTarget(e);
			int idx = scc0Node2comp[v];
			int idx2 = scc0Node2comp[nbr];

			if (idx != idx2 && !scc.containsEdge(idx, idx2)) {
				scc.addEdge(idx, idx2);
			}
		}
		return scc;
	}

	void writeSCC() {
		writeSCC(scc, lmbda, op, pgraph);
	}

	static List<List<Integer>> findComponents(PGraph pgraph, double lmbda) {
		SimpleGraph<Integer, DefaultEdge> sg = new SimpleGraph<>(DefaultEdge.class);
		for (int i = 0; i < pgraph.nodes.size(); i++) {
			sg.addVertex(i);
		}

		for (int i = 0; i < pgraph.nodes.size(); i++) {
			for (Oedge e : pgraph.nodes.get(i).oedges) {
				if (e.sim >= lmbda && i != e.nIdx) {
					sg.addEdge(i, e.nIdx);
				}
			}
		}

		ConnectivityInspector<Integer, DefaultEdge> ci = new ConnectivityInspector<>(sg);
		List<Set<Integer>> connectedSets = ci.connectedSets();
		List<List<Integer>> ret = new ArrayList<>();
		for (Set<Integer> c : connectedSets) {
			List<Integer> l = new ArrayList<>();
			for (int x : c) {
				l.add(x);
			}
			ret.add(l);
		}
		return ret;
	}

	// This is usable from outside
	public static void writeSCC(gtGraph scc, double lmbda, PrintStream op, PGraph pgraph) {
		// example: person#location_sim.txt
		op.println("lambda: " + lmbda + " N: " + scc.vertexSet().size());
		for (int i = 0; i < scc.vertexSet().size(); i++) {
			op.println("\ncomponent " + i);
			List<Integer> nodesIdxes = scc.comps.get(i);
			for (int idx : nodesIdxes) {
				Node n = pgraph.nodes.get(idx);
				op.println(n.id);
			}

			for (DefaultEdge e : scc.outgoingEdgesOf(i)) {
				int neigh = scc.getEdgeTarget(e);
				int firstNIdx = scc.comps.get(neigh).get(0);
				String id = pgraph.nodes.get(firstNIdx).id;
				op.println(" => " + neigh + " " + id);
			}
		}
		op.println("writing Done\n");
	}

	public static double computeObj(gtGraph scc, PGraph pgraph, double lmbda) {
		double obj = 0;
		int idx1 = 0;
		for (List<Integer> comp : scc.comps) {
			for (int i : comp) {

				// edges inside the component
				for (int j : comp) {
					if (i != j) {
						obj += (pgraph.getW(i, j) - lmbda);
					}
				}

				// edges outside the component
				for (DefaultEdge e : scc.outgoingEdgesOf(idx1)) {
					int idx2 = scc.getEdgeTarget(e);
					if (idx1 != idx2) {
						for (int j : scc.comps.get(idx2)) {
							obj += pgraph.getW(i, j) - lmbda;
						}
					}
				}
			}

			idx1++;
		}
		return obj;
	}

}
