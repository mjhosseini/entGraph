package entailment.vector;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import constants.ConstantsAgg;
import entailment.Util;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

public class PredicateVector extends SimplePredicateVector {
	// Don't store a HashMap here so that after the pvecs are formed, the speed
	// will be high.
	List<Integer> argIdxes;// we store in sparse format
	List<Double> vals;
	List<String> minRightIntervals;
	List<String> maxLeftIntervals;
	List<Double> PMIs;
	Map<Integer, Integer> argIdxToArrayIdx;
	// Int2IntMap argIdxToArrayIdx;

	// Int2IntMap testmap = new Int2IntOpenHashMap();

	// HashSet<String> ents;
	EntailGraph entGraph;

	double norm2 = 0;
	double sumPMIs = 0;
	double norm1 = 0;

	void clean() {
		this.argIdxes = null;
		this.vals = null;
		this.PMIs = null;
		this.argIdxToArrayIdx = null;
		this.minRightIntervals = null;
		this.maxLeftIntervals = null;
	}

	public PredicateVector(String predicate, int uniqueId, EntailGraph entGraph) {
		this.predicate = predicate;
		this.uniqueId = uniqueId;
		this.argIdxes = new ArrayList<Integer>();
		this.argIdxToArrayIdx = new HashMap<Integer, Integer>();
		// this.argIdxToArrayIdx = new Int2IntOpenHashMap();
		this.vals = new ArrayList<Double>();
		if (ConstantsAgg.useTimeEx) {
			this.minRightIntervals = new ArrayList<>();
			this.maxLeftIntervals = new ArrayList<>();
		}
		this.similarityInfos = new HashMap<Integer, SimilaritiesInfo>();
		// this.ents = new HashSet<String>();
		this.entGraph = entGraph;

	}

	// adds the idx of an arg-pair. It records whether this idx has been added
	// for the first time
	void addArgPair(int idx, String timeInterval, double count) {
		if (!argIdxToArrayIdx.containsKey(idx)) {
			argIdxToArrayIdx.put(idx, argIdxToArrayIdx.size());
			argIdxes.add(idx);
			vals.add(0.0);
			if (ConstantsAgg.useTimeEx) {
				minRightIntervals.add("3000-01-01");
				maxLeftIntervals.add("1000-01-01");
			}

			double prevCount = entGraph.argPairIdxToCount.get(idx);

			if (EntailGraphFactoryAggregator.typeScheme != EntailGraphFactoryAggregator.TypeScheme.LDA) {
				entGraph.argPairIdxToCount.put(idx, prevCount + 1);

			} else {
				entGraph.argPairIdxToCount.put(idx, prevCount + count);
			}
			EntailGraphFactoryAggregator.allNonZero++;
		}
		int arrIdx = argIdxToArrayIdx.get(idx);
		vals.set(arrIdx, vals.get(arrIdx) + count);

		if (ConstantsAgg.computeProbELSims) {
			double prevOcc = entGraph.argPairIdxToOcc.get(idx);
			entGraph.argPairIdxToOcc.put(idx, prevOcc + count);
		}
		// if (EntailGraphFactoryAggregator.typeScheme !=
		// EntailGraphFactoryAggregator.TypeScheme.LDA) {
		// double prevCount = entGraph.argPairIdxToCount.get(idx);
		// entGraph.argPairIdxToCount.put(idx, prevCount + count);// TODO:
		// // should we
		// // care
		// // about
		// // this in
		// // cutoffs?
		// }

		if (ConstantsAgg.useTimeEx) {

			String[] lr = Util.getLeftRightTimes(timeInterval);
			// if (lr.length>0){
			// System.out.println("left, right: "+lr[0]+ " "+lr[1]);
			// }
			if (lr.length == 2 && lr[0].length() > 0 && lr[0].compareTo(maxLeftIntervals.get(arrIdx)) > 0) {
				maxLeftIntervals.set(arrIdx, lr[0]);
			}
			if (lr.length == 2 && lr[1].length() > 0 && lr[1].compareTo(minRightIntervals.get(arrIdx)) < 0) {
				minRightIntervals.set(arrIdx, lr[1]);
			}
		}
	}

	// we remove any argpair that has less than ... occurrences
	void cutoffInfreqArgPairs(int argPAirCutoff) {
		HashSet<Integer> toberemovedIdxes = new HashSet<>();

		for (int i = 0; i < argIdxes.size(); i++) {
			// if (vals.get(i)<EntailGraphFactory.minOccArgPairInPredicate){
			// toberemovedIdxes.add(i);
			// }

			if (entGraph.argPairIdxToCount.get(argIdxes.get(i)) < argPAirCutoff) {
				toberemovedIdxes.add(i);
			}
			// else{
			// System.err.println("no remove:
			// "+entGraph.argPairIdxToCount.get(argIdxes.get(i)));
			// }
		}

		ArrayList<Integer> argIdxes = new ArrayList<>();// we store in sparse
														// format
		ArrayList<Double> vals = new ArrayList<>();
		// Map<Integer, Integer> argIdxToArrayIdx = new HashMap<>();
		Int2IntMap argIdxToArrayIdx = new Int2IntOpenHashMap();

		for (int i = 0; i < this.argIdxes.size(); i++) {
			if (!toberemovedIdxes.contains(i)) {
				argIdxToArrayIdx.put((int) this.argIdxes.get(i), (int) vals.size());
				vals.add(this.vals.get(i));
				argIdxes.add(this.argIdxes.get(i));
			}
		}

		this.argIdxes = argIdxes;
		this.vals = vals;
		this.argIdxToArrayIdx = argIdxToArrayIdx;
	}

	int getNumApsToRetain() {
		if (EntailGraphFactoryAggregator.predNumArgPairsNS.containsKey(this.predicate)) {
			int NSBasedAllowed = EntailGraphFactoryAggregator.predNumArgPairsNS.get(this.predicate);
			if (NSBasedAllowed < ConstantsAgg.numArgPairsNSBasedAlwaysAllowed) {
				return ConstantsAgg.numArgPairsNSBasedAlwaysAllowed;
			} else {
				return NSBasedAllowed;
			}

		} else {
			return argIdxes.size();
		}
	}

	Set<Integer> getNSPredBasedToBeRemovedArrIdxes() {
		int numAllowed = getNumApsToRetain();

		List<Integer> a = new ArrayList<>();
		for (double i : vals) {
			a.add((int) i);
		}

		Set<Integer> ret = new HashSet<>();

		Collections.sort(a, Collections.reverseOrder());
		if (numAllowed >= a.size()) {
			return ret;
		} else {
			int cutoff = a.get(numAllowed - 1);

//			System.out.println("pred based cutoff: " + predicate + " " + cutoff);

			Set<Integer> remainingIdxes = new HashSet<>();

			for (int i = 0; i < vals.size(); i++) {
				if (vals.get(i) > cutoff) {
					remainingIdxes.add(i);
				}
			}

			for (int i = 0; i < vals.size(); i++) {
				if (vals.get(i) == cutoff && remainingIdxes.size() <= numAllowed) {
					remainingIdxes.add(i);
				}
			}

			for (int i = 0; i < vals.size(); i++) {
				if (!remainingIdxes.contains(i)) {
					ret.add(i);
					// System.out.println("removing: "+ entGraph.argPairs.get(argIdxes.get(i)));
				}
			}

			return ret;
		}
	}

	// we only retain the first numAPsToRetain arg-pairs
	void cutoffInfreqArgPairsPredBased() {
		Set<Integer> toberemovedIdxes = getNSPredBasedToBeRemovedArrIdxes();

		List<Integer> argIdxes = new ArrayList<>();// we store in sparse
													// format
		List<Double> vals = new ArrayList<>();
		// HashMap<Integer, Integer> argIdxToArrayIdx = new HashMap<>();
		Int2IntMap argIdxToArrayIdx = new Int2IntOpenHashMap();

		for (int i = 0; i < this.argIdxes.size(); i++) {
			if (!toberemovedIdxes.contains(i)) {
				argIdxToArrayIdx.put((int) this.argIdxes.get(i), (int) vals.size());
				vals.add(this.vals.get(i));
				argIdxes.add(this.argIdxes.get(i));
			}
		}

		this.argIdxes = argIdxes;
		this.vals = vals;
		this.argIdxToArrayIdx = argIdxToArrayIdx;
	}

	// double dotProd(PredicateVector pvec2) {
	// double ret = 0;
	// int arrIdx = 0;
	// for (int idx : argIdxes) {
	// if (!pvec2.argIdxToArrayIdx.containsKey((idx))) {
	// arrIdx++;
	// continue;
	// }
	// double val = vals.get(arrIdx);
	// int arrIdx2 = pvec2.argIdxToArrayIdx.get(idx);
	// double val2 = pvec2.vals.get(arrIdx2);
	// ret += val * val2;
	// arrIdx++;
	// }
	// return ret;
	// }

	String getArgPairs() {
		StringBuilder ret = new StringBuilder();
		ArrayList<ArgPair> thisArgPairs = new ArrayList<>();
		for (int argIdx : argIdxes) {
			String argPairStr = entGraph.argPairs.get(argIdx);
			int arrIdx = argIdxToArrayIdx.get(argIdx);
			double count;
			if (EntailGraphFactoryAggregator.writePMIorCount) {
				count = PMIs.get(arrIdx);
			} else {
				count = vals.get(arrIdx);
			}

			ArgPair argPair = new ArgPair(argPairStr, count);
			thisArgPairs.add(argPair);
		}

		Collections.sort(thisArgPairs);
		for (ArgPair argPair : thisArgPairs) {
			ret.append((argPair.argPairStr + ": " + argPair.count) + "\n");
		}

		return ret.toString();
	}

	// This was naive!
	// String computeDotProds() {
	// String ret = "";
	// ArrayList<DotProd> dotProds = new ArrayList<>();
	// for (String pred : predToVec.keySet()) {
	// if (pred.equals(this.predicate)) {
	// continue;
	// }
	// double dotProd = dotProd(predToVec.get(pred));
	// if (dotProd > 0) {
	// dotProds.add(new DotProd(pred, dotProd));
	// //
	// }
	// Collections.sort(dotProds);
	// for (DotProd dp : dotProds) {
	// ret += dp.pred + ": " + dp.dot + "\n";
	// }
	// }
	// return ret;
	// }

	public static void main(String[] args) {
		// String fileName = args[0];
		// try {
		// convertToPArgFormat(fileName);
		// } catch (FileNotFoundException e) {
		// e.printStackTrace();
		// }
	}

	void setNorm2() {
		double n = 0;
		for (double v : vals) {
			n += v * v;
		}
		this.norm2 = (double) Math.sqrt(n);
		// System.out.println("norm2: "+norm2);
	}

	void setNorm1() {
		double n = 0;
		for (double v : vals) {
			n += v;
		}
		this.norm1 = n;
		if (n == 0) {
			System.err.println("norm1 is zero");
		}
		// System.out.println("norm1: "+norm1);
	}

	void setSumPMIs() {
		double n = 0;
		for (double v : PMIs) {
			if (v > 0) {
				n += v;
			}
		}
		this.sumPMIs = n;
	}

	void computeSimilarities() {

		if (entGraph.writeSims) {
			// entGraph.graphOp2.println("predicate: " + predicate);
			// entGraph.graphOp2.println("num neighbors: " +
			// similarityInfos.size());
		}

		if (similarityInfos.size() == 0) {
			if (entGraph.writeSims) {
				// entGraph.graphOp2.println();
			}
			return;
		}

		// For the sake of efficiency, I'm computing all the scores in this
		// function. Because, iterating over
		// the hashmap of simInfos will not be very efficient (especially when
		// we have untyped and large ent graphs)!

		// ArrayList<Similarity> cosSimList = new ArrayList<Similarity>();
		// ArrayList<Similarity> WeedsProbList = new ArrayList<Similarity>();
		// ArrayList<Similarity> WeedsPMIList = new ArrayList<Similarity>();
		// ArrayList<Similarity> LinList = new ArrayList<Similarity>();
		// ArrayList<Similarity> BIncList = new ArrayList<Similarity>();
		// ArrayList<Similarity> timeSims = new ArrayList<Similarity>();

		for (int pvecIdx : similarityInfos.keySet()) {
			SimilaritiesInfo simInfo = similarityInfos.get(pvecIdx);
			PredicateVector pvec2 = (PredicateVector) this.entGraph.getPvecs().get(pvecIdx);
			SimilaritiesInfo simInfo2 = pvec2.similarityInfos.get(this.uniqueId);

			// add cos similarity
			double cosSim;
			if (!ConstantsAgg.embBasedScores && !EntailGraphFactoryAggregator.anchorBasedScores) {
				cosSim = simInfo.basics.dotProd;
				cosSim /= (norm2 * pvec2.norm2);
			} else {
				cosSim = 0;
			}

			// if (entGraph.writeSims) {
			// cosSimList.add(new Similarity(pvec2.predicate, cosSim));
			// }

			// add time sims
			double timeSim;
			if (!ConstantsAgg.useTimeEx) {
				timeSim = 0;
				// timeSims.add(new Similarity(pvec2.predicate, 0));
			} else {
				timeSim = (double) (simInfo.basics.timePreceding) / this.argIdxes.size();
				// timeSims.add(new Similarity(pvec2.predicate, timeSim));
			}

			// add Weed's prob similarity
			if (simInfo.basics.sumFreq == 0) {
				System.err.println("simInfo is zero");
			}
			if (simInfo2.basics.sumFreq == 0) {
				System.err.println("simInfo2 is zero");
			}
			double weedProbPr = (double) (simInfo.basics.sumFreq + EntailGraphFactoryAggregator.smoothParam)
					/ (this.norm1 + 2 * EntailGraphFactoryAggregator.smoothParam);
			double weedProbRec = (double) (simInfo2.basics.sumFreq + EntailGraphFactoryAggregator.smoothParam)
					/ (pvec2.norm1 + 2 * EntailGraphFactoryAggregator.smoothParam);
			double weedProbSim = (2 * weedProbPr * weedProbRec) / (weedProbPr + weedProbRec);
			if (new Double(weedProbPr).isNaN()) {
				System.err.println(this.predicate + " " + pvec2.predicate);
				System.err.println("Nan: " + weedProbPr + " " + weedProbPr + " " + weedProbRec + " "
						+ simInfo.basics.sumFreq + simInfo2.basics.sumFreq + " " + this.norm1 + " " + pvec2.norm2);
			}
			// if (entGraph.writeSims) {
			// WeedsProbList.add(new Similarity(pvec2.predicate, weedProbSim));
			// }

			// double SRSim = 0;
			// double SRBinarySim = 0;
			//
			// if (this.norm1 != 0 && pvec2.norm1 != 0) {
			// double SR = simInfo.basics.SR;
			// double p = (SR / this.norm1);
			// double pp = 1 - p;
			// if (p < 0) {
			// throw new RuntimeException("serious problem!");
			// }
			// if (p != 0) {
			// SRSim += p * Math.log((p * entGraph.numTuples) / pvec2.norm1);
			// }
			// if (pp != 0) {
			// SRSim += pp * Math.log((pp * entGraph.numTuples) / pvec2.norm1);
			// }
			//
			// SRSim *= SR;
			// }
			//
			// if (this.argIdxes.size() != 0 && pvec2.argIdxes.size() != 0) {
			// double SRBinary = simInfo.basics.SRBinary;
			// double p = SRBinary / this.argIdxes.size();
			// double pp = 1 - p;
			// if (p != 0) {
			// SRBinarySim = p * (double) Math.log((p * entGraph.nnz) /
			// pvec2.argIdxes.size());
			// }
			// if (pp != 0) {
			// SRBinarySim += pp * (double) Math.log((pp * entGraph.nnz) /
			// pvec2.argIdxes.size());
			// }
			// SRBinarySim *= SRBinary;
			// }

			double weedPMIPr = 0;
			double weedPMIRec;
			double weedPMISim = 0;
			double LinSim = 0;
			double BIncSim = 0;

			// add Weed's PMI similarity
			// The conditions says if there is any feature in the intersection!
			if (simInfo.basics.sumPMI > 0 && simInfo2.basics.sumPMI > 0) {
				weedPMIPr = (simInfo.basics.sumPMI + EntailGraphFactoryAggregator.smoothParam)
						/ (this.sumPMIs + 2 * EntailGraphFactoryAggregator.smoothParam);
				weedPMIRec = (simInfo2.basics.sumPMI + EntailGraphFactoryAggregator.smoothParam)
						/ (pvec2.sumPMIs + 2 * EntailGraphFactoryAggregator.smoothParam);
				weedPMISim = (2 * weedPMIPr * weedPMIRec) / (weedPMIPr + weedPMIRec);
				// if (entGraph.writeSims) {
				// WeedsPMIList.add(new Similarity(pvec2.predicate,
				// weedPMISim));
				// }

				LinSim = (simInfo.basics.sumPMI + simInfo2.basics.sumPMI + EntailGraphFactoryAggregator.smoothParam)
						/ (this.sumPMIs + pvec2.sumPMIs + 2 * EntailGraphFactoryAggregator.smoothParam);
				// if (entGraph.writeSims) {
				// LinList.add(new Similarity(pvec2.predicate, LinSim));
				// }

				BIncSim = (double) Math.sqrt(LinSim * weedPMIPr);
				// if (entGraph.writeSims) {
				// BIncList.add(new Similarity(pvec2.predicate, BIncSim));
				// }
			}

			// add cos similarity
			double probELSim = 0;
			if (ConstantsAgg.computeProbELSims) {
				if (EntailGraphFactoryAggregator.dsPredToPredToScore.containsKey(this.predicate)) {
					if (EntailGraphFactoryAggregator.dsPredToPredToScore.get(this.predicate)
							.containsKey(pvec2.predicate)) {
						probELSim = EntailGraphFactoryAggregator.dsPredToPredToScore.get(predicate)
								.get(pvec2.predicate);
					}
				}
			}

			simInfo.setSims(cosSim, weedProbSim, weedPMISim, LinSim, BIncSim, timeSim, weedPMIPr, probELSim);

		}

		// if (entGraph.writeSims){
		// writeSims(this.entGraph,cosSimList, "cos sims");
		// writeSims(this.entGraph, WeedsProbList, "Weed's probabilistic
		// model");
		// writeSims(this.entGraph, WeedsPMIList, "Weed's PMI model");
		// writeSims(this.entGraph, LinList, "Lin sim");
		// writeSims(this.entGraph, BIncList, "BInc sim");
		// entGraph.graphOp2.println();
		// entGraph.graphOp2.println();
		// }

	}

	// static void writeSims(EntailGraph entGraph, ArrayList<Similarity>
	// simsList, String simName) {
	// entGraph.graphOp2.println();
	//
	// Collections.sort(simsList, Collections.reverseOrder());
	// entGraph.graphOp2.println(simName);
	// for (int i = 0; i < Math.min(simsList.size(),
	// EntailGraph.numSimilarsToShow); i++) {
	// entGraph.graphOp2.println(simsList.get(i).pred + " " +
	// simsList.get(i).sim);
	// }
	// }

	public static void writeSims(PrintStream graphOp2, List<Similarity> simsList, String simName) {
		graphOp2.println();

		Collections.sort(simsList, Collections.reverseOrder());
		graphOp2.println(simName);
		for (int i = 0; i < Math.min(simsList.size(), EntailGraph.numSimilarsToShow); i++) {
			graphOp2.println(simsList.get(i).pred + " " + simsList.get(i).sim);
		}
	}

}

class ArgPair implements Comparable<ArgPair> {
	String argPairStr;
	double count;

	public ArgPair(String argPairStr, double count) {
		this.argPairStr = argPairStr;
		this.count = count;
	}

	public int compareTo(ArgPair argPair2) {
		if (count > argPair2.count) {
			return -1;
		} else if (count < argPair2.count) {
			return 1;
		}
		return 0;
	}

}