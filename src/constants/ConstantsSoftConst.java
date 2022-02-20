package constants;

public class ConstantsSoftConst {

	public static int numThreads = 60;//
	public static int numIters = 4;// 4 for TACL experiments
	public static double lmbda = 0.01; //.01 for ALL TACL experiments. 0 for the follow-up papers. 
	public static double lmbda2 = 1.5;// 1.5 for all TACL experiments and the follow-up papers.
	public static double epsilon = .3;// .3 for all TACL experiments and the follow-up papers.
	public static double epsilonTrans = 1;// was .1. Must be 1 so it will be cancelled. 1 for all experiments.
	public static double lmbda3 = 0;// Must be 0 (related to soft transitivity constraints).

	public static double smoothParam = 5.0;// doesn't matter, deprecated
	public static String tPropSuffix = "_global_i4_1.5_.3_0.01.txt";
	public static boolean forceSelfEdgeOne = true;// Must be true for normal experiments
	public static boolean predBasedPropagation = true;// must be true
	public static boolean sizeBasedPropagation = false;// must be false
	public static boolean factorized = false;// must be false
	public static boolean obj1 = false;// obj1: max(w-tau), false: 1(w>tau)w //must be false
	public static boolean applyMaxRankInIters = true;// false for TACL, ACL19 and EMNLP-findings2021 experiments. true for NC experiments.
	public static int minEdgesToApplyMaxRankInIters = -1;// Always -1.
}
