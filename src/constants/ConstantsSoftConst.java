package constants;

public class ConstantsSoftConst {

	public static int numThreads = 60;
	public static int numIters = 4;
	// public static double lmbda = .001;// lmbda for L1 regularization
	public static double lmbda = .01;// lmbda for L1 regularization //.01 for ALL TACL experiments
	public static double lmbda2 = 0;
	// public static double lmbda3 = 0;
	public static double tau = .3;
	
	public static double smoothParam = 5.0;//doesn't matter, deprecated
	// static final String tPropSuffix = "_tProp_i4_predBased_areg_trans_1.0.txt";
	// static final String tPropSuffix = "_tProp_trans_0_.3_obj2_n.txt";
	public static String tPropSuffix = "_binc_lm1_.001_reg_0_.3.txt";
	public static boolean predBasedPropagation = true;//must be true
	public static boolean sizeBasedPropagation = false;//must be felse
	public static boolean factorized = false;//must be false
	public static boolean obj1 = false;// obj1: max(w-tau), false: 1(w>tau)w //must be false
	public static String compatiblesPath = "../../python/gfiles/ent/compatibles_all.txt";
	

}
