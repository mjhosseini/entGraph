package graph.trans;

import java.util.ArrayList;
import java.util.List;

import graph.PGraph;
import lpsolve.LpSolve;
import lpsolve.LpSolveException;

public class LinProg {
	LpSolve lp;
	int N;
	double lmbda;
	PGraph pgraph;
	List<Integer> idxes;// only consider a subset of indices

	public LinProg(PGraph pgraph, double lmbda, List<Integer> idxes) {
		this.pgraph = pgraph;
		this.lmbda = lmbda;
		this.idxes = idxes;
		this.N = idxes.size();
	}

	void formILPBasic() throws LpSolveException {

		this.lp = LpSolve.makeLp(0, N * N);
		
		this.lp.setBasiscrash(0);

		// couldn't construct a new model...
		if (lp.getLp() == 0) {
			System.err.println("Couldn't build a model...");
			System.exit(1);
		}

		lp.setAddRowmode(true);

		// var idx (1 based)
		int[] column = new int[N * N];

		// makes building the model faster if it is done rows by row

		// set obj fn
		double[] weights = new double[N * N];
		double w;
		for (int i = 0; i < N; i++) {
			for (int j = 0; j < N; j++) {
				int idx = 1 + i * N + j;
				// make binary
				lp.setInt(idx, true);
				// lp.setColName(idx, pgraph.nodes.get(i).id + " " + pgraph.nodes.get(j).id);
				column[idx - 1] = idx;
				if (i == j) {
					w = 0;
					lp.setBounds(idx, 1.0, 1.0);
				} else {
					if (!pgraph.nodes.get(idxes.get(i)).idx2oedges.containsKey(idxes.get(j))) {
						w = 0;
					} else {
						w = pgraph.nodes.get(idxes.get(i)).idx2oedges.get(idxes.get(j)).sim;
					}
					w -= lmbda;
					lp.setBounds(idx, 0.0, 1.0);
				}
				// if (w<0) {
				// w=-1;
				// }
				// else {
				// w = 1;
				// }
				weights[idx - 1] = w;

			}
		}

		// set obj function
		lp.setObjFnex(N * N, weights, column);

		// make sure maximizes
		lp.setMaxim();
		// rowmode should be turned off again when done building the model
		lp.setAddRowmode(false);

	}

	void addTransitivityCons(int[] vio) {
		try {

			lp.addConstraintex(3, new double[] { 1, 1, -1 }, vio, LpSolve.LE, 1);
		} catch (LpSolveException e) {
			e.printStackTrace();
		}
	}

	double[] solveILPIncremental() {
		try {
			this.formILPBasic();
		} catch (LpSolveException e1) {
			e1.printStackTrace();
		}
		List<int[]> vios = new ArrayList<>();
		double[] sol = null;
		while (true) {

			try {
				sol = solveSystem();
				// System.out.println("sol: " + Arrays.toString(sol));
			} catch (LpSolveException e) {
				e.printStackTrace();
			}
			vios = findVio(sol);
			System.out.println("num vios: " + vios.size());
			for (int[] vio : vios) {
				addTransitivityCons(vio);
			}

			if (vios.size() == 0) {
				break;
			}
		}
		/* clean up such that all used memory by lpsolve is freed */
		if (lp.getLp() != 0) {
			lp.deleteLp();
		}

		return sol;

	}

	List<int[]> findVio(double[] sol) {
		List<int[]> vios = new ArrayList<>();
		for (int i = 0; i < N; i++) {
			for (int j = 0; j < N; j++) {
				if (i == j) {
					continue;
				}
				if (isConnected(sol, i, j)) {
					for (int k = 0; k < N; k++) {
						if (isConnected(sol, j, k) && !isConnected(sol, i, k)) {
							// System.out.println("adding vio: " + i + " " + j + " " + k);
							vios.add(new int[] { i * N + j + 1, j * N + k + 1, i * N + k + 1 });
						}
					}
				}
			}
		}
		return vios;
	}

	boolean isConnected(double[] sol, int i, int j) {
		int idx = i * N + j;
		return sol[idx] == 1;
	}

	// void solve_ILP_incremental() {
	// form_ILP_basic();
	// lp = self.lp;
	// row = [0]*self.N**2
	//
	// num_c = 0
	//
	//
	//
	// while 1==1:
	// t0 = time.time()
	// vars = self.solve_ilp()
	// t1 = time.time()
	// print "num cs: ", num_c
	// print "inc solve: num constraints", self.N," ", num_c, " ", (t1-t0)
	// vio = self.find_vio(vars)
	//
	// print "vio: ", vio
	//
	// cs = []
	// for (i,j,k) in vio:
	//
	// if (i,j) in self.prunedEdges or (j,k) in self.prunedEdges or (i,k) in
	// self.prunedEdges:
	// #print "not added: ", (i,j), " ", (j,k), " ", (i,k)
	// continue
	//
	// idx1 = i*self.N+j
	// idx2 = j*self.N+k
	// idx3 = i*self.N+k
	// c = (idx1,idx2,idx3)
	// num_c += 1
	// cs.append(c)
	//
	// if (len(cs)==0):
	// break
	// self.add_constraints(cs,row)
	//
	// return vars
	// }

	/**
	 * @return An array of values for each variable
	 */
	public double[] solveSystem() throws LpSolveException {
		double[] row = null;
		int numVars = N * N;

		// Only show important messages while solving
		lp.setVerbose(LpSolve.IMPORTANT);

		// Calculate a solution
		int ret = lp.solve();

		// If got an answer
		if (ret == LpSolve.OPTIMAL) {
			row = new double[numVars];

			/* objective value */
			System.out.println("Objective value: " + lp.getObjective());
			/* variable values */
			lp.getVariables(row);

			// print answers
			for (int idx = 0; idx < numVars; idx++) {
				if (row[idx] == 1.0) {
					int i = idx / N;
					int j = idx % N;
					// System.out.println(pgraph.nodes.get(idxes.get(i)).id + "=>" +
					// pgraph.nodes.get(idxes.get(j)).id);
					// System.out.println(idxes.get(i) + "=>" + idxes.get(j));
				}
			}
		} else {
			System.out.println("Couldn't get an answer");
		}
		return row;
	}

	public void clear() {
		if (lp.getLp() != 0)
			lp.deleteLp();
	}
}
