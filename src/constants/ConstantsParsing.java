package constants;

public class ConstantsParsing {

	public static int maxLinesToRun = 100000;// This is because of the memory leak in easyCCG, don't change it!
	public static int maxMBallowd = 14000;

	public static int numThreads = 20;
	// final int maxMBallowd = 140;
	public static boolean convToEntityLinked = false;// Must be always false, we do linking separately!
	public static String[] accepteds = new String[] { "GE", "EG", "EE", "GG" };//{ "GE", "EG", "EE" }: This is what I've been always using
	public static final boolean lemmatizePred = true;// eaten.might.1 => eat.might.1// Must be true for normal entailment graphs
	public static boolean parseQuestions = false;// Always set if to false for processing the main corpus
	public static boolean writeUnaryRels = false;
	public static boolean writeDebugString = false;
	public static boolean writeSemParse = true;
	public static boolean filterUntensed = false;// for deduplicating when a tense is added, eg [receiving; receiving.will]->[receiving.will]
	public static boolean tenseParseTest = false;// for testing parses on tense pair lists. turn off writeSemParse
	
	public static boolean snli = true;// a few good hacks for the snli ds
	public static int nbestParses = 1;

}
