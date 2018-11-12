package constants;

import graph.PGraph;
import graph.PGraph.FeatName;

public class ConstantsGraphs {

	public static boolean removeStopPreds = false;
	public static String suffix = "_sim.txt";
	public static PGraph.FeatName featName = PGraph.FeatName.BINC;
	public static String root = "../../python/gfiles/typedEntGrDir_aida_figer_3_3_f/";// ALL TACL EXPERIMENTS
	// public static String root =
	// "../../python/gfiles/typedEntGrDirC_aida_typed_NSSize_predBased_3_3/";
	// public static String root =
	// "../../python/gfiles/typedEntGrDir_aida_figer_10_10/";
	// public static String root =
	// "../../python/gfiles/typedEntGrDir_aida_figer_10_10/";

	// public static float edgeThreshold = 0.01f;// All the experiments are done by
	// threshold = .01. Never change this!
	// // However, it isn't
	// // really worth it! .05 reduces edges by half, but not worth it

	// public static float edgeThreshold = 0.0215112528889f;//cg pr, pr = .75

	// public static float edgeThreshold = 0.14436184f; //typed binc, pr = .79
	public static float edgeThreshold = 0.01f;// , pr = .82//ALL TACL EXPERIMENTS
	// public static float edgeThreshold = 0.08007261f;//typed binc, pr = .75
	// static String tPropSuffix = "_tprop_test2.txt";
	public static boolean addTargetRels = false;// must be false

}
