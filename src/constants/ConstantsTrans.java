package constants;

import graph.PGraph;

public class ConstantsTrans {

	public static boolean checkFrgVio = true;
	public static PGraph.TransitiveMethod transMethod = PGraph.TransitiveMethod.HTLFRG;
	public static boolean fullTNF = false;// Only relevant for TNF. true: Berant's TNF, false: Berant's HTL(-FRG)

	public static int specILPMaxClusterAllowed = 100;// for SpectralILP
	public static int specILPMaxClusterSizeAllowed = 100;// for SpectralILPWithin (just an approximation)

	public static boolean shouldReplaceOutputs = true;// if true, will solve everything, otherwise ignores the files it
	// already has!
	public static boolean shouldWrite = true;
	public static boolean formBinaryGraph = true;
	public static boolean transitive = true;// otherwise, plain graph!
	public static boolean confAvg = false;//should be true when crossGraph is on!
	public static int numTransThreads = 16;
	// public static FeatName featName = FeatName.Iter;
	// public static String suffix = "_tprop_lm1_.01_reg_1.5_.3.txt";
//	public static String graphPostFix = "_" + transMethod + "_conf_noconfE_test"+".txt";
	public static String graphPostFix = "_" + transMethod + "_conf_nodisc_san"+".txt";
//	public static double discountNegScoresHTL = 1;
	static {
		if (!checkFrgVio && transMethod == PGraph.TransitiveMethod.HTLFRG) {
			graphPostFix = "HTL_conf_nodisc_san.txt";
		}

	}

	public static void setPGraphParams() {
		ConstantsGraphs.removeEventEventModifers = true;
		ConstantsGraphs.removeNegs = true;
		ConstantsGraphs.sortEdgesConfidenceBased = true;
		ConstantsGraphs.rankDiscount = false;
		if (ConstantsGraphs.sortEdgesConfidenceBased) {
			PGraph.setPredToOcc(ConstantsGraphs.root);
		}
	}

	public static boolean writeDebug = false;
}
