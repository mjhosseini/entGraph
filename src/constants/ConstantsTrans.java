package constants;

import graph.PGraph;

public class ConstantsTrans {

	public static boolean checkFrgVio = false;
	public static PGraph.TransitiveMethod transMethod = PGraph.TransitiveMethod.HTLFRG;
	public static boolean fullTNF = false;// Only relevant for TNF. true: Berant's TNF, false: Berant's HTL(-FRG)

	public static int specILPMaxClusterAllowed = 100;// for SpectralILP
	public static int specILPMaxClusterSizeAllowed = 100;// for SpectralILPWithin (just an approximation)

	public static boolean shouldReplaceOutputs = false;// if true, will solve everything, otherwise ignores the files it
	// already has!
	public static boolean shouldWrite = true;
	public static boolean formBinaryGraph = true;
	public static boolean transitive = true;// otherwise, plain graph!
	public static int numTransThreads = 16;
	// public static FeatName featName = FeatName.Iter;
	// public static String suffix = "_tprop_lm1_.01_reg_1.5_.3.txt";
	public static String graphPostFix = "_" + transMethod + ".txt";
	
	static {
		if (!checkFrgVio && transMethod== PGraph.TransitiveMethod.HTLFRG) {
			graphPostFix = "_HTL.txt";
			
		}
	}

}
