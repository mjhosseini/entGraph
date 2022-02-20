package constants;

import graph.PGraph;

public class ConstantsGraphs {

	public static String root = "../../python/gfiles/typedEntGrDir_3_3/";
	public static String suffix = "_sim.txt";
	 public static PGraph.FeatName featName = PGraph.FeatName.BINC; // Set to rw for ACL-2019 experiments and contextualized for EMNLP-findings 2021 experiments.
	public static float edgeThreshold = 0.01f;// TACL experiments were done by 0.01f. Other experiments (ACL2019 and EMNLP-findings 2021) were done by 0.0.
	public static boolean addTargetRels = false;// must be false
	public static boolean sortEdgesConfidenceBased = false;// will be used only for HTL or self training.
	public static boolean removeEventEventModifers = false;// false for TACL experiments, True for NC. might change in
															// transitive
	public static boolean removeStopPreds = false; // false for TACL experiments, True for NC.
	public static boolean removeNegs = false;// might change in transitive.
	public static boolean rankFeats = false; // was false for TACL experiments and NC, and ACL2019 and EMNLP-findings 2021. If true, it uses 1/rank as the feat value.
	public static int maxRank = -1; // -1 means no constraints // was -1 for TACL experiments. 100 or 120 for NC.
}
