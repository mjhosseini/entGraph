package graph.trans;

import java.util.ArrayList;
import java.util.List;

import graph.PGraph;
import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.ParameterSet;
import ilog.cplex.IloCplex.UnknownObjectException;
import lpsolve.LpSolveException;

public class LinProgCplex {
	IloCplex lp;
	int N;
	double lmbda;
	PGraph pgraph;
	List<Integer> idxes;// only consider a subset of indices
	IloNumVar[] x;
	
	static {
//		ParameterSet parameterSet = new IloCplex.ParameterSet();
//		parameterSet.setParam(IloCplex.Param.Threads, 8);
				
	}

	public LinProgCplex(PGraph pgraph, double lmbda, List<Integer> idxes) {
		this.pgraph = pgraph;
		this.lmbda = lmbda;
		this.idxes = idxes;
		this.N = idxes.size();
		
	}

	void formILPBasic() throws LpSolveException, IloException {
		System.out.println("forming basic ILP");
		// this.lp = LpSolve.makeLp(0, N * N);
		this.lp = new IloCplex();

		IloNumVar[][] var = new IloNumVar[1][];
		// IloRange[][] rng = new IloRange[1][];

//		 String[] varName = new String[N * N];

		// set obj fn
		double[] weights = new double[N * N];
		double w;
		for (int i = 0; i < N; i++) {
			for (int j = 0; j < N; j++) {

				int idx = i * N + j;

				// varName[idx] = i + "=>" + j;

				if (i == j) {
					// lb[idx] = ub[idx] = 1;
					w = 0;
				} else {
					// lb[idx] = 0;
					// ub[idx] = 1;
					if (!pgraph.nodes.get(idxes.get(i)).idx2oedges.containsKey(idxes.get(j))) {
						w = 0;
					} else {
						w = pgraph.getW(i, j);
					}
					w -= lmbda;
				}
				// if (w<0) {
				// w=-1;
				// }
				// else {
				// w = 1;
				// }
				weights[idx] = w;
			}
		}

		IloLPMatrix lpMat = lp.addLPMatrix();

		x = lp.boolVarArray(lp.columnArray(lpMat, N * N));
		var[0] = x;

		// rng[0] = new IloRange[N];

		for (int i = 0; i < N; i++) {
			lp.addEq(lp.prod(1, x[i*N+i]), 1);
		}
		
		System.out.println("basic ILP formed");

		// set obj function
		lp.addMaximize(lp.scalProd(x, weights));
		
	}

	void addTransitivityCons(int[] vio) {
		try {

			lp.addLe(lp.sum(lp.prod(1, x[vio[0]]), lp.prod(1, x[vio[1]]), lp.prod(-1, x[vio[2]])), 1);
		} catch (IloException e) {
			e.printStackTrace();
		}
	}

	double[] solveILPIncremental() {
		try {
			this.formILPBasic();
		} catch (LpSolveException e1) {
			e1.printStackTrace();
		} catch (IloException e) {
			e.printStackTrace();
		}
		List<int[]> vios = new ArrayList<>();
		double[] sol = null;
		while (true) {

			try {
				sol = solveSystem();
				// System.out.println("sol: " + Arrays.toString(sol));
			} catch (LpSolveException | IloException e) {
				e.printStackTrace();
			}
			vios = findVio(sol);
			
			System.err.println("num vios: " + vios.size());
//			System.err.println("sol: ");
//			for (int i=0; i<sol.length; i++) {
//				System.err.println(sol[i]);
//			}
			for (int[] vio : vios) {
				addTransitivityCons(vio);
			}

			if (vios.size() == 0) {
				break;
			}
		}

		lp.end();

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
							vios.add(new int[] { i * N + j, j * N + k, i * N + k});
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
	 * @throws IloException
	 * @throws UnknownObjectException
	 */
	public double[] solveSystem() throws LpSolveException, UnknownObjectException, IloException {
		double[] sol = null;
		
		lp.exportModel("lpex1.lp");

		if (lp.solve()) {
			sol = lp.getValues(x);
			// double[] dj = cplex.getReducedCosts(var[0]);
			// double[] pi = cplex.getDuals(rng[0]);
			// double[] slack = cplex.getSlacks(rng[0]);

			lp.output().println("Solution status = " + lp.getStatus());
			lp.output().println("Solution value  = " + lp.getObjValue());

			// int nvars = x.length;
			// for (int j = 0; j < nvars; ++j) {
			// cplex.output().println("Variable " + j +
			// ": Value = " + x[j] +
			// " Reduced cost = " + dj[j]);
			// }

			// int ncons = slack.length;
			// for (int i = 0; i < ncons; ++i) {
			// cplex.output().println("Constraint " + i + ": Slack = " + slack[i] + " Pi = "
			// + pi[i]);
			// }
		} else {
			System.err.println("Couldn't get an answer");
//			System.exit(0);
		}
		return sol;
	}

}
