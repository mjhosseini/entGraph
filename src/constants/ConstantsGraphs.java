package constants;

import graph.PGraph;

public class ConstantsGraphs {

	// public static String root =
	// "../../python/gfiles/typedEntGrDirC_NSBased_10_3thing_80_location_thing_times4_f20_GG_UT_back_noGGThing_hier/";
	// public static String root =
	// "../../python/gfiles/typedEntGrDirC_80per_NSBased_3_3thloc_t6_noGG_f20Thing/";
	// public static String root =
	// "../../python/gfiles/entgraphs_contextual_5e-4_1e-2_split_epair_fill_0_bsz64_onlytrain/";
	// public static String root =
	// "../../python/gfiles/typedEntGrDir_aida_figer_3_3_f/";
	// public static String root =
	// "../../python/gfiles/typedEntGrDirC_NSBased_3_3_thloc_t6_noGG_f20Thing/";
	public static String root = "../../python/gfiles/entgraphs_newsC_contextual_5e-4_1e-2_bsz_64_d_250_4gpu_nr_.25_fill_0_bsz64/";
	// public static String root =
	// "../../python/gfiles/typedEntGrDirC_20per_NSBased_3_3thloc_t6_noGG_f20Thing/";
	// public static String root =
	// "../../python/gfiles/typedEntGrDir_NC_AUG_MC_ap50_conve/";
	public static String suffix = "_sim.txt";
	// public static String suffix = "_tprop_lm1_.01_reg_1.5_.3.txt";
	// public static String suffix = "_tPropC_i4_1.5_.3_0_mr100_thr0.txt";
	// public static String suffix = "_binc_lm1_.001_reg_1.5_1_.2_.01_i20.txt";
	// public static PGraph.FeatName featName = PGraph.FeatName.BINC;
	// public static PGraph.FeatName featName = PGraph.FeatName.Iter;
	public static PGraph.FeatName featName = PGraph.FeatName.contextualized;
	// public static PGraph.FeatName featName = PGraph.FeatName.rw;
	// public static String root =
	// "../../python/gfiles/typedEntGrDir_aida_figer_3_3_f/";// ALL TACL EXPERIMENTS
	// public static String root =
	// "../../python/gfiles/typedEntGrDir_3_3_f_convE_ptyped_train_2Ts_70_rw_c_ap50_L1/";

	// public static String root =
	// "../../python/gfiles/typedEntGrDir_3_3_f_convE_ptyped_train_100_rw_c_ap0_L1/";
	// public static String root =
	// "../../python/gfiles/typedEntGrDirC_aida_typed_NSSize_predBased_3_3/";
	// public static String root =
	// "../../python/gfiles/typedEntGrDir_aida_figer_10_10/";
	// public static String root =
	// "../../python/gfiles/typedEntGrDir_aida_figer_10_10/";

	public static double edgeThreshold = 0;
	// public static double edgeThreshold = 0;
	// public static float edgeThreshold = 0.01f;// ALL TACL EXPERIMENTS are done by
	// threshold = .01. Never change this!
	// // However, it isn't
	// // really worth it! .05 reduces edges by half, but not worth it

	// public static float edgeThreshold = 0.0215112528889f;//cg pr, pr = .75

	// public static float edgeThreshold = 0.14436184f; //typed binc, pr = .79
	// public static float edgeThreshold = 0.08007261f;//typed binc, pr = .75
	// static String tPropSuffix = "_tprop_test2.txt";
	public static boolean addTargetRels = false;// must be false
	public static boolean sortEdgesConfidenceBased = false;// will be used only for HTL or self training
	public static boolean removeEventEventModifers = true;// false for TACL experiments, True for NC. might change in
															// transitive
	public static boolean removeStopPreds = true; // false for TACL experiments, True for NC.
	public static boolean removeNegs = false;// might change in transitive
	public static boolean rankFeats = false; // was false for TACL experiments and NC. If true, it uses 1/rank as the
											// feat value.
	public static int maxRank = 120; // -1 means no constraints // was -1 for TACL experiments. 100 for NC
}
