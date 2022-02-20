package constants;

import graph.PGraph;

public class ConstantsTrans {

	public static PGraph.TransitiveMethod transMethod = PGraph.TransitiveMethod.HTLFRG;
	public static boolean checkFrgVio = false;
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
	public static String graphPostFix = "_" + transMethod +"_conf_final_mr100_.02"+".txt";
	static {
		if (!checkFrgVio && transMethod == PGraph.TransitiveMethod.HTLFRG) {
			graphPostFix = "_HTL_conf_final_mr100_.02"+".txt";
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
		
		//used to replicate results on NS BInc
		ConstantsGraphs.removeEventEventModifers = false;//false for both NS, true for NC
		ConstantsGraphs.removeNegs = false;//false for NS and NC
		ConstantsGraphs.removeStopPreds = false;///false for both NS, true for NC
		ConstantsGraphs.edgeThreshold = 0; // 0 for both, I think! (could be .01 for NS)
		
//		ConstantsGraphs.removeEventEventModifers = true;
//		ConstantsGraphs.removeNegs = true;
//		ConstantsGraphs.removeStopPreds = false;
//		ConstantsGraphs.edgeThreshold = 0;
		
		ConstantsGraphs.maxRank = 100;//-1 for NS, 100 for NC.
		
		ConstantsGraphs.sortEdgesConfidenceBased = true;
		if (ConstantsGraphs.sortEdgesConfidenceBased) {
			PGraph.setPredToOcc(ConstantsGraphs.root);
		}
	}
}
