package entailment.vector;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import constants.ConstantsAgg;
import entailment.randWalk.RandWalkMatrix;
import entailment.vector.EntailGraphFactoryAggregator.LinkPredModel;
import entailment.vector.EntailGraphFactoryAggregator.ProbModel;

public class EntailGraph extends SimpleEntailGraph {

	static final int numSimilarsToShow = 1000000000;// inf!
	List<String> argPairs = new ArrayList<String>();// when we cutoff, we don't change this, but some won't be used
	Map<String, Integer> argPairToIdx = new HashMap<>();// same as above when we cutoff
	Map<Integer, Double> argPairIdxToCount = new HashMap<>();// number of predicates
	Map<Integer, Double> argPairIdxToOcc = new HashMap<>();// number of seen times, used for rand walk, etc

	private List<PredicateVector> pvecs = new ArrayList<PredicateVector>();

	double numTuples;
	int nnz;
	PrintStream graphOp1;// This is for writing the predicate vectors
	String opFileName;
	boolean writeInfo = false;
	final int minPredForArgPair;// min num of unique predicates for

	List<InvertedIdx> invertedIdxes;
	boolean unary = false;

	@Override
	public List<SimplePredicateVector> getPvecs() {
		ArrayList<SimplePredicateVector> ret = new ArrayList<>();
		for (PredicateVector pvec : pvecs) {
			ret.add(pvec);
		}
		return ret;
	}

	// deletes unnecessary objects!!!
	void clean11() {
		this.argPairToIdx = null;
		this.argPairs = null;
		this.invertedIdxes = null;
		for (PredicateVector pvec : pvecs) {
			pvec.clean();
		}
	}

	public EntailGraph(String types, String opFileName, int minPredForArgPair, boolean unary) {
		this.types = types;
		this.opFileName = opFileName;
		this.minPredForArgPair = minPredForArgPair;
		this.unary = unary;
		// try {
		// long t0 = System.currentTimeMillis();
		// buildVectors(fileName);
		// System.err.println("build vec time: "
		// + (System.currentTimeMillis() - t0));
		// } catch (IOException e) {
		// e.printStackTrace();
		// }
	}

	void writeLinkPredPercentiles() {
		if (EntailGraphFactoryAggregator.allPosLinkPredProbs.size() > 0) {
			System.out.println("num probs: " + EntailGraphFactoryAggregator.allPosLinkPredProbs.size());
			Collections.sort(EntailGraphFactoryAggregator.allPosLinkPredProbs);

			for (int percentile = 0; percentile <= 100; percentile++) {
				int index = (percentile * EntailGraphFactoryAggregator.allPosLinkPredProbs.size()) / 100;
				index = Math.min(index, EntailGraphFactoryAggregator.allPosLinkPredProbs.size() - 1);
				System.out.println("link pred prob percentile " + percentile + ": "
						+ EntailGraphFactoryAggregator.allPosLinkPredProbs.get(index));
			}
		}
	}

	int getNSBasedPredCutoff() {

		List<Integer> a = new ArrayList<>();
		for (PredicateVector pvec : pvecs) {
			a.add(pvec.argIdxes.size());
		}

		Collections.sort(a, Collections.reverseOrder());
		int numAllowed = EntailGraphFactoryAggregator.cutOffsNS.get(types)[0];
		if (numAllowed >= a.size()) {
			return -1;
		} else {
			int ret = a.get(numAllowed - 1);
			System.out.println("NS pred cutoff for " + types + " " + ret);
			return ret;
		}
	}

	int getNSBasedAPCutoff() {

		List<Integer> a = new ArrayList<>();
		for (double i : argPairIdxToCount.values()) {
			a.add((int) i);
		}

		Collections.sort(a, Collections.reverseOrder());
		int numAllowed = EntailGraphFactoryAggregator.cutOffsNS.get(types)[1];
		if (numAllowed >= a.size()) {
			return -1;
		} else {
			int ret = a.get(numAllowed - 1);

			if (!ConstantsAgg.addTimeStampToFeats && ConstantsAgg.relAddress.contains("_GG") && (types.equals("thing#location")
					|| types.equals("location#thing") || types.equals("thing#location-country")
					|| types.equals("location-country#thing") || types.equals("thing#thing"))) {
				int mult;
				if (ConstantsAgg.figerHierarchy) {
					mult = 2;
				} else {
					mult = 6;
				}
				System.out.println("multiplying arg pair cutoff by " + mult);
				ret *= mult;// TODO: be careful
			}

			System.out.println("NS argpair cutoff for " + types + " " + ret);
			return ret;
		}

	}

	void processGraph() {

		if (pvecs.size() <= 1) {
			return;// not interested in graphs with one node!!!
		}

		boolean shouldCutoffNSBased = ConstantsAgg.cutoffBasedonNSGraphs
				&& EntailGraphFactoryAggregator.cutOffsNS.containsKey(types)
				&& EntailGraphFactoryAggregator.type2RankNS.get(types) < ConstantsAgg.numTopTypePairs;

		// cutoff arg-pairs
		int argPAirCutoff = minPredForArgPair;
		if (shouldCutoffNSBased) {
			argPAirCutoff = Math.max(argPAirCutoff, getNSBasedAPCutoff());
		}

		for (PredicateVector pvec : pvecs) {
			pvec.cutoffInfreqArgPairs(argPAirCutoff);
			if (shouldCutoffNSBased) {// just retain the first X number of arg-pairs for that predicate
				pvec.cutoffInfreqArgPairsPredBased();
			}
		}

		// cutoff preds

		int predCutoff = ConstantsAgg.minArgPairForPred;
		if (shouldCutoffNSBased) {
			predCutoff = Math.max(predCutoff, getNSBasedPredCutoff());
		}

		List<PredicateVector> pvecsTmp = pvecs;
		pvecs = new ArrayList<PredicateVector>();
		predToIdx = new HashMap<String, Integer>();
		int id = 0;
		for (PredicateVector pvec : pvecsTmp) {
			if (pvec.argIdxes.size() >= predCutoff) {
				pvecs.add(pvec);
				pvec.uniqueId = id;
				predToIdx.put(pvec.predicate, pvec.uniqueId);
				id++;
			}
		}

		if (pvecs.size() <= 1) {
			return;// not interested in graphs with one node!!!
		}

		// cutoffs done

		// if (EntailGraphFactoryAggregator.iterateAllArgPairs) {
		//// addAllMissingLinks();
		// }
		if (EntailGraphFactoryAggregator.anchorBasedScores) {// now, let's add the interesting missing links
			addAnchorMissingLinks();
		}

		setPvecNorms();

		try {

			if (writeInfo) {
				this.graphOp1 = new PrintStream(new File(opFileName + "_rels.txt"));
			}
			if (writeSims) {
				this.graphOp2 = new PrintStream(new File(opFileName + "_sim.txt"));
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		if (writeInfo) {
			graphOp1.println("types: " + types + ", num preds: " + pvecs.size());
		}
		// if (writeSims) {
		// graphOp2.println("types: " + types + ", num preds: " + pvecs.size());
		// }
		System.err.println("types: " + types);
		System.err.println("num preds after cutoff: " + pvecs.size());

		// How much are covered?
		HashSet<String> allPreds = new HashSet<>();
		for (PredicateVector pvec : pvecs) {
			String thisPred = pvec.predicate.substring(0, pvec.predicate.indexOf("#"));
			allPreds.add(thisPred);
		}
		if (!ConstantsAgg.isTyped) {
			int numDSPredsCovered = 0;
			for (String s : EntailGraphFactoryAggregator.dsPreds) {
				if (allPreds.contains(s)) {
					numDSPredsCovered++;
				} else {
					System.err.println("not covered: " + s);
				}
			}
			System.err.println("num preds covered: " + numDSPredsCovered + " out of "
					+ EntailGraphFactoryAggregator.dsPreds.size());
		}

		for (PredicateVector pvec : pvecs) {
			String s = pvec.predicate;
			if (writeInfo) {
				graphOp1.println("predicate: " + s);
			}
			String argPairs = pvec.getArgPairs();
			if (writeInfo) {
				graphOp1.println(argPairs);
			}
		}

		long t0 = System.currentTimeMillis();
		buildInvertedIdx();
		System.err.println("build inv idx time: " + (System.currentTimeMillis() - t0));

		t0 = System.currentTimeMillis();
		setAllPMIs();
		System.err.println("set PMIs time: " + (System.currentTimeMillis() - t0));

		if (ConstantsAgg.computeProbELSims) {
			computeAllDSPredScores();
			// if (EntailGraphFactoryAggregator.allPosLinkPredProbs != null) {
			// writeLinkPredPercentiles();
			// }
		}

		System.err.println("starting to set similar vecs!");

		t0 = System.currentTimeMillis();
		setSimilarVecs();
		System.err.println("set similar vecs: " + (System.currentTimeMillis() - t0));

		for (PredicateVector pvec : pvecs) {
			pvec.clean();
		}

		t0 = System.currentTimeMillis();
		System.err.println("now computing similarities");
		computeSimilarities();
		System.err.println("compute dots: " + (System.currentTimeMillis() - t0));
		if (writeInfo) {
			graphOp1.close();
		}
		// this.clean();
	}

	void computeAllDSPredScores() {
		String[] typess = types.split("#");
		if (typess[0].equals(typess[1])) {
			typess[0] += "_1";
			typess[1] += "_2";
		}
		int idx = 0;

		int numThreads = ConstantsAgg.dsPredNumThreads;

		List<String> currentArgPairs = new ArrayList<>();
		for (String s : argPairs) {
			currentArgPairs.add(s);
		}

		double NTuples = 0;
		for (double x : argPairIdxToOcc.values()) {
			NTuples += x;
		}

		final BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(numThreads);
		ThreadPoolExecutor threadPool = new ThreadPoolExecutor(numThreads, numThreads, 10, TimeUnit.HOURS, queue);
		// to silently discard rejected tasks. :add new
		// ThreadPoolExecutor.DiscardPolicy()

		threadPool.setRejectedExecutionHandler(new RejectedExecutionHandler() {
			@Override
			public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
				// this will block if the queue is full
				try {
					executor.getQueue().put(r);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});

		for (String predPair : EntailGraphFactoryAggregator.dsRawPredPairs) {

			ProbScoreRunner pelRunner = new ProbScoreRunner(this, predPair, idx, typess, currentArgPairs, NTuples);

			threadPool.execute(pelRunner);

			idx++;
		}

		threadPool.shutdown();
		// Wait hopefully all threads are finished. If not, forget about it!
		try {
			threadPool.awaitTermination(200, TimeUnit.HOURS);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
	}

	double computeProbScore(String rel1, String rel2, double preComputedProb, List<String> currentArgPairs,
			double NTuples) {
		double probScore = preComputedProb;
		if (preComputedProb == -1) {

			if (ConstantsAgg.probModel == ProbModel.Cos) {
				probScore = getCosPreds(rel1, rel2);
			} else if (ConstantsAgg.probModel == ProbModel.RandWalk) {
				double ret = 0;
				// System.out.println(EntailGraphFactoryAggregator.predToEntPair);
				if (!RandWalkMatrix.predToEntPair.containsKey(rel1)) {
					return 0;
				}
				List<String> connectedEntPairs = RandWalkMatrix.predToEntPair.get(rel1);
				System.err.println("num connected to rel1: " + rel1 + " " + connectedEntPairs.size());
				for (String entPair : connectedEntPairs) {
					if (RandWalkMatrix.entPairToPred.containsKey(entPair)) {
						double denom1 = 0;
						if (ConstantsAgg.linkPredBasedRandWalk) {
							denom1 = RandWalkMatrix.predToSumNeighs.get(rel1);
						} else {
							try {
								// denom1 = pvecs.get(predToIdx.get(rel1)).norm1;
								denom1 = pvecs.get(predToIdx.get(rel1)).sumPMIs;
							} catch (Exception e) {
								e.printStackTrace();
							}
						}

						double p1 = 0, p2 = 0;
						if (denom1 != 0) {
							if (ConstantsAgg.linkPredBasedRandWalk) {
								p1 = getScore(rel1, entPair) / denom1;
							} else {
								try {
									int argIdx = argPairToIdx.get(entPair);
									PredicateVector pvec = pvecs.get(predToIdx.get(rel1));
									// p1 = pvec.vals.get(pvec.argIdxToArrayIdx.get(argIdx)) / denom1;
									p1 = pvec.PMIs.get(pvec.argIdxToArrayIdx.get(argIdx)) / denom1;
								} catch (Exception e) {
									System.err.println(entPair);
									e.printStackTrace();
								}

							}
						} else {
							System.err.println("denom1 0: " + rel1);
						}
						double denom2 = 0;
						if (ConstantsAgg.linkPredBasedRandWalk) {
							denom2 = RandWalkMatrix.entPairToSumNeighs.get(entPair);
						} else {
							try {
								// denom2 = invertedIdxes.get(argPairToIdx.get(entPair)).norm1;
								denom2 = invertedIdxes.get(argPairToIdx.get(entPair)).sumPMIs;
							} catch (Exception e) {
								// e.printStackTrace();
							}
						}

						if (denom2 != 0) {
							if (ConstantsAgg.linkPredBasedRandWalk) {
								p2 = getScore(rel2, entPair) / denom2;
							} else {
								try {
									int argIdx = argPairToIdx.get(entPair);
									PredicateVector pvec = pvecs.get(predToIdx.get(rel2));
									// p2 = pvec.vals.get(pvec.argIdxToArrayIdx.get(argIdx)) / denom2;
									p2 = pvec.PMIs.get(pvec.argIdxToArrayIdx.get(argIdx)) / denom2;
								} catch (Exception e) {
									// e.printStackTrace();
								}
							}
						}
						ret += p1 * p2;
					}
				}
				return ret;
			}

			else {
				double numerator = 0;
				double denominator = 0;
				int idx = 0;

				for (String argPair : currentArgPairs) {
					// System.out.println(argPair);
					double nArgPair = argPairIdxToOcc.get(idx);
					double pr_ArgPair = nArgPair / NTuples;// TODO: be careful
					// double pr_ArgPair = 1;
					// System.out.println("narg nnz: "+nArgPair+" "+nnz);
					double pr1 = 0;
					double pr2 = 0;
					if (ConstantsAgg.probModel == ProbModel.PL) {
						pr1 = getScore(rel1, argPair);
						pr2 = getScore(rel2, argPair);
					} else {
						if (predToIdx.containsKey(rel1)) {
							PredicateVector pvec1 = pvecs.get(predToIdx.get(rel1));
							if (pvec1.argIdxToArrayIdx.containsKey(idx)) {

								if (ConstantsAgg.probModel == ProbModel.PEL) {
									pr1 = getScore(rel1, argPair);// TODO: be careful
								} else if (ConstantsAgg.probModel == ProbModel.PE) {
									int arrIdx = pvec1.argIdxToArrayIdx.get(idx);
									pr1 = pvec1.vals.get(arrIdx) / nArgPair;

									// TODO: remove below
									if (getScore(rel1, argPair) == 0) {
										// System.out.println("pe setting triple pr to 0: "+ rel1+" "+argPair);
										pr1 = 0;
									}
									// System.out.println("count: "+pvec1.vals.get(arrIdx));
									// System.out.println("pr1: "+pr1);
								}
							}
						}
						if (predToIdx.containsKey(rel2)) {
							PredicateVector pvec2 = pvecs.get(predToIdx.get(rel2));
							if (pvec2.argIdxToArrayIdx.containsKey(idx)) {
								if (ConstantsAgg.probModel == ProbModel.PEL) {
									pr2 = getScore(rel2, argPair);// TODO: be careful
								} else if (ConstantsAgg.probModel == ProbModel.PE) {
									int arrIdx = pvec2.argIdxToArrayIdx.get(idx);
									pr2 = pvec2.vals.get(arrIdx) / nArgPair;

									// TODO: remove below
									if (getScore(rel2, argPair) == 0) {
										// System.out.println("pe setting triple pr to 0: "+ rel2+" "+argPair);
										pr2 = 0;
									}

									// System.out.println("count: "+pvec2.vals.get(arrIdx));
									// System.out.println("pr2: "+pr2);
								}

							}
						}
					}

					// TODO: be careful about numerator!

					// numerator += pr_ArgPair * pr1 * pr2;
					// numerator += pr_ArgPair * pr1 * (1-pr2);

					if (pr2 >= pr1) {
						numerator += pr_ArgPair * pr1;
					}

					denominator += pr1 * pr_ArgPair;
					if (pr1 > 0) {
						// System.out.println("count: " + argPair + " " + argPairIdxToCount.get(idx));
						// System.out.println("pred and arg pair: " + rel1 + " " + rel2 + " " + argPair
						// + " " + pr1 + " " + pr2
						// + " " + pr_ArgPair);
						// if (pr2>0) {
						// System.out.println("both positive");
						// }
					}

					idx++;
				}

				if (denominator == 0) {
					probScore = 0;
				} else {
					probScore = numerator / denominator;
				}
			}
		}

		if (!EntailGraphFactoryAggregator.dsPredToPredToScore.containsKey(rel1)) {
			EntailGraphFactoryAggregator.dsPredToPredToScore.put(rel1, new HashMap<>());
		}

		EntailGraphFactoryAggregator.dsPredToPredToScore.get(rel1).put(rel2, probScore);
		System.out.println(rel1 + " => " + rel2 + " " + probScore);
		return probScore;
	}

	public static double getCosPreds(String rel1, String rel2) {
		String[] ss1 = rel1.split("#");
		String[] ss2 = rel2.split("#");
		String rawPred1 = ss1[0];
		String rawPred2 = ss2[0];

		if (ConstantsAgg.linkPredModel == LinkPredModel.ConvE) {
			if (ss1[1].equals("thing_2")) {
				rawPred1 += "_reverse";
			}
			if (ss2[1].equals("thing_2")) {
				rawPred2 += "_reverse";
			}
		}

		try {
			double[] r1Emb = RandWalkMatrix.relsToEmbed.get(rawPred1);
			double[] r2Emb = RandWalkMatrix.relsToEmbed.get(rawPred2);
			return Math.max(getCos(r1Emb, r2Emb), 0);
		} catch (Exception e) {
			return 0;
		}
	}

	public static double getCos(double[] a, double[] b) {
		double dot = 0;
		for (int i = 0; i < a.length; i++) {
			dot += a[i] * b[i];
		}
		return dot / (getNorm(a) * getNorm(b));
	}

	public static double getNorm(double[] a) {
		double ret = 0;
		for (int i = 0; i < a.length; i++) {
			ret += Math.pow(a[i], 2);
		}
		return Math.sqrt(ret);
	}

	void addAllMissingLinks() {
		System.out.println("adding all arg pairs to all remainig predicates!");

		Set<String> allRemainedArgPairs = new HashSet<>();
		for (PredicateVector pvec : pvecs) {
			for (int i : pvec.argIdxes) {
				allRemainedArgPairs.add(argPairs.get(i));
			}
		}

		System.out.println("num remained args and preds: " + allRemainedArgPairs.size() + " " + pvecs.size());

		for (PredicateVector pvec : pvecs) {
			for (String argPair : allRemainedArgPairs) {
				addBinaryRelation(pvec.predicate, argPair, null, 1, EntailGraphFactoryAggregator.linkPredThreshold, -1);
				// String[] ss = argPair.split("#");
				// String reverseArgPair = ss[1] + "#" + ss[0];
				// addBinaryRelation(pvec.predicate, reverseArgPair, null, 1,
				// EntailGraphFactoryAggregator.linkPredThreshold);
			}
		}
	}

	void addAnchorMissingLinks() {
		System.out.println("adding anchor arg pairs to all remainig predicates!");
		for (PredicateVector pvec : pvecs) {
			for (String argPair : EntailGraphFactoryAggregator.anchorArgPairs) {
				addBinaryRelation(pvec.predicate, argPair, null, 1, EntailGraphFactoryAggregator.linkPredThreshold, -1);
				String[] ss = argPair.split("#");
				String reverseArgPair = ss[1] + "#" + ss[0];
				addBinaryRelation(pvec.predicate, reverseArgPair, null, 1,
						EntailGraphFactoryAggregator.linkPredThreshold, -1);
			}
		}
	}

	// Note that when we cut-off the predicates, some of the NEs that we
	// had in our entTowiki.txt previously, will not appear anymore!
	// Let's just save the ones that appear
	// void cutoffEntToWiki() throws IOException {
	// HashSet<String> allRemainedEnts = new HashSet<String>();
	// for (PredicateVector pvec : pvecs) {
	// for (String s : pvec.ents) {
	// allRemainedEnts.add(s.toLowerCase());
	// }
	// }
	//
	// BufferedReader br = new BufferedReader(new InputStreamReader(
	// new FileInputStream("entToWiki.txt")));
	// PrintStream op = new PrintStream(new File("entToWiki_cutoff.txt"));
	// String line;
	// while ((line = br.readLine()) != null) {
	// String s = line.split("::")[0].toLowerCase();
	// if (allRemainedEnts.contains(s)) {
	// op.println(line);
	// }
	// }
	// op.close();
	// br.close();
	// }

	void setAllPMIs() {

		double allOccurrences1 = 0;
		double allOccurrences2 = 0;
		for (InvertedIdx invIdx : invertedIdxes) {
			allOccurrences1 += invIdx.norm1;
		}

		for (PredicateVector pvec : pvecs) {
			allOccurrences2 += pvec.norm1;
		}

		System.err.println("all occ1: " + allOccurrences1);
		System.err.println("all occ2: " + allOccurrences2);
		this.numTuples = allOccurrences1;

		for (PredicateVector pvec : pvecs) {
			pvec.PMIs = new ArrayList<Double>();
			for (int i = 0; i < pvec.argIdxes.size(); i++) {
				pvec.PMIs.add(-1.0);
			}
			for (int argIdx : pvec.argIdxes) {

				double probPred = pvec.norm1 / this.numTuples;
				InvertedIdx invIdx = invertedIdxes.get(argIdx);

				double probArgPair = invIdx.norm1 / this.numTuples;
				int arrIdx = pvec.argIdxToArrayIdx.get(argIdx);
				double pRel = pvec.vals.get(arrIdx) / numTuples;
				// double PMI = (double) Math.log(pRel / (probPred * probArgPair));
				double PMI = (Math.log(pRel) - Math.log(probPred) - Math.log(probArgPair));
				// System.out.println("pmi: "+PMI);

				pvec.PMIs.set(arrIdx, PMI);
				invIdx.PMIs.set(invIdx.sampleIdxToArrIdx.get(pvec.uniqueId), PMI);
			}
			pvec.setSumPMIs();
		}

		for (InvertedIdx invIdx : invertedIdxes) {
			invIdx.setSumPMIs();
		}

	}

	void computeSimilarities() {
		int ii = 0;
		for (PredicateVector pvec : pvecs) {
			if (ii % 1000 == 0) {
				System.err.println(this.types+" "+ii);
			}
			pvec.computeSimilarities();
			ii++;
		}
	}

	// For each predicate, what are the predicates that we should find
	// similarity to.
	void setSimilarVecs() {
		System.err.println("num of features: " + invertedIdxes.size());
		System.err.println("num samples: " + pvecs.size());
		long numOperations = 0;
		int nnz = 0;
		for (InvertedIdx invIdx : invertedIdxes) {
			int numSamples = invIdx.samplesIdxes.size();
			numOperations += numSamples * (numSamples - 1) / 2;
			nnz += numSamples;
		}

		this.nnz = nnz;
		System.err.println("num operations: " + numOperations);
		System.err.println("nnz: " + nnz);

		// int nnz2 = 0;
		// for (PredicateVector pvec: pvecs){
		// nnz2 += pvec.argIdxes.size();
		// }
		//
		// System.out.println("nnz2: "+nnz2);

		int ii = 0;
		for (InvertedIdx invIdx : invertedIdxes) {
			if (ii % 10000 == 0) {
				System.err.println(ii);
			}
			for (int i = 0; i < invIdx.samplesIdxes.size(); i++) {
				int pvecIdx1 = invIdx.samplesIdxes.get(i);
				double val1 = invIdx.vals.get(i);

				double PMI1 = invIdx.PMIs.get(i);

				String leftInterval1 = null;
				String rightInterval1 = null;

				if (ConstantsAgg.useTimeEx) {
					leftInterval1 = invIdx.maxLeftTimes.get(i);
					rightInterval1 = invIdx.minRightTimes.get(i);
				}

				PredicateVector pvec1 = pvecs.get(pvecIdx1);

				if (val1 == 0) {
					System.err.println("weird val1: " + pvec1.predicate + " ");
				}

				// System.out.println("intervals: "+ pvec1.predicate + " "+
				// leftInterval1+" "+rightInterval1);
				for (int j = i + 1; j < invIdx.samplesIdxes.size(); j++) {
					int pvecIdx2 = invIdx.samplesIdxes.get(j);
					double val2 = invIdx.vals.get(j);
					double PMI2 = invIdx.PMIs.get(j);

					String leftInterval2 = null;
					String rightInterval2 = null;

					if (ConstantsAgg.useTimeEx) {
						leftInterval2 = invIdx.maxLeftTimes.get(j);
						rightInterval2 = invIdx.minRightTimes.get(j);
					}

					PredicateVector pvec2 = pvecs.get(pvecIdx2);

					// This is important if we have unary
					// if (unary && !EntailGraphFactory.acceptablePredPairs
					// .contains(pvec1.predicate + "#" + pvec2.predicate)) {
					// // System.out.println("continued!");
					// continue;
					// }

					if (!pvec1.similarityInfos.containsKey(pvecIdx2)) {
						SimilaritiesInfo simInfo = new SimilaritiesInfo(pvec2.predicate);
						pvec1.similarityInfos.put(pvecIdx2, simInfo);
						EntailGraphFactoryAggregator.allEdgeCounts++;
					}

					int timePreceding1 = 0;
					int timePreceding2 = 0;
					if (ConstantsAgg.useTimeEx) {
						timePreceding1 = (rightInterval1.compareTo(leftInterval2) < 0) ? 1 : 0;
						if (timePreceding1 == 1) {
							// System.out.println("int score: "+rightInterval1+"
							// "+leftInterval2);
						}

						timePreceding2 = (rightInterval2.compareTo(leftInterval1) < 0) ? 1 : 0;

					}

					pvec1.similarityInfos.get(pvecIdx2).addSims(val1 * val2, val1, PMI1, Math.min(val1, val2),
							timePreceding1);

					if (!pvec2.similarityInfos.containsKey(pvecIdx1)) {
						SimilaritiesInfo simInfo = new SimilaritiesInfo(pvec1.predicate);
						pvec2.similarityInfos.put(pvecIdx1, simInfo);
						EntailGraphFactoryAggregator.allEdgeCounts++;
					}

					if (EntailGraphFactoryAggregator.allEdgeCounts % 1000000 == 0) {

						int mb = 1024 * 1024;
						long usedMb = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / mb;

						System.err
								.println("alledges: " + EntailGraphFactoryAggregator.allEdgeCounts + " mb: " + usedMb);
					}

					pvec2.similarityInfos.get(pvecIdx1).addSims(val1 * val2, val2, PMI2, Math.min(val1, val2),
							timePreceding2);
				}
			}
			invertedIdxes.set(ii, null);
			ii++;
		}
		invertedIdxes = null;
	}

	// void buildVectors(String fileName) throws IOException {
	// int lineNumbers = 0;
	// BufferedReader br = null;
	// try {
	// br = new BufferedReader(new InputStreamReader(new FileInputStream(
	// fileName)));
	// } catch (FileNotFoundException e) {
	// e.printStackTrace();
	// }
	// boolean started = false;
	// String line = null;
	// while ((line = br.readLine()) != null) {
	// if (line.startsWith("#line:")) {
	// started = true;// These are the main strs
	// lineNumbers++;
	// // if (lineNumbers>1000000){
	// // break;
	// // }
	// if (lineNumbers % 10000 == 0) {
	// System.err.println(lineNumbers);
	// }
	// continue;
	// }
	// if (line.equals("") || !started || line.contains("e.s.nlp.pipeline")
	// || line.startsWith("#")) {
	// continue;
	// }
	// // System.out.println(line);
	// String pred = null;
	// String arg1 = null;
	// String arg2 = null;
	// try {
	// StringTokenizer st = new StringTokenizer(line);
	// pred = st.nextToken();
	// // System.out.println("pred: "+pred);
	// pred = Util.getPredicateLemma(pred);
	// // System.out.println("pred lemma: "+ pred);
	// arg1 = st.nextToken();
	// arg2 = st.nextToken();
	// } catch (Exception e) {
	// // System.err.println("exception for: "+line);
	// }
	//
	// if (!predToIdx.containsKey(pred)) {
	// PredicateVector pvec = new PredicateVector(pred,
	// predToIdx.size(), argPairs);
	// predToIdx.put(pred, pvec.uniqueId);
	// pvecs.add(pvec);
	// }
	//
	// PredicateVector pvec = pvecs.get(predToIdx.get(pred));
	// String pair = arg1 + "#" + arg2;
	// if (!argPairToIdx.containsKey(pair)) {
	// int idx = argPairToIdx.size();
	// argPairToIdx.put(pair, idx);
	// argPairs.add(pair);
	// }
	//
	// int pairIdx = argPairToIdx.get(pair);
	// pvec.addArgPair(pairIdx);
	// }
	//
	// for (PredicateVector pvec : pvecs) {
	// pvec.setNorm2();
	// pvec.setNorm1();
	// }
	//
	// }

	void setPvecNorms() {
		for (PredicateVector pvec : pvecs) {
			pvec.setNorm2();
			pvec.setNorm1();
		}
	}

	void addBinaryRelation(String pred, String featName, String timeInterval, double count, double threshold,
			double preComputedScore) {
		EntailGraphFactoryAggregator.numAllTuplesPlusReverse++;
		if (!predToIdx.containsKey(pred)) {
			PredicateVector pvec = new PredicateVector(pred, predToIdx.size(), this);
			predToIdx.put(pred, pvec.uniqueId);
			pvecs.add(pvec);
		}

		PredicateVector pvec = pvecs.get(predToIdx.get(pred));
		// pvec.ents.add(arg1);
		// pvec.ents.add(arg2);

		if (!argPairToIdx.containsKey(featName)) {
			int idx = argPairToIdx.size();
			argPairToIdx.put(featName, idx);
			argPairs.add(featName);

			argPairIdxToCount.put(idx, 0.0);
			if (ConstantsAgg.computeProbELSims) {
				argPairIdxToOcc.put(idx, 0.0);
			}
		}

		int pairIdx = argPairToIdx.get(featName);

		// If embBasedScore, then change count to count*score here! (instead of the
		// default count*1)
		if (ConstantsAgg.embBasedScores || EntailGraphFactoryAggregator.anchorBasedScores) {

			if (pvec.argIdxToArrayIdx.containsKey(pairIdx)) {
				return;// TODO: you might want to remove this
			}

			double score = preComputedScore;
			if (score == -1) {

				score = getScore(pred, featName);
				if (score < threshold) {
					return;
				}
			}

			count *= score;
		}

		pvec.addArgPair(pairIdx, timeInterval, count);
	}

	public static double getScore(String pred, String featName) {
		// (visit.1,visit.2)#person#location e1 e2 => s = -|e_1 + r - e_2 |_1
		// (visit.1,visit.2)#person_1#person_2 e1 e2 => ditto
		// (visit.1,visit.2)#person_2#person_1 e1 e2 => swap e1 and e2

		String[] ss = pred.split("#");
		String[] args = featName.split("#");
		String rawPred = ss[0];

		if (ss.length != 3 || args.length != 2) {
			// return 1e-40f;
			return 0;
		}

		if (ss[1].endsWith("_2") && ss[2].endsWith("_1")) {
			// swap args
			String tmp = args[0];
			args[0] = args[1];
			args[1] = tmp;
		}

		if (ConstantsAgg.linkPredModel == LinkPredModel.ConvE) {

			String triple = args[0] + "#" + rawPred + "#" + args[1];
			if (RandWalkMatrix.tripleToScore.containsKey(triple)) {
				double ret = RandWalkMatrix.tripleToScore.get(triple);

				// ret *= ConstantsAgg.sigmoidScaleParameter;
				// ret += ConstantsAgg.sigmoidLocParameter;
				//
				// // sigmoid
				// double s = (double) (1.0 / (1.0 + Math.exp(-ret)));

				// System.out.println(triple + " " + ret);
				EntailGraphFactoryAggregator.allPosLinkPredProbs.add(ret);
				return ret;
			}
			// return 1e-40f;
			return 0;
		}

		// System.out.println(args[0]+" "+ rawPred+" "+args[1]);
		// if (EntailGraphFactoryAggregator.anchorBasedScores) {
		// if (EntailGraphFactoryAggregator.anchorArgPairs.contains(args[0] + "#" +
		// args[1])) {
		// return 1f;
		// } else {
		// return 1e-40f;
		// }
		// } else {// embBased
		double[] e1Emb = RandWalkMatrix.entsToEmbed.get(args[0]);
		double[] e2Emb = RandWalkMatrix.entsToEmbed.get(args[1]);
		double[] rEmb = RandWalkMatrix.relsToEmbed.get(rawPred);

		// System.out.println("embeds null? "+ e1Emb+" "+e2Emb+" "+rEmb);

		double ret = 0;

		// now, compute the score
		try {
			double sum = 0;
			for (int i = 0; i < e1Emb.length; i++) {
				sum += Math.abs(e1Emb[i] + rEmb[i] - e2Emb[i]);
			}

			sum *= -1;

			sum *= ConstantsAgg.sigmoidScaleParameter;
			sum += ConstantsAgg.sigmoidLocParameter;

			// sigmoid
			double s = (double) (1.0 / (1.0 + Math.exp(-sum)));
			ret = s;

		} catch (Exception e) {
		}
		// if (ret != 0) {
		// System.out.println(rawPred + " # " + args[0] + " # " + args[1] + " " + ret);
		// }
		return (double) Math.max(ret, 1e-40);// TODO: you really shouldn't need this!
		// }
	}

	void buildInvertedIdx() {
		invertedIdxes = new ArrayList<InvertedIdx>();
		for (int i = 0; i < argPairs.size(); i++) {
			invertedIdxes.add(new InvertedIdx(i));
		}

		for (int i = 0; i < pvecs.size(); i++) {
			PredicateVector pvec = pvecs.get(i);

			for (int idx : pvec.argIdxes) {
				int arrIdx = pvec.argIdxToArrayIdx.get(idx);
				if (!ConstantsAgg.useTimeEx) {
					invertedIdxes.get(idx).addIdxVal(i, pvec.vals.get(arrIdx), null, null);
				} else {
					invertedIdxes.get(idx).addIdxVal(i, pvec.vals.get(arrIdx), pvec.minRightIntervals.get(arrIdx),
							pvec.maxLeftIntervals.get(arrIdx));
				}
			}
		}

		int maxInvSize = 0;
		for (int i = 0; i < argPairs.size(); i++) {
			InvertedIdx invertedIdx = invertedIdxes.get(i);
			int thisSize = invertedIdx.samplesIdxes.size();
			if (thisSize == 0) {
				continue;
			}
			if (maxInvSize < thisSize) {
				maxInvSize = thisSize;
			}
			if (writeInfo) {
				graphOp1.println("inv idx of " + argPairs.get(i) + " :" + thisSize);
			}
			// System.err.println("inv idx of " + argPairs.get(i) + " :" +
			// thisSize);
			for (int j = 0; j < invertedIdx.samplesIdxes.size(); j++) {
				if (writeInfo) {
					graphOp1.println(pvecs.get(invertedIdx.samplesIdxes.get(j)).predicate);
				}
				// System.err.println(pvecs.get(invertedIdx.samplesIdxes.get(j)).predicate);
			}
		}

		if (writeInfo) {
			graphOp1.println("maxInvSize: " + maxInvSize);
		}

		for (InvertedIdx invIdx : invertedIdxes) {
			invIdx.setNorm1();
		}
	}

	public static void main(String[] args) {
		long t0 = System.currentTimeMillis();
		String fileName;
		if (args.length > 0) {
			fileName = args[0];
		} else {
			fileName = "news_NE.json";
		}
		EntailGraph p = new EntailGraph("none", "op", ConstantsAgg.minArgPairForPred, false);
		System.err.println("time: " + (System.currentTimeMillis() - t0));
	}
}
