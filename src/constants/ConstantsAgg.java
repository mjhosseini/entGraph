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
	public static boolean updatedTyping = true;// a few fixes here and there. flase for TACL experiments. True for NC
												// (NewsCrawl).
	public static boolean figerHierarchy = false;// false for all experiments.
	public static boolean isForeign = false;
	public static boolean addTimeStampToFeats = false;
	public static final boolean normalizePredicate = true;// if rawExtraction, wouldn't matter.
	public static final boolean keepWillTense = false;// Must be false for normal entailment graphs
	public static final boolean backupToStanNER = false; // false for TACL experiments. True for NC.
	public static final boolean onlyBinc = true;// vs all similarity measures. use to save memory and storage!

	public static boolean removeEventEventModifiers = false; // false for TACL experiments. True for NC experiments.
	public static boolean removeStopPreds = false; // false for TACL experiments. True for NC experiments.
	public static boolean removePronouns = false;//
	public static boolean cutoffBasedonNSGraphs = false;// use NSpike-based cutoffs. false for TACL experiments. True
														// for NC experiments

	// cutoffs
	public static int minArgPairForPred = 3;
	public static int minPredForArgPair = 3; // min num of unique predicates for argpair.
	// when NSbased num argPairs, we allownumArgPairsNSBasedAlwaysAllowed argPairs for each pred, even if not in
	// NS.
	public static int numArgPairsNSBasedAlwaysAllowed = 0;// Always 0. Parameter not used.
	public static int numTopTypePairs = 0;// the big types, used in NSbased sizes. 0 for TACL experiments. 20 for NC.
	public static int maxPredsTotal = -1; // Always -1. Parameter not used.

	public static final int minPredForArg = -1;// min num of unique predicates. Always -1. Parameter not used.
	public static boolean removeGGFromTopPairs = false;// false for TACL. True for NC. whether we should remove triples
														// with two general
														// entities
														// from top pairs
	public static boolean removeGGFromThings = false;// false for TACL. True for NC. whether we should remove triples
														// with two general entities
	// from any thing graph

	public static final int numThreads = 20;

	// embedding parameters
	public static boolean embBasedScores = false;// use sigmoid(transE score) instead of counts
	public static double sigmoidLocParameter = 10; // Parameter not used.
	public static double sigmoidScaleParameter = .25; // Parameter not used.
	public static int dsPredNumThreads = 15; // Parameter not used.
	public static EntailGraphFactoryAggregator.ProbModel probModel = EntailGraphFactoryAggregator.ProbModel.PEL; // Parameter
																													// not
																													// used.
	public static EntailGraphFactoryAggregator.LinkPredModel linkPredModel = EntailGraphFactoryAggregator.LinkPredModel.ConvE; // Parameter
																																// not
																																// used.

	public static String relAddress = "news_gen.json";
	public static String NERAddress = "data/stan_NER/news_genC_stanNER.json"; // Only used if backupToStanNER. Not used for TACL experiments. Used for NC experiments. 

	public static String simsFolder = "typedEntGrDir_" + minArgPairForPred + "_" + minPredForArgPair;

	public static String foreinTypesAddress = "data/german_types.txt";// only used if isForeign=True.

	public static boolean computeProbELSims = false; // Parameter not used.
	public static boolean linkPredBasedRandWalk = false; // Parameter not used.

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
