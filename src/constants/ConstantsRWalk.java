package constants;

import entailment.vector.EntailGraphFactoryAggregator;

//Not used in the papers.
public class ConstantsRWalk {

	public static int convEArgPairNeighs = 0;	
	public static String triples2scoresPath = "embs/probs_all_ConvE_NS_10_10_unique_ex.txt";
	public static String embsPath = "embs/rels2emb_ConvE_NS_unt_10_10_1000.txt";
	public static String allTriplesPath = "embs/triples_10_10.txt";
	
	public static double threshold = 1e-6;
	public static double thresholdMul = -1;//for 2nd-order statistic, etc
	public static int NThr = 1000000000;
	public static int L = 2; 

	public static void setAggConstants() {
		ConstantsAgg.linkPredBasedRandWalk = true;
		ConstantsAgg.probModel = EntailGraphFactoryAggregator.ProbModel.RandWalk;
		ConstantsAgg.linkPredModel = EntailGraphFactoryAggregator.LinkPredModel.ConvE;
		ConstantsAgg.simsFolder = "typedEntGrDir_untyped_10_10_convE_lstm_tok_NS_rwm_ap_dg_c0"
				+ ConstantsRWalk.convEArgPairNeighs;//+"thr_"+thresholdMul;
		ConstantsAgg.dsPredNumThreads = 15;
	}

}
