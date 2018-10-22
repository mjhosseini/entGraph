package constants;

public class ConstantsParsing {

	public static int maxLinesToRun = 100000;// This is because of the memory leak in easyCCG
	public static int maxMBallowd = 14000;

	public static int numThreads = 20;
	// final int maxMBallowd = 140;
	public static boolean convToEntityLinked = false;// Must be always false, we do linking separately!
	public static String[] accepteds = new String[] { "GE", "EG", "EE", "GG" };//{ "GE", "EG", "EE" }: This is what I've been always using
	public static final boolean lemmatizePred = true;// eaten.might.1 => eat.might.1
	public static boolean parseQuestions = false;// Always set if to false for processing the main corpus
	public static boolean writeUnaryRels = true;
	public static boolean writeDebugString = false;
	public static boolean snli = true;// a few good hacks for snli ds
	public static int nbestParses = 1;

}
