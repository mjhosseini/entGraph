package constants;

import java.io.IOException;

import entailment.randWalk.RandWalkMatrix;
import entailment.vector.EntailGraphFactoryAggregator;

public class ConstantsRWalk {

//	public static boolean avgWithCos = false;
	public static int convEArgPairNeighs = 0;
//	public static String triples2scoresPath = "embs/probs_all_ConvE_NS_20_20_unique_ex.txt";
//	public static String embsPath = "embs/rels2emb_ConvE_NS_unt_20_20_1000.txt";
//	public static String allTriplesPath = "embs/triples_20_20.txt";
	
//	public static String triples2scoresPath = "embs/probs_all_ConvE_NS_10_10_unique_ex.txt";
	public static String triples2scoresPath = "embs/probs_all_conve_lstm_10_10_100.txt";
	public static String embsPath = "embs/rels2emb_ConvE_NS_unt_10_10_1000.txt";
	public static String allTriplesPath = "embs/triples_10_10.txt";
	
	public static double threshold = 1e-6;
	public static double thresholdMul = -1;//for 2nd-order statistic, etc
	public static int NThr = 1000000000;
	public static int L = 1; 

	public static void setAggConstants() {
		ConstantsAgg.linkPredBasedRandWalk = true;
		ConstantsAgg.probModel = EntailGraphFactoryAggregator.ProbModel.RandWalk;
		ConstantsAgg.linkPredModel = EntailGraphFactoryAggregator.LinkPredModel.ConvE;
//		ConstantsAgg.simsFolder = "typedEntGrDir_untyped_20_20_convE_ol_NS_rwm_ap"
//				+ ConstantsRWalk.convEArgPairNeighs;//+"thr_"+thresholdMul;
		ConstantsAgg.simsFolder = "typedEntGrDir_untyped_10_10_convE_ol_NS_rwm_ap"
				+ ConstantsRWalk.convEArgPairNeighs;//+"thr_"+thresholdMul;
		ConstantsAgg.dsPredNumThreads = 15;
	}

}
