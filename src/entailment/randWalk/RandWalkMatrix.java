package entailment.randWalk;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import constants.ConstantsAgg;
import constants.ConstantsRWalk;
import entailment.linkingTyping.SimpleSpot;
import entailment.vector.EntailGraph;
import entailment.vector.EntailGraphFactoryAggregator;
import graph.SimpleScore;

//usage: give it triples2scoresPath, embsPath and allTriplesPath,
//It will compute random walk scores
public class RandWalkMatrix {

	String types = "thing#thing";
	BiMap<String, Integer> pred2idx;
	BiMap<String, Integer> argPair2idx;

	// List<RandWalkNode> nodes;
	int numPreds;
	public static Map<String, double[]> entsToEmbed;
	public static Map<String, double[]> relsToEmbed;
	public static Map<String, Double> predToSumNeighs;
	public static LinkedHashMap<String, List<String>> predToEntPair;
	public static Map<String, Double> entPairToSumNeighs;
	public static LinkedHashMap<String, List<String>> entPairToPred;
	public static Map<String, Double> tripleToScore;
	static Set<String> allTriples;
	MySparseMatrix mat;
	MySparseMatrix matNoHub;
	List<MySparseMatrix> randWalkMats;
	public static int numAllStoredTriples = 0;
	static Set<String> hubs;
	static int numAllSeenEdges = 0;

	public static void loadLinkPredInfo() {
		try {
			hubs = new HashSet<>();
			relsToEmbed = EntailGraphFactoryAggregator.loadEmbeddings(ConstantsRWalk.embsPath);
			allTriples = EntailGraphFactoryAggregator.loadAllTriples(ConstantsRWalk.allTriplesPath);
			tripleToScore = RandWalkMatrix.loadTriplesToScore(ConstantsRWalk.triples2scoresPath);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public RandWalkMatrix(Map<String, Double> tripleToScore, LinkedHashMap<String, List<String>> entPairToPred,
			Map<String, Double> entPairToSumNeighs, LinkedHashMap<String, List<String>> predToEntPair,
			Map<String, Double> predToSumNeighs) {

		this.numPreds = predToEntPair.size();
		pred2idx = HashBiMap.create();
		argPair2idx = HashBiMap.create();
		int idx = 0;

		int N = 0;
		// allocated the indices for nodes
		for (String s : predToEntPair.keySet()) {
			// System.out.println(s);
			pred2idx.put(s, idx++);
			N += predToEntPair.get(s).size();
		}

		int N2 = 0;
		idx = 0;

		for (String s : entPairToPred.keySet()) {
			argPair2idx.put(s, idx++);
			N2 += entPairToPred.get(s).size();
		}

		assert N == N2;
		// N *= 2;

		System.out.println("N is: " + N + " " + predToEntPair.size() + " " + entPairToPred.size());

		// making matrices A (pred by ent-pair) and B (ent-pair by pred)

		// B
		int[] rowIndexB = new int[N];
		double[] xB = new double[N];
		int[] colIndexB = new int[pred2idx.size() + 1];
		colIndexB[colIndexB.length - 1] = N;

		int n = 0;
		int c = 0;

		for (String pred : predToEntPair.keySet()) {
			colIndexB[c++] = n;
			// List<Integer> neighs = new ArrayList<>();
			// List<Double> scores = new ArrayList<>();
			for (String entPair : predToEntPair.get(pred)) {
				rowIndexB[n] = argPair2idx.get(entPair);
				xB[n] = EntailGraph.getScore(pred, entPair) / entPairToSumNeighs.get(entPair);
				if (xB[n] == 0) {
					System.out.println("x 0: " + EntailGraph.getScore(pred, entPair) + " " + predToSumNeighs.get(pred)
							+ " " + pred + " " + entPair);
				}
				// System.out.println("x: " + x[n]);
				n++;

				// neighs.add(name2idx.get(sn));
				// scores.add(EntailGraph.getScore(s, sn));
			}

			// RandWalkNode node = new RandWalkNode(s, name2idx.get(s), neighs, scores,
			// predToSumNeighs.get(s));
			// nodes.add(node);
		}

		// A
		int[] rowIndexA = new int[N];
		double[] xA = new double[N];
		int[] colIndexA = new int[argPair2idx.size() + 1];
		colIndexA[colIndexA.length - 1] = N;

		n = 0;
		c = 0;

		for (String entPair : entPairToPred.keySet()) {// column
			colIndexA[c++] = n;
			// List<Integer> neighs = new ArrayList<>();
			// List<Double> scores = new ArrayList<>();
			for (String pred : entPairToPred.get(entPair)) {// row

				// if (w!=0) {
				rowIndexA[n] = pred2idx.get(pred);
				xA[n] = EntailGraph.getScore(pred, entPair) / predToSumNeighs.get(pred);
				// System.out.println("x: " + x[n]);
				n++;
				// }

				// neighs.add(name2idx.get(sn));
				// scores.add(EntailGraph.getScore(sn, s));
			}

			// RandWalkNode node = new RandWalkNode(s, name2idx.get(s), neighs, scores,
			// entPairToSumNeighs.get(s));
			// nodes.add(node);

		}

		// System.out.println("colIndexes: ");
		// for (int j : colIndex) {
		// System.out.println(j);
		// }

		MySparseMatrix A = new MySparseMatrix(pred2idx.size(), argPair2idx.size(), xA, rowIndexA, colIndexA);
//				.threshold(numAllSeenEdges);// TODO: be careful
		MySparseMatrix B = new MySparseMatrix(argPair2idx.size(), pred2idx.size(), xB, rowIndexB, colIndexB);
//				.threshold(numAllSeenEdges);
		System.out.println("A, B size: " + A.size() + " " + B.size());
		//TODO: be careful with this, added on 15 Nov
		A = A.threshold(ConstantsRWalk.NThr);
		B = B.threshold(ConstantsRWalk.NThr);
		mat = (A.abmm(B)).threshold(ConstantsRWalk.threshold);

		for (double x : mat.values()) {
			if (x == 0) {
				System.out.println("what?? " + x);
			}
		}

		matNoHub = A.removeHubs(hubs, pred2idx, argPair2idx).abmm(B.removeHubs(hubs, argPair2idx, pred2idx))
				.threshold(ConstantsRWalk.threshold);
		// mat = A * B

		// mat = new MySparseMatrix(name2idx.size(), name2idx.size(), x, rowIndex,
		// colIndex);
		System.out.println("mat size: " + mat.size() + " ncols, nrows: " + mat.ncols() + " " + mat.nrows());
		System.out.println("matNoHub size: " + matNoHub.size() + " ncols, nrows: " + matNoHub.ncols() + " " + matNoHub.nrows());
		// mat = mat.threshold(ConstantsRWalk.threshold);
		// System.out.println(
		// "mat size after threshold: " + mat.size() + " ncols, nrows: " + mat.ncols() +
		// " " + mat.nrows());

		// mat = mat.abmm(mat).threshold(ConstantsRWalk.threshold);
		// mat = mat.subMatrix(predToEntPair.size());
	}

	void setRandWalkMat(int K) {
		randWalkMats = new ArrayList<>();
		randWalkMats.add(mat);
		

		// MySparseMatrix randWalkMat =
		// matNoHub.threshold(ConstantsRWalk.thresholdMul);//TODO: be careful!
//		MySparseMatrix randWalkMat = matNoHub.threshold(ConstantsRWalk.NThr);// TODO: be careful!
//		MySparseMatrix matThr = randWalkMat;
		
		MySparseMatrix randWalkMat = mat;
		for (int k = 0; k < K - 1; k++) {
			System.out.println("multiplying matrix, current size: " + randWalkMat.size());
			randWalkMat = randWalkMat.abmm(mat);//.threshold(ConstantsRWalk.NThr);
			randWalkMats.add(randWalkMat);
		}

	}

	void writeResults() {
		String fnameTProp = ConstantsAgg.simsFolder + "/" + types + "_rw.txt";
		writeTPropResults(randWalkMats, fnameTProp);
		System.out.println("results written for: " + fnameTProp);
	}

	void writeTPropResults(List<MySparseMatrix> mats, String fnameTProp) {

//		int idx1 = pred2idx.get("(consider.2,consider.as.2)#thing_1#thing_2");
//		int idx2 = pred2idx.get("(offer.2,offer.as.2)#thing_1#thing_2");
//		double score = mats.get(0).get(idx1, idx2);
//		double score2 = mats.get(0).get(idx2, idx1);
//		System.out.println("test scores: " + score + score2);

		List<MySparseMatrix> matsTranspose = new ArrayList<>();
		for (MySparseMatrix mat : mats) {
			matsTranspose.add(mat.transpose());
		}

		mats = matsTranspose;

		PrintStream op = null;
		try {
			op = new PrintStream(new File(fnameTProp));
		} catch (Exception e) {
			e.printStackTrace();
		}
		// List<String> predList = allPredsList.get(allPredsList.size() - 1);

		int N = predToEntPair.size();
		op.println(types + " " + " num preds: " + N);

		int thisColIdx = 0;
		MySparseMatrix lastMat = mats.get(mats.size() - 1);

		for (String pred : predToEntPair.keySet()) {
			assert pred2idx.get(pred) == thisColIdx;

			op.println("predicate: " + pred);
			op.println("max num neighbors: " + (lastMat.colIndex[thisColIdx + 1] - lastMat.colIndex[thisColIdx]));// TODO
			op.println();
			for (int L = 0; L < mats.size(); L++) {
				MySparseMatrix thisMat = mats.get(L);

				int[] colIndex = thisMat.colIndex;
				int[] rowIndex = thisMat.rowIndex;
				double[] values = thisMat.values();

				List<SimpleScore> scores = new ArrayList<>();
				List<SimpleScore> scores_cos = new ArrayList<>();

				double sumOuts = 0;

				for (int j = colIndex[thisColIdx]; j < colIndex[thisColIdx + 1]; j++) {

					String pred2 = pred2idx.inverse().get(rowIndex[j]);
					double w = values[j];
					sumOuts += w;

					if (w < 1e-4) {// TODO: be careful
						continue;
					}

					double w_cos = Math.sqrt(w * EntailGraph.getCosPreds(pred, pred2));

					// System.out.println("w:" + w);
					scores.add(new SimpleScore("", pred2, (float) w));
					scores_cos.add(new SimpleScore("", pred2, (float) w_cos));
				}

//				if (sumOuts<.99 || sumOuts>1.01) {
					System.out.println(pred);
					System.out.println("sanity: " + sumOuts);
//				}

				Collections.sort(scores, Collections.reverseOrder());
				Collections.sort(scores_cos, Collections.reverseOrder());

				op.println("rand walk " + L + " sims");
				for (SimpleScore sc : scores) {
					op.println(sc.pred2 + " " + sc.score);
				}

				op.println();

				op.println("rand walk cos " + L + " sims");
				for (SimpleScore sc : scores_cos) {
					op.println(sc.pred2 + " " + sc.score);
				}

				op.println();
			}
			thisColIdx++;
		}
		op.close();
	}

	public static Map<String, Double> loadTriplesToScore(String fname) throws IOException {
		
		BufferedReader br = new BufferedReader(new FileReader(new File(fname)));
		Map<String, Double> ret = new HashMap<>();
		entPairToPred = new LinkedHashMap<>();
		predToEntPair = new LinkedHashMap<>();
		entPairToSumNeighs = new HashMap<>();
		predToSumNeighs = new HashMap<>();

		String line = null;
		int index = 0;
		while ((line = br.readLine()) != null) {
			try {
				index++;
				if (index >= 1000) {
					break;// TODO: remove this
				}
				if (index % 100000 == 0) {
					System.out.println(index);
				}

				String[] ss = line.trim().split(" ");
				String pred = ss[0];
				boolean reverse = pred.endsWith("reverse");
				pred = pred.replace("_reverse", "");
				String e1 = ss[1];

				int N_ap = 0;// num arg-pairs (if existing) read other than the
				// original ones

				for (int i = 2; i < ss.length; i += 2) {
					String e2 = ss[i];
					double prob = -1;
					try {
						prob = Double.parseDouble(ss[i + 1]);
						if (prob <= ConstantsRWalk.threshold) {
							continue;// if it's sorted, you can even break
						}
						if (prob == 0) {
							System.out.println("prob 0: " + ss[i] + " " + ss[i + 1]);
						}
					} catch (Exception e) {
						e.printStackTrace();
						System.out.println(ss[i] + " " + ss[i + 1]);
						break;
					}

					String triple;
					String entPair;
					String entPair_reverse;
					if (!reverse) {
						triple = e1 + "#" + pred + "#" + e2;
						entPair = e1 + "#" + e2;
						entPair_reverse = e2 + "#" + e1;
					} else {
						triple = e2 + "#" + pred + "#" + e1;
						entPair = e2 + "#" + e1;
						entPair_reverse = e1 + "#" + e2;
					}

					if (allTriples.contains(triple) || N_ap < ConstantsRWalk.convEArgPairNeighs) {// TODO: be careful

						ret.putIfAbsent(triple, prob);
						if (!allTriples.contains(triple)) {
							N_ap++;
						}
						else {
							numAllSeenEdges++;
						}
						
						String pred_orig = pred + "#thing_1#thing_2";
						String pred_reverse = pred + "#thing_2#thing_1";

						RandWalkMatrix.numAllStoredTriples++;
						if (RandWalkMatrix.numAllStoredTriples % 100000 == 0) {
							System.err.println(RandWalkMatrix.numAllStoredTriples);
							System.err.println("triple to score size: " + ret.size());

							int mb = 1024 * 1024;
							long usedMb = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / mb;
							// System.gc();
							System.err.println("used mb: " + usedMb);

						}

						entPairToPred.putIfAbsent(entPair, new ArrayList<>());
						entPairToPred.putIfAbsent(entPair_reverse, new ArrayList<>());
						entPairToSumNeighs.putIfAbsent(entPair, 0.0);
						entPairToSumNeighs.putIfAbsent(entPair_reverse, 0.0);
						predToEntPair.putIfAbsent(pred_orig, new ArrayList<>());
						predToEntPair.putIfAbsent(pred_reverse, new ArrayList<>());
						predToSumNeighs.putIfAbsent(pred_orig, 0.0);
						predToSumNeighs.putIfAbsent(pred_reverse, 0.0);

						if (!entPairToPred.get(entPair).contains(pred_orig)) {
							entPairToPred.get(entPair).add(pred_orig);
							entPairToSumNeighs.put(entPair, entPairToSumNeighs.get(entPair) + prob);

							predToEntPair.get(pred_orig).add(entPair);
							predToSumNeighs.put(pred_orig, predToSumNeighs.get(pred_orig) + prob);
						}

						if (!entPairToPred.get(entPair_reverse).contains(pred_reverse)) {
							entPairToPred.get(entPair_reverse).add(pred_reverse);
							entPairToSumNeighs.put(entPair_reverse, entPairToSumNeighs.get(entPair_reverse) + prob);

							predToEntPair.get(pred_reverse).add(entPair_reverse);
							predToSumNeighs.put(pred_reverse, predToSumNeighs.get(pred_reverse) + prob);
						}
					}
				}
			} catch (Exception e) {

			}

		}
		br.close();

		System.out.println("pred hubs");
		findHubs(predToSumNeighs);

		System.out.println("ent hubs");
		findHubs(entPairToSumNeighs);

		return ret;
	}

	static void findHubs(Map<String, Double> predToSumNeighs) {
		List<SimpleSpot> simpleSpots = new ArrayList<>();
		for (String s : predToSumNeighs.keySet()) {
			simpleSpots.add(new SimpleSpot(s, predToSumNeighs.get(s)));
		}
		Collections.sort(simpleSpots, Collections.reverseOrder());
		for (int i = 0; i < 20; i++) {
			System.out.println(simpleSpots.get(i).spot + " " + simpleSpots.get(i).count);
			hubs.add(simpleSpots.get(i).spot);
		}
	}

	// void walkFromAllNodes(int L) {
	// for (int i = 0; i < numPreds; i++) {
	// for (int j = 0; j < ConstantsAgg.randWalkTimes; j++) {
	// int endNode = walkFromNode(i, L);
	//
	// }
	// }
	// }

	// int walkFromNode(int i, int L) {
	// int curIdx = i;
	// System.out.println("first node: " + nodes.get(curIdx).name);
	// for (int l = 0; l < L; l++) {
	// curIdx = nodes.get(curIdx).getNextRandNode();
	// System.out.println("node: " + nodes.get(curIdx).name);
	// }
	// return curIdx;
	// }

	static double computeScoreTwoPreds(String rel1, String rel2) {
		double ret = 0;
		// System.out.println(EntailGraphFactoryAggregator.predToEntPair);
		if (!RandWalkMatrix.predToEntPair.containsKey(rel1)) {
			return 0;
		}
		List<String> connectedEntPairs = RandWalkMatrix.predToEntPair.get(rel1);
		System.out.println("num connected to rel1: " + rel1 + " " + connectedEntPairs.size());
		for (String entPair : connectedEntPairs) {
			if (RandWalkMatrix.entPairToPred.containsKey(entPair)) {
				double denom1 = RandWalkMatrix.predToSumNeighs.get(rel1);
				double p1 = 0, p2 = 0;
				if (denom1 != 0) {
					p1 = EntailGraph.getScore(rel1, entPair) / denom1;
				} else {
					System.out.println("denom1 0: " + rel1);
				}

				double denom2 = RandWalkMatrix.entPairToSumNeighs.get(entPair);
				if (denom2 != 0) {
					p2 = EntailGraph.getScore(rel2, entPair) / denom2;
				}
				ret += p1 * p2;
			}
		}
		return ret;
	}

	public static void main(String[] args) {
		ConstantsRWalk.setAggConstants();
		loadLinkPredInfo();
		double score = computeScoreTwoPreds("(consider.2,consider.as.2)#thing_1#thing_2",
				"(offer.2,offer.as.2)#thing_1#thing_2");
		System.out.println("(capture.2,capture.in.2)#(mile.of.1,mile.of.2)#True score:" + score);
		if (!(new File(ConstantsAgg.simsFolder)).exists()) {
			(new File(ConstantsAgg.simsFolder)).mkdirs();
		}
		RandWalkMatrix rwMat = new RandWalkMatrix(tripleToScore, entPairToPred, entPairToSumNeighs, predToEntPair,
				predToSumNeighs);
		rwMat.setRandWalkMat(ConstantsRWalk.L);
		rwMat.writeResults();

	}

}
