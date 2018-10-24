package constants;

import graph.PGraph;

public class ConstantsTrans {

	public static boolean checkFrgVio = false;
	// public static boolean origBerantFRG = true;
	public static boolean fullTNF = false;// Must be true. false means HTL(-FRG)
	public static PGraph.TransitiveMethod transMethod = PGraph.TransitiveMethod.HTLFRG;
	public static int specILPMaxClusterAllowed = 100;// for SpectralILP
	public static int specILPMaxClusterSizeAllowed = 100;// for SpectralILPWithin (just an approximation)
	public static boolean shouldReplaceOutputs = true;// if true, will solve everything, otherwise ignores the files it
	// already has!
	public static boolean shouldWrite = true;
	public static boolean formBinaryGraph = false;
	public static boolean transitive = false;
	public static int numTransThreads = 16;
	//	public static FeatName featName = FeatName.Iter;
	//	public static String suffix = "_tprop_lm1_.01_reg_1.5_.3.txt";
		public static String graphPostFix = "_" + transMethod + "NOFRG_san.txt";

}
