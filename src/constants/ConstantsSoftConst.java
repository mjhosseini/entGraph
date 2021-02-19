package constants;

public class ConstantsSoftConst {

	public static int numThreads = 60;// was 60 for TACL experiments
	public static int numIters = 4;// 4 for TACL experiments
	// public static double lmbda = .001;// lmbda for L1 regularization
	public static double lmbda = 0;// lmbda for L1 regularization //.01 for ALL TACL experiments
	public static double lmbda2 = 1.5;// 1.5 for all TACL experiments
	public static double epsilon = .3;// .3 for all TACL experiments
	public static double epsilonTrans = 1;// was .1. Must be 1 so it will be cancelled
	public static double lmbda3 = 0;// was .01. Must be 0

	public static double smoothParam = 5.0;// doesn't matter, deprecated
	// static final String tPropSuffix = "_tProp_i4_predBased_areg_trans_1.0.txt";
	// static final String tPropSuffix = "_tProp_trans_0_.3_obj2_n.txt";
	// public static String tPropSuffix = "_rw_lm1_0_reg_inf_1_thr_0_selfE.txt";
	// public static String tPropSuffix = "_tPropC_i4_1_.3_0_mr100_thr0.txt"; // NC
	// binc experiments
	public static String tPropSuffix = "_tPropC_i4_1.5_.3_0_thr0_mr120.txt";
	// public static String tPropSuffix = "_tPropC_i4_1.5_.3_0_thr0.00005.txt";
	// public static String tPropSuffix = "_tPropC_i4_.01_1_0.00005_thr0.00005.txt";
	public static boolean forceSelfEdgeOne = true;// Must be true for normal experiments
	public static boolean predBasedPropagation = true;// must be true
	public static boolean sizeBasedPropagation = false;// must be false
	public static boolean factorized = false;// must be false
	public static boolean obj1 = false;// obj1: max(w-tau), false: 1(w>tau)w //must be false
	public static boolean applyMaxRankInIters = true;// false for TACL experiments, true for MC experiments.
	public static int minEdgesToApplyMaxRankInIters = -1; // if allEdgesProp < 300M => act as if applyMaxRankInIters =
															// false. Always false
	// public static String compatiblesPath0 =
	// "../../python/gfiles/ent/compatibles_all.txt";
}
