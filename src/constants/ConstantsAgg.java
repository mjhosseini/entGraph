package constants;

import entailment.vector.EntailGraphFactoryAggregator;

public class ConstantsAgg {

	// ####################################################################################################
	// Extract sims
	// All parameters:

	public static boolean onlyDSPreds = false;
	public static boolean rawExtractions = false;// gbooks original (or gbooks in general?)
	public static boolean GBooksCCG = false;
	public static boolean useTimeEx = false;
	public static boolean isCCG = true;
	public static boolean isTyped = true;
	public static boolean updatedTyping = true;// a few fixes here and there. flase for TACL experiments!
	public static boolean figerHierarchy = false;// for TACL experiments, we had it false
	public static boolean isForeign = false;
	public static boolean addTimeStampToFeats = false;
	public static final boolean normalizePredicate = true;// if rawExtraction, wouldn't matter.
	public static final boolean keepWillTense = false;// Must be false for normal entailment graphs
	public static final boolean backupToStanNER = true;
	public static final boolean onlyBinc = true;// vs all similarity measures. use to save memory and storage!

	public static boolean removeEventEventModifiers = true;
	public static boolean removeStopPreds = true;
	public static boolean removePronouns = false;// TODO: You must move this to parsing
	public static boolean cutoffBasedonNSGraphs = true;// use NSpike-based cutoffs

	// cutoffs
	public static int minArgPairForPred = 3;// 100;
	public static int minPredForArgPair = 3;// 20;// min num of unique predicates for argpair
	// when NS based num aps, we allow x aps for each pred, even if not in NS
	public static int numArgPairsNSBasedAlwaysAllowed = 0;// default: 10
	public static int numTopTypePairs = 20;// the big types, used in NSbased sizes (usually 20)
	public static int maxPredsTotal = -1;// 35000;

	public static final int minPredForArg = -1;// min num of unique predicates for
	public static boolean removeGGFromTopPairs = true;// Good for NC. whether we should remove triples with two general
														// entities
														// from top pairs
	public static boolean removeGGFromThings = true;// Good for NC. whether we should remove triples with two general
													// entities
	// from any thing graph

	public static final int numThreads = 20;

	// embedding parameters
	public static boolean embBasedScores = false;// use sigmoid(transE score) instead of counts
	public static double sigmoidLocParameter = 10;
	public static double sigmoidScaleParameter = .25;
	public static int dsPredNumThreads = 15;
	public static EntailGraphFactoryAggregator.ProbModel probModel = EntailGraphFactoryAggregator.ProbModel.PEL;
	public static EntailGraphFactoryAggregator.LinkPredModel linkPredModel = EntailGraphFactoryAggregator.LinkPredModel.ConvE;

	// public static String relAddress = "news_gen12.json";
	// public static String relAddress = "news_genC_GG.json";
	public static String relAddress = "news_genC_GG_20per.json";
	public static String NERAddress = "data/stan_NER/news_genC_stanNER.json";

	// public static String simsFolder = "typedEntGrDir_aida_gen12_UT_hier_back" +
	// minArgPairForPred + "_"
	// + minPredForArgPair;

	// public static String simsFolder = "typedEntGrDirC_NSBased_f20_thing60_60_" +
	// minArgPairForPred + "_"
	// + minPredForArgPair + "_test";
	// public static String simsFolder = "typedEntGrDirC_NSBased_" +
	// minArgPairForPred + "_" + minPredForArgPair
	// + "thing_80_location_thing_times4_f20_GG_UT_back_noGGThing_week";
	// public static String simsFolder = "typedEntGrDirC_NSBased_" +
	// minArgPairForPred + "_" + minPredForArgPair
	// + "f20_GG_noGGThing_year";
	// public static String simsFolder = "typedEntGrDirC_NSBased_" +
	// minArgPairForPred + "_" + minPredForArgPair
	// + "thloc_t6_noGG_f20Thing";

	public static String simsFolder = "typedEntGrDirC_20per_NSBased_" + minArgPairForPred + "_" + minPredForArgPair
			+ "thloc_t6_noGG_f20Thing";

	// public static String simsFolder = "typedEntGrDirC_NSBased_" +
	// minArgPairForPred + "_" + minPredForArgPair
	// + "thloc_t2_noGG_f20Thing_hier";

	// public static String simsFolder = "typedEntGrDirC_CN_NBEE_NSBased_" +
	// minArgPairForPred + "_" + minPredForArgPair
	// + "_f20_GG_noGGThing_week";

	// public static String simsFolder = "typedEntGrDirC_CN_NBEE_" +
	// minArgPairForPred + "_" + minPredForArgPair
	// + "_f20_GG_noGGThing_week";

	// public static String simsFolder = "typedEntGrDirC_CN_NBEE_NSBased_" +
	// minArgPairForPred + "_" + minPredForArgPair
	// + "_f20_GG_week";

	public static String foreinTypesAddress = "data/german_types.txt";// only important if isForeign=True

	// public static String simsFolder = "typedEntGrDir_datestamp_" +
	// minArgPairForPred + "_" + minPredForArgPair;

	public static boolean computeProbELSims = false;
	public static boolean linkPredBasedRandWalk = false;

	public static String NSSizesAddress;
	public static String NSPredSizesAddress;

	static {

		if (ConstantsAgg.addTimeStampToFeats) {
			NSSizesAddress = "data/NS_sizes_week_3_3.txt";
			NSPredSizesAddress = "data/NS_pred_sizes_week_3_3.txt";
		} else if (ConstantsAgg.figerHierarchy) {
			NSSizesAddress = "data/NS_sizes_hier_3_2.txt";
			NSPredSizesAddress = "data/NS_pred_sizes_hier_3_2.txt";
		} else {
			NSSizesAddress = "data/NS_sizes.txt";
			NSPredSizesAddress = "data/NS_pred_sizes.txt";
		}
	}
}
