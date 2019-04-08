package constants;

import graph.PGraph;

public class ConstantsTrans {

	public static PGraph.TransitiveMethod transMethod = PGraph.TransitiveMethod.HTLFRG;
	public static boolean checkFrgVio = true;
	public static boolean fullTNF = false;// Only relevant for TNF. true: Berant's TNF, false: Berant's HTL(-FRG)

	public static int specILPMaxClusterAllowed = 100;// for SpectralILP, won't be used for HTL-FRG
	public static int specILPMaxClusterSizeAllowed = 100;// for SpectralILPWithin (just an approximation), won't be used
															// for HTL-FRG

	public static boolean shouldReplaceOutputs = true;// if true, will solve everything, otherwise ignores the files it
	// already has!
	public static boolean shouldWrite = true;
	public static boolean formBinaryGraph = true;
	public static boolean transitive = true;// otherwise, plain graph!
	public static boolean confAvg = false;// must be false
	public static int numTransThreads = 16;
	// public static FeatName featName = FeatName.Iter;
	// public static String suffix = "_tprop_lm1_.01_reg_1.5_.3.txt";
	// public static String graphPostFix = "_" + transMethod +
	// "_conf_noconfE_test"+".txt";
	public static String graphPostFix = "_" + transMethod;
	// +"_conf_nodisc_noconfE_san"+".txt";
	// public static double discountNegScoresHTL = 1;
	static {
		if (!checkFrgVio && transMethod == PGraph.TransitiveMethod.HTLFRG) {
			graphPostFix = "_HTL.txt";//"HTL_conf_nodisc_noconfE_san.txt";
		}

	}
	
	public static boolean writeDebug = false;

	public static void setPGraphParams() {
		
		//for conf, etc
//		ConstantsGraphs.removeEventEventModifers = true;
//		ConstantsGraphs.removeNegs = true;
//		ConstantsGraphs.sortEdgesConfidenceBased = true;
//		ConstantsGraphs.rankDiscount = false;
//		if (ConstantsGraphs.sortEdgesConfidenceBased) {
//			PGraph.setPredToOcc(ConstantsGraphs.root);
//		}
		
		//normal case:
//		ConstantsGraphs.removeEventEventModifers = true;
//		ConstantsGraphs.removeNegs = true;
		ConstantsGraphs.sortEdgesConfidenceBased = true;
		ConstantsGraphs.rankDiscount = false;
		if (ConstantsGraphs.sortEdgesConfidenceBased) {
			PGraph.setPredToOcc(ConstantsGraphs.root);
		}
	}

}
