package entailment.learning;

import java.util.Vector;
import java.util.Arrays;

import lpsolve.*;

public class LinearProgramming {
	LpSolve lp;
	public static int numClasses = 6;
	Vector<double[]> sets = new Vector<double[]>();
	Vector<int[]> transitives = new Vector<int[]>();

	public LinearProgramming(int classes) {
		this.numClasses = classes;
	}

	public int execute() throws LpSolveException {
		LpSolve lp;
		int Ncol, j, ret = 0;

		/*
		 * We will build the model row by row So we start with creating a model with 0
		 * rows and 2 columns
		 */
		Ncol = 2; /* there are two variables in the model */

		/* create space large enough for one row */
		int[] colno = new int[Ncol];
		double[] row = new double[Ncol];

		lp = LpSolve.makeLp(0, Ncol);
		if (lp.getLp() == 0)
			ret = 1; /* couldn't construct a new model... */

		if (ret == 0) {
			/*
			 * let us name our variables. Not required, but can be useful for debugging
			 */
			lp.setColName(1, "x");
			lp.setColName(2, "y");

			lp.setAddRowmode(true); /*
									 * makes building the model faster if it is done rows by row
									 */

			/* construct first row (120 x + 210 y <= 15000) */
			j = 0;

			colno[j] = 1; /* first column */
			row[j++] = 120;

			colno[j] = 2; /* second column */
			row[j++] = 210;

			/* add the row to lpsolve */
			lp.addConstraintex(j, row, colno, LpSolve.LE, 15000);
		}

		if (ret == 0) {
			/* construct second row (110 x + 30 y <= 4000) */
			j = 0;

			colno[j] = 1; /* first column */
			row[j++] = 110;

			colno[j] = 2; /* second column */
			row[j++] = 30;

			/* add the row to lpsolve */
			lp.addConstraintex(j, row, colno, LpSolve.LE, 4000);
		}

		if (ret == 0) {
			/* construct third row (x + y <= 75) */
			j = 0;

			colno[j] = 1; /* first column */
			row[j++] = 1;

			colno[j] = 2; /* second column */
			row[j++] = 1;

			/* add the row to lpsolve */
			lp.addConstraintex(j, row, colno, LpSolve.LE, 75);
		}

		if (ret == 0) {
			lp.setAddRowmode(false); /*
										 * rowmode should be turned off again when done building the model
										 */

			/* set the objective function (143 x + 60 y) */
			j = 0;

			colno[j] = 1; /* first column */
			row[j++] = 143;

			colno[j] = 2; /* second column */
			row[j++] = 60;

			/* set the objective in lpsolve */
			lp.setObjFnex(j, row, colno);
		}

		if (ret == 0) {
			/* set the object direction to maximize */
			lp.setMaxim();

			/*
			 * just out of curioucity, now generate the model in lp format in file model.lp
			 */
			lp.writeLp("model.lp");

			/* I only want to see important messages on screen while solving */
			lp.setVerbose(LpSolve.IMPORTANT);

			/* Now let lpsolve calculate a solution */
			ret = lp.solve();
			if (ret == LpSolve.OPTIMAL)
				ret = 0;
			else
				ret = 5;
		}

		if (ret == 0) {
			/* a solution is calculated, now lets get some results */

			/* objective value */
			System.out.println("Objective value: " + lp.getObjective());

			/* variable values */
			lp.getVariables(row);
			for (j = 0; j < Ncol; j++)
				System.out.println(lp.getColName(j + 1) + ": " + row[j]);

			/* we are done now */
		}

		/* clean up such that all used memory by lpsolve is freed */
		if (lp.getLp() != 0)
			lp.deleteLp();

		return (ret);
	}

	public int myexecute() throws LpSolveException {
		LpSolve lp;
		int Ncol, j, ret = 0;

		/*
		 * We will build the model row by row So we start with creating a model with 0
		 * rows and 2 columns
		 */
		Ncol = 2; /* there are two variables in the model */

		/* create space large enough for one row */
		int[] column = new int[Ncol];
		double[] row = new double[Ncol];

		lp = LpSolve.makeLp(0, Ncol);
		/* couldn't construct a new model... */
		if (lp.getLp() == 0) {
			System.err.println("Couldn't build a model...");
			System.exit(1);
		}

		// --------------------------------------------------
		// CREATE VARIABLES
		// --------------------------------------------------
		/* name our variables. Not required, but can be useful for debugging */
		// lp.setColName(1, "x");
		// lp.setColName(2, "y");

		lp.setAddRowmode(true); /*
								 * makes building the model faster if it is done rows by row
								 */

		// --------------------------------------------------
		// ADD CONSTRAINTS
		// --------------------------------------------------
		int rows = 0;
		column[0] = 1; /* first column var num */
		column[1] = 2; /* first column var num */

		// --------------------
		// set int constraint on variables
		lp.setInt(1, true);
		lp.setInt(2, true);
		// --------------------
		// set bound on variables from 0 to 1
		lp.setBounds(1, 0.0, 1.0);
		lp.setBounds(2, 0.0, 1.0);

		// --------------------
		// construct row (x + y = 1)
		j = 0;

		row[j++] = 1;
		row[j++] = 1;

		// add the row to lpsolve
		rows++;
		lp.addConstraintex(j, row, column, LpSolve.EQ, 1);

		// rowmode should be turned off again when done building the model
		lp.setAddRowmode(false);

		// --------------------------------------------------
		// OBJECTIVE FUNCTION
		// --------------------------------------------------
		j = 0;

		column[j] = 1; /* first column */
		row[j++] = .3012;

		column[j] = 2; /* second column */
		row[j++] = .03;

		/* set the objective in lpsolve */
		lp.setObjFnex(j, row, column);

		/* set the object direction to maximize */
		lp.setMaxim();

		/*
		 * just out of curioucity, now generate the model in lp format in file model.lp
		 */
		/// lp.writeLp("model.lp");

		/* I only want to see important messages on screen while solving */
		lp.setVerbose(LpSolve.IMPORTANT);

		/* Now let lpsolve calculate a solution */
		ret = lp.solve();
		if (ret == LpSolve.OPTIMAL)
			ret = 0;
		else
			ret = 5;

		/* a solution is calculated, now lets get some results */

		/* objective value */
		System.out.println("Objective value: " + lp.getObjective());

		/* variable values */
		lp.getVariables(row);
		for (j = 0; j < Ncol; j++)
			System.out.println(lp.getColName(j + 1) + ": " + row[j]);

		/* clean up such that all used memory by lpsolve is freed */
		if (lp.getLp() != 0)
			lp.deleteLp();

		return (ret);
	}

	/**
	 * Returns the variable index of the first relation
	 */
	public int addProbs(double probs[]) {
		if (probs.length != numClasses)
			System.err.println("Wrong number of vars");
		else {
			sets.add(probs);
		}
		return sets.size();
	}

	/**
	 * Add a transitive closure constraint
	 * 
	 * @param closed
	 *            The transitive relation that must be true
	 */
	public void addTransitiveConstraint(int one, int two, int closed) {
		int arr[] = { one, two, closed };
		transitives.add(arr);
	}

	public void addInverseConstraint(int index1, int newIndex) {
		// ?????????
		// This is called from Experiment ... but it disappeared somehow.
		// Need to re-write this code!!!

	}

	public void buildSystem() throws LpSolveException {
		int j, numVars = sets.size() * numClasses;

		int[] column = new int[numClasses];

		lp = LpSolve.makeLp(0, numVars);
		// couldn't construct a new model...
		if (lp.getLp() == 0) {
			System.err.println("Couldn't build a model...");
			System.exit(1);
		}
		// makes building the model faster if it is done rows by row
		lp.setAddRowmode(true);

		// --------------------------------------------------
		// ADD CONSTRAINTS
		// --------------------------------------------------
		int rows = 0;
		double ones[] = new double[numClasses];
		double transOnes[] = new double[3];
		Arrays.fill(ones, 1);
		Arrays.fill(transOnes, 1.0);
		j = 0;

		// first set the basic ranges of variables
		for (int i = 1; i <= numVars; i++) {
			// set int constraint on variables
			lp.setInt(i, true);
			// set bound on variables from 0 to 1
			lp.setBounds(i, 0.0, 1.0);
		}

		// add row for each set (pairwise decision): x1 + x2 + x3 + x4 + x5 + x6
		// = 1
		for (double[] set : sets) {
			// set variable names
			for (int i = 0; i < numClasses; i++)
				column[i] = rows * numClasses + i + 1;

			System.out.println("New row");
			System.out.println(Arrays.toString(ones));
			System.out.println(Arrays.toString(column));
			// add the row
			lp.addConstraintex(numClasses, ones, column, LpSolve.EQ, 1);
			rows++;
		}

		// add row for each transitive rule: x1 + x2 - 1 <= x3
		int transColumn[] = { 1, 1, -1 };
		for (int[] trans : transitives) {
			// set variable names
			for (int i = 0; i < trans.length; i++)
				transColumn[i] = trans[i];

			// somehow this array can get corrupted, so reset every time
			Arrays.fill(transOnes, 1.0);
			transOnes[2] = -1;

			System.out.println("New transitive constraint");
			System.out.println(Arrays.toString(transOnes));
			System.out.println(Arrays.toString(transColumn));

			// add the row
			// lp.addConstraintex(3, transOnes, transColumn, LpSolve.EQ, 1);
			lp.addConstraintex(3, transOnes, transColumn, LpSolve.LE, 1);
			rows++;
		}

		// rowmode should be turned off again when done building the model
		lp.setAddRowmode(false);

		// --------------------------------------------------
		// OBJECTIVE FUNCTION
		// --------------------------------------------------
		double[] row = new double[numVars];
		column = new int[numVars];
		j = 0;
		rows = 0;

		// build up the long sum of prob*var
		for (double[] set : sets) {
			for (int i = 0; i < numClasses; i++) {
				int index = rows * numClasses + i;
				// set variable names
				column[index] = index + 1;
				row[index] = set[i];
			}
			rows++;
		}

		System.out.println("Objective");
		System.out.println(Arrays.toString(row));
		System.out.println(Arrays.toString(column));
		/* set the objective in lpsolve */
		lp.setObjFnex(numVars, row, column);

		/* set the object direction to maximize */
		lp.setMaxim();
	}

	/**
	 * @return An array of values for each variable
	 */
	public double[] solveSystem() throws LpSolveException {
		double[] row = null;
		int numVars = sets.size() * numClasses;

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
			for (int j = 0; j < numVars; j++) {
				if (row[j] == 1.0)
					System.out.println(((j / 6) + 1) + ": " + ((j % 6) + 1));
				// System.out.println(lp.getColName(j + 1) + ": " + row[j]);
			}
		} else {
			System.out.println("Couldn't get an answer");
		}

		/* clean up such that all used memory by lpsolve is freed */
		if (lp.getLp() != 0)
			lp.deleteLp();

		return row;
	}

	public void clear() {
		if (lp.getLp() != 0)
			lp.deleteLp();
	}

	public static void main(String[] args) {
		try {
			// new LinearProgramming().execute();
			LinearProgramming lp = new LinearProgramming(6);
			/*
			 * double p[] = {.1, .3, .2, .05, .3, .05}; lp.addProbs(p); p = new double[]
			 * {.25, .15, .1, .05, .05, .4}; lp.addProbs(p); p = new double[] {.21, .20,
			 * .19, .20, .1, .1}; lp.addProbs(p); /* double p[] = {-2.3, -1.2, -1.6, -3,
			 * -1.2, -3}; lp.addProbs(p); p = new double[] {-1.39, -1.9, -2.3, -3, -3,
			 * -.91}; lp.addProbs(p); p = new double[] {-1.56, -1.61, -1.66, -1.61, -2.3,
			 * -2.3}; lp.addProbs(p); lp.addTransitiveConstraint(5, 12, 14);
			 */

			double p[] = { -34.20010312954961, -44.687800075167246, -37.20647984488537, -41.33895209484322,
					-41.09090896250227, -32.93185255642826 };
			lp.addProbs(p);
			p = new double[] { -26.03321503748283, -36.905296014907094, -29.72567687658605, -35.87306507332955,
					-38.255697736951845, -24.342840881230067 };
			lp.addProbs(p);
			p = new double[] { -28.474960003447624, -35.99479380298384, -29.741429672650536, -47.88323280798873,
					-47.52203032273881, -20.973125132628105 };
			lp.addProbs(p);
			p = new double[] { -29.86502182740245, -41.055490972541705, -33.831240990697, -43.61306926646025,
					-42.9367356530006, -29.492987797579566 };
			lp.addProbs(p);
			p = new double[] { -28.474960003447624, -35.99479380298384, -29.741429672650536, -47.88323280798873,
					-47.52203032273881, -20.973125132628105 };
			lp.addProbs(p);
			p = new double[] { -30.35619268470929, -42.85740281323377, -34.752482138481746, -40.39366619299165,
					-41.93186765744574, -34.040079702406935 };
			lp.addProbs(p);
			p = new double[] { -34.969615911935215, -44.51298649473317, -43.163276505956425, -45.185821005091576,
					-43.43280086118347, -35.8922851901277 };
			lp.addProbs(p);
			p = new double[] { -32.29989221825368, -42.46793804647205, -34.96559785908806, -40.82753077562152,
					-40.9243964734876, -35.03888478541067 };
			lp.addProbs(p);

			lp.addTransitiveConstraint(6, 43, 31);
			lp.addTransitiveConstraint(48, 1, 31);
			lp.addTransitiveConstraint(45, 31, 1);
			lp.addTransitiveConstraint(45, 32, 1);
			lp.addTransitiveConstraint(46, 33, 3);
			lp.addTransitiveConstraint(47, 31, 1);
			lp.addTransitiveConstraint(47, 32, 2);
			lp.addTransitiveConstraint(47, 33, 3);
			lp.addTransitiveConstraint(48, 31, 1);
			lp.addTransitiveConstraint(48, 32, 2);
			lp.addTransitiveConstraint(48, 33, 3);
			lp.addTransitiveConstraint(48, 34, 4);
			lp.addTransitiveConstraint(48, 35, 5);
			lp.addTransitiveConstraint(48, 36, 6);
			lp.addTransitiveConstraint(31, 45, 1);
			lp.addTransitiveConstraint(31, 47, 1);
			lp.addTransitiveConstraint(31, 48, 1);
			lp.addTransitiveConstraint(32, 45, 1);
			lp.addTransitiveConstraint(32, 47, 2);
			lp.addTransitiveConstraint(32, 48, 2);
			lp.addTransitiveConstraint(33, 46, 3);
			lp.addTransitiveConstraint(33, 47, 3);
			lp.addTransitiveConstraint(33, 48, 3);
			lp.addTransitiveConstraint(34, 48, 4);
			lp.addTransitiveConstraint(35, 48, 5);

			lp.buildSystem();
			lp.solveSystem();
		} catch (LpSolveException e) {
			e.printStackTrace();
		}
	}
}
