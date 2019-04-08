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
	public static boolean figerHierarchy = true;// for TACL experiments, we had it false
	public static boolean isForeign = false;
	public static boolean addTimeStampToFeats = false;
	public static final boolean normalizePredicate = true;// if rawExtraction, wouldn't matter.
	public static final boolean keepWillTense = false;// Must be false for normal entailment graphs
	public static final boolean backupToStanNER = true;// You can make this true, but it will take some good time to
														// run!
	public static boolean removeEventEventModifiers = false;
	public static boolean removeStopPreds = false;
	public static boolean cutoffBasedonNSGraphs = false;// use NSpike-based cutoffs
	public static String NSSizesAddress = "NS_sizes.txt";

	// cutoffs
	public static int minArgPairForPred = 3;// 100;
	public static int minPredForArgPair = 2;// 20;// min num of unique predicates for argpair
	// when NS based num aps, we allow x aps for each pred, even if not in NS
	public static int numArgPairsNSBasedAlwaysAllowed = 0;// default: 10
	public static int numTopTypePairs = 20;// the big types, used in NSbased sizes
	public static int maxPredsTotal = -1;// 35000;

	public static final int minPredForArg = -1;// min num of unique predicates for
	public static boolean removeGGFromTopPairs = false;// whether we should remove triples with two general entities
														// from top pairs

	public static final int numThreads = 20;

	// embedding parameters
	public static boolean embBasedScores = false;// use sigmoid(transE score) instead of counts
	public static double sigmoidLocParameter = 10;
	public static double sigmoidScaleParameter = .25;
	public static int dsPredNumThreads = 15;
	public static EntailGraphFactoryAggregator.ProbModel probModel = EntailGraphFactoryAggregator.ProbModel.PEL;
	public static EntailGraphFactoryAggregator.LinkPredModel linkPredModel = EntailGraphFactoryAggregator.LinkPredModel.ConvE;

	public static String relAddress = "news_gen12.json";
	// public static String relAddress = "news_genC.json";
	// public static String relAddress = "news_genC_GG.json";

	public static String simsFolder = "typedEntGrDir_aida_gen12_UT_hier_back" + minArgPairForPred + "_" + minPredForArgPair;

	// public static String simsFolder = "typedEntGrDirC_NSBased_f20_thing60_60_" +
	// minArgPairForPred + "_"
	// + minPredForArgPair + "_test";
//	public static String simsFolder = "typedEntGrDirC_NSBased_" + minArgPairForPred + "_" + minPredForArgPair
//			+ "thing_150_150_f20_GG_UT";

	public static String foreinTypesAddress = "data/german_types.txt";// only important if isForeign=True

	// public static String simsFolder = "typedEntGrDir_datestamp_" +
	// minArgPairForPred + "_" + minPredForArgPair;

	public static boolean computeProbELSims = false;
	public static boolean linkPredBasedRandWalk = false;

}
