package entailment.vector;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import entailment.Util;

public class PredicateVector extends SimplePredicateVector {
	// Don't store a HashMap here so that after the pvecs are formed, the speed
	// will be high.
	ArrayList<Integer> argIdxes;// we store in sparse format
	ArrayList<Float> vals;
	ArrayList<String> minRightIntervals;
	ArrayList<String> maxLeftIntervals;
	ArrayList<Float> PMIs;
	HashMap<Integer, Integer> argIdxToArrayIdx;
	// HashSet<String> ents;
	EntailGraph entGraph;

	float norm2 = 0;
	float sumPMIs = 0;
	float norm1 = 0;

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
		this.vals = new ArrayList<Float>();
		this.minRightIntervals = new ArrayList<>();
		this.maxLeftIntervals = new ArrayList<>();
		this.similarityInfos = new HashMap<Integer, SimilaritiesInfo>();
		// this.ents = new HashSet<String>();
		this.entGraph = entGraph;
	}

	// adds the idx of an arg-pair. It returns whether this idx has been added
	// for the first time
	void addArgPair(int idx, String timeInterval, float count) {
		if (!argIdxToArrayIdx.containsKey(idx)) {
			argIdxToArrayIdx.put(idx, argIdxToArrayIdx.size());
			argIdxes.add(idx);
			vals.add(0.0f);
			if (EntailGraphFactoryAggregator.useTimeEx) {
				minRightIntervals.add("3000-01-01");
				maxLeftIntervals.add("1000-01-01");
			} else {
				minRightIntervals.add(null);
				maxLeftIntervals.add(null);
			}

			float prevCount = entGraph.argPairIdxToCount.get(idx);
			
			if (EntailGraphFactoryAggregator.typeScheme != EntailGraphFactoryAggregator.TypeScheme.LDA) {
				entGraph.argPairIdxToCount.put(idx, prevCount + 1);// TODO:
																	// should we
																	// care
																	// about
																	// this in
																	// cutoffs?
			} else {
				entGraph.argPairIdxToCount.put(idx, prevCount + count);// TODO:
																		// should
																		// we
																		// care
																		// about
																		// this
																		// in
																		// cutoffs?
			}
			EntailGraphFactoryAggregator.allNonZero++;
		}
		int arrIdx = argIdxToArrayIdx.get(idx);
		vals.set(arrIdx, vals.get(arrIdx) + count);

//		if (EntailGraphFactoryAggregator.typeScheme != EntailGraphFactoryAggregator.TypeScheme.LDA) {
//			float prevCount = entGraph.argPairIdxToCount.get(idx);
//			entGraph.argPairIdxToCount.put(idx, prevCount + count);// TODO:
//																	// should we
//																	// care
//																	// about
//																	// this in
//																	// cutoffs?
//		}

		if (EntailGraphFactoryAggregator.useTimeEx) {

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
	void cutoffInfreqArgPairs() {
		HashSet<Integer> toberemovedIdxes = new HashSet<>();
		for (int i = 0; i < argIdxes.size(); i++) {
			// if (vals.get(i)<EntailGraphFactory.minOccArgPairInPredicate){
			// toberemovedIdxes.add(i);
			// }
			if (entGraph.argPairIdxToCount.get(argIdxes.get(i)) < entGraph.minPredForArgPair) {
				toberemovedIdxes.add(i);
			}
			// else{
			// System.err.println("no remove:
			// "+entGraph.argPairIdxToCount.get(argIdxes.get(i)));
			// }
		}

		ArrayList<Integer> argIdxes = new ArrayList<>();// we store in sparse
														// format
		ArrayList<Float> vals = new ArrayList<>();
		HashMap<Integer, Integer> argIdxToArrayIdx = new HashMap<>();

		for (int i = 0; i < this.argIdxes.size(); i++) {
			if (!toberemovedIdxes.contains(i)) {
				argIdxToArrayIdx.put(this.argIdxes.get(i), vals.size());
				vals.add(this.vals.get(i));
				argIdxes.add(this.argIdxes.get(i));
			}
		}

		this.argIdxes = argIdxes;
		this.vals = vals;
		this.argIdxToArrayIdx = argIdxToArrayIdx;
	}

	// float dotProd(PredicateVector pvec2) {
	// float ret = 0;
	// int arrIdx = 0;
	// for (int idx : argIdxes) {
	// if (!pvec2.argIdxToArrayIdx.containsKey((idx))) {
	// arrIdx++;
	// continue;
	// }
	// float val = vals.get(arrIdx);
	// int arrIdx2 = pvec2.argIdxToArrayIdx.get(idx);
	// float val2 = pvec2.vals.get(arrIdx2);
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
			float count = vals.get(arrIdx);
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
	// float dotProd = dotProd(predToVec.get(pred));
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
		float n = 0;
		for (float v : vals) {
			n += v * v;
		}
		this.norm2 = (float) Math.sqrt(n);
	}

	void setNorm1() {
		float n = 0;
		for (float v : vals) {
			n += v;
		}
		this.norm1 = n;
		if (n == 0) {
			System.err.println("norm1 is zero");
		}
	}

	void setSumPMIs() {
		float n = 0;
		for (float v : PMIs) {
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
			float cosSim = simInfo.basics.dotProd;
			cosSim /= (norm2 * pvec2.norm2);
			// if (entGraph.writeSims) {
			// cosSimList.add(new Similarity(pvec2.predicate, cosSim));
			// }

			// add time sims
			float timeSim;
			if (!EntailGraphFactoryAggregator.useTimeEx) {
				timeSim = 0;
				// timeSims.add(new Similarity(pvec2.predicate, 0));
			} else {
				timeSim = (float) (simInfo.basics.timePreceding) / this.argIdxes.size();
				// timeSims.add(new Similarity(pvec2.predicate, timeSim));
			}

			// add Weed's prob similarity
			if (simInfo.basics.sumFreq == 0) {
				System.err.println("simInfo is zero");
			}
			if (simInfo2.basics.sumFreq == 0) {
				System.err.println("simInfo2 is zero");
			}
			float weedProbPr = (float) (simInfo.basics.sumFreq + EntailGraphFactoryAggregator.smoothParam)
					/ (this.norm1 + 2 * EntailGraphFactoryAggregator.smoothParam);
			float weedProbRec = (float) (simInfo2.basics.sumFreq + EntailGraphFactoryAggregator.smoothParam)
					/ (pvec2.norm1 + 2 * EntailGraphFactoryAggregator.smoothParam);
			float weedProbSim = (2 * weedProbPr * weedProbRec) / (weedProbPr + weedProbRec);
			if (new Float(weedProbPr).isNaN()) {
				System.err.println(this.predicate + " " + pvec2.predicate);
				System.err.println("Nan: " + weedProbPr + " " + weedProbPr + " " + weedProbRec + " "
						+ simInfo.basics.sumFreq + simInfo2.basics.sumFreq + " " + this.norm1 + " " + pvec2.norm2);
			}
			// if (entGraph.writeSims) {
			// WeedsProbList.add(new Similarity(pvec2.predicate, weedProbSim));
			// }

			// float SRSim = 0;
			// float SRBinarySim = 0;
			//
			// if (this.norm1 != 0 && pvec2.norm1 != 0) {
			// float SR = simInfo.basics.SR;
			// float p = (SR / this.norm1);
			// float pp = 1 - p;
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
			// float SRBinary = simInfo.basics.SRBinary;
			// float p = SRBinary / this.argIdxes.size();
			// float pp = 1 - p;
			// if (p != 0) {
			// SRBinarySim = p * (float) Math.log((p * entGraph.nnz) /
			// pvec2.argIdxes.size());
			// }
			// if (pp != 0) {
			// SRBinarySim += pp * (float) Math.log((pp * entGraph.nnz) /
			// pvec2.argIdxes.size());
			// }
			// SRBinarySim *= SRBinary;
			// }

			float weedPMIPr = 0;
			float weedPMIRec;
			float weedPMISim = 0;
			float LinSim = 0;
			float BIncSim = 0;

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

				BIncSim = (float) Math.sqrt(LinSim * weedPMIPr);
				// if (entGraph.writeSims) {
				// BIncList.add(new Similarity(pvec2.predicate, BIncSim));
				// }
			}

			simInfo.setSims(cosSim, weedProbSim, weedPMISim, LinSim, BIncSim, timeSim, weedPMIPr);

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

	static void writeSims(PrintStream graphOp2, ArrayList<Similarity> simsList, String simName) {
		graphOp2.println();

		Collections.sort(simsList, Collections.reverseOrder());
		graphOp2.println(simName);
		for (int i = 0; i < Math.min(simsList.size(), EntailGraph.numSimilarsToShow); i++) {
			graphOp2.println(simsList.get(i).pred + " " + simsList.get(i).sim);
		}
	}

}

class Similarity implements Comparable<Similarity> {
	String pred;
	float sim;

	public Similarity(String pred, float sim) {
		this.pred = pred;
		this.sim = sim;
	}

	public int compareTo(Similarity dp2) {
		return (new Float(sim)).compareTo(new Float(dp2.sim));
		// if (sim > dp2.sim) {
		// return 1;
		// } else if (sim < dp2.sim) {
		// return -1;
		// }
		//
		// return 0;
	}

}

class ArgPair implements Comparable<ArgPair> {
	String argPairStr;
	float count;

	public ArgPair(String argPairStr, float count) {
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