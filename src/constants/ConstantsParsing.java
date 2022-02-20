package constants;

public class ConstantsParsing {

	public static int maxLinesToRun = 100000;// This is because of the memory leak in easyCCG, don't change it!
	public static int maxMBallowd = 14000;

	public static int numThreads = 20;
	// final int maxMBallowd = 140;
	public static boolean convToEntityLinked = false;// Must be always false, we do linking separately!
	public static String[] accepteds = new String[] { "GE", "EG", "EE"};//{ "GE", "EG", "EE" }: This is what I've been always using
	public static boolean onlyNounOrNE = false;//False for TACL experiments and LevyProcessing, true for NC. In order to remove pronouns, etc in case of GG. 
	public static boolean lemmatizePred = true;// eaten.might.1 => eat.might.1// Must be true for normal entailment graphs
	public static boolean normalizePredicate = true;// (eat.2, eat.might.1) => (eat.1,eat.2). was false for TACL experiments. It shouldn't change the results, though, since
	//we used to do this in Aggregator: ConstantsAgg.normalizePredicate. Make it true for contextualized method.
	public static boolean removebasicEvnetifEEModifer = false; //was false for TACL experiments, true for NC.
	public static boolean parseQuestions = false;// Always set it to false for processing the main corpus
	public static boolean writeUnaryRels = false;
	public static boolean splitBinary2Unary = false;// if false, the unaries will be the ones that are indeed one arg, e.g., John walked
	public static boolean writeDebugString = false;//should be false when running the experiments
	public static boolean writeSemParse = false;
	public static boolean filterUntensed = false;// for deduplicating when a tense is added, eg [receiving; receiving.will]->[receiving.will]
	public static boolean tenseParseTest = false;// for testing parses on tense pair lists.
	
	public static boolean snli = false;//false for TACL experiments, true for NC. a few good hacks for the snli ds
	public static boolean writeTokenizationInfo = true; // false for TACL experiments. true for the parsing used in the EMNLP-findings 2021 paper.
	public static int nbestParses = 1;
	public static boolean openIE = false;//openIE. Not implemented
}
