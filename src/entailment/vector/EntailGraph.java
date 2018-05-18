package entailment.vector;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import entailment.vector.EntailGraphFactoryAggregator.ProbModel;
import graph.LabelPropagationMNWithinGraph;

public class EntailGraph extends SimpleEntailGraph {

	// static final int numSimilarsToShow = 40;
	static final int numSimilarsToShow = 1000000000;// inf!
	ArrayList<String> argPairs = new ArrayList<String>();// when we cutoff, we don't change this, but some won't be used
	HashMap<String, Integer> argPairToIdx = new HashMap<>();// same as above when we cutoff
	HashMap<Integer, Double> argPairIdxToCount = new HashMap<>();// number of predicates
	HashMap<Integer, Double> argPairIdxToOcc = new HashMap<>();// number of seen times

	private ArrayList<PredicateVector> pvecs = new ArrayList<PredicateVector>();

	double numTuples;
	int nnz;
	PrintStream graphOp1;// This is for writing the predicate vectors
	String opFileName;
	boolean writeInfo = false;
	final int minPredForArgPair;// min num of unique predicates for

	ArrayList<InvertedIdx> invertedIdxes;
	boolean unary = false;

	@Override
	public ArrayList<SimplePredicateVector> getPvecs() {
		ArrayList<SimplePredicateVector> ret = new ArrayList<>();
		for (PredicateVector pvec : pvecs) {
			ret.add(pvec);
		}
		return ret;
	}

	// deletes unnecessary objects!!!
	void clean1() {
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

	void processGraph() {

		// int nnz = 0;
		// for (PredicateVector pvec: pvecs){
		// nnz += pvec.argIdxes.size();
		// }
		//
		// System.out.println("nnz0: "+nnz);

		// System.out.println("process: "+types+" "+writeInfo+" "+writeSims);
		// System.out.println("write info: "+writeInfo+" "+pvecs.size());

		if (pvecs.size() <= 1) {
			return;// not interested in graphs with one node!!!
		}

		// Let's count occurrences per arg-pair

		for (PredicateVector pvec : pvecs) {
			pvec.cutoffInfreqArgPairs();
		}

		// HashMap<Integer, Integer> occToCount = new HashMap<Integer, Integer>();
		// for (PredicateVector pvec : pvecs) {
		// int occ = pvec.argIdxes.size();
		// if (!occToCount.containsKey(occ)) {
		// occToCount.put(occ, 0);
		// }
		// occToCount.replace(occ, occToCount.get(occ) + 1);
		// }

		// int sumCount = 0;
		// for (int i = 0; i < 100; i++) {
		// if (!occToCount.containsKey(i)) {
		// continue;
		// }
		// // System.err.println("num preds with " + i + " args: " +
		// // occToCount.get(i));
		// sumCount += occToCount.get(i);
		// // System.err.println("num preds with more args: " + (pvecs.size() -
		// // sumCount));
		// }

		ArrayList<PredicateVector> pvecsTmp = pvecs;
		pvecs = new ArrayList<PredicateVector>();
		predToIdx = new HashMap<String, Integer>();
		int id = 0;
		for (PredicateVector pvec : pvecsTmp) {
			if (pvec.argIdxes.size() >= EntailGraphFactoryAggregator.minArgPairForPred) {
				pvecs.add(pvec);
				pvec.uniqueId = id;
				predToIdx.put(pvec.predicate, pvec.uniqueId);
				id++;
			}
		}

		if (pvecs.size() <= 1) {
			return;// not interested in graphs with one node!!!
		}

		if (EntailGraphFactoryAggregator.onlyDSPreds) {
			computeAllDSPredScores();
		}

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
		if (!EntailGraphFactoryAggregator.isTyped) {
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

		// try {
		// cutoffEntToWiki();
		// } catch (IOException e) {
		// e.printStackTrace();
		// }

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

		for (PredicateVector pvec : pvecs) {
			pvec.clean();
		}

		t0 = System.currentTimeMillis();
		setSimilarVecs();
		System.err.println("set similar vecs: " + (System.currentTimeMillis() - t0));

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

		int numThreads = EntailGraphFactoryAggregator.dsPredNumThreads;

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
			// if (idx == 400) {
			// break;// TODO: remove this
			// }

			ProbLRunner pelRunner = new ProbLRunner(this, predPair, idx, typess, currentArgPairs, NTuples);

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

	double computeProbL(String rel1, String rel2, double preComputedProb, List<String> currentArgPairs,
			double NTuples) {
		double probL = preComputedProb;
		if (preComputedProb == -1) {
			
			if (EntailGraphFactoryAggregator.probModel == ProbModel.Cos) {
				probL = getCosPreds(rel1, rel2);
				
			}
			else {
				double numerator = 0;
				double denominator = 0;
				int idx = 0;

				for (String argPair : currentArgPairs) {
					// System.out.println(argPair);
					double nArgPair = argPairIdxToOcc.get(idx);
//					double pr_ArgPair = nArgPair / NTuples;//TODO: be careful
					double pr_ArgPair = 1;
					// System.out.println("narg nnz: "+nArgPair+" "+nnz);
					double pr1 = 0;
					double pr2 = 0;
					if (EntailGraphFactoryAggregator.probModel == ProbModel.PL) {
						pr1 = getScore(rel1, argPair);
						pr2 = getScore(rel2, argPair);
					} else {
						if (predToIdx.containsKey(rel1)) {
							PredicateVector pvec1 = pvecs.get(predToIdx.get(rel1));
							if (pvec1.argIdxToArrayIdx.containsKey(idx)) {

								if (EntailGraphFactoryAggregator.probModel == ProbModel.PEL) {
									pr1 = getScore(rel1, argPair);//TODO: be careful
								} else if (EntailGraphFactoryAggregator.probModel == ProbModel.PE) {
									int arrIdx = pvec1.argIdxToArrayIdx.get(idx);
									pr1 = pvec1.vals.get(arrIdx) / nArgPair;
									// System.out.println("count: "+pvec1.vals.get(arrIdx));
									// System.out.println("pr1: "+pr1);
								}

							}
						}
						if (predToIdx.containsKey(rel2)) {
							PredicateVector pvec2 = pvecs.get(predToIdx.get(rel2));
							if (pvec2.argIdxToArrayIdx.containsKey(idx)) {
								if (EntailGraphFactoryAggregator.probModel == ProbModel.PEL) {
									pr2 = getScore(rel2, argPair);//TODO: be careful
								} else if (EntailGraphFactoryAggregator.probModel == ProbModel.PE) {
									int arrIdx = pvec2.argIdxToArrayIdx.get(idx);
									pr2 = pvec2.vals.get(arrIdx) / nArgPair;
									// System.out.println("count: "+pvec2.vals.get(arrIdx));
									// System.out.println("pr2: "+pr2);
								}

							}
						}
					}
					
					numerator += pr_ArgPair * pr1 * pr2;

					denominator += pr1 * pr_ArgPair;
					if (pr1 > 0) {
//						System.out.println("count: " + argPair + " " + argPairIdxToCount.get(idx));
//						System.out.println("pred and arg pair: " + rel1 + " " + rel2 + " " + argPair + " " + pr1 + " " + pr2
//								+ " " + pr_ArgPair);
//						if (pr2>0) {
//							System.out.println("both positive");
//						}
					}

					idx++;
				}
				if (denominator == 0) {
					probL = 0;
				} else {
					probL = numerator / denominator;
				}
			}
			
			
		}

		if (!EntailGraphFactoryAggregator.dsPredToPredToScore.containsKey(rel1)) {
			EntailGraphFactoryAggregator.dsPredToPredToScore.put(rel1, new HashMap<>());
		}

		EntailGraphFactoryAggregator.dsPredToPredToScore.get(rel1).put(rel2, probL);
		System.out.println(rel1 + " => " + rel2 + " " + probL);
		return probL;
	}
	
	double getCosPreds(String rel1, String rel2) {
		String rawPred1 = rel1.split("#")[0];
		String rawPred2 = rel2.split("#")[0];
		
		try {
			double[] r1Emb = EntailGraphFactoryAggregator.relsToEmbed.get(rawPred1);
			double[] r2Emb = EntailGraphFactoryAggregator.relsToEmbed.get(rawPred2);
			return Math.max(getCos(r1Emb, r2Emb),0);
		} catch (Exception e) {
			return 0;
		}
	}
	
	double getCos(double[] a, double[] b) {
		double dot = 0;
		for (int i=0; i<a.length; i++) {
			dot += a[i]*b[i];
		}
		return dot/(getNorm(a)*getNorm(b));
	}
	
	double getNorm(double[] a) {
		double ret = 0;
		for (int i=0; i<a.length; i++) {
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
	}

	void computeSimilarities() {
		int ii = 0;
		for (PredicateVector pvec : pvecs) {
			if (ii % 1000 == 0) {
				System.err.println(ii);
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
				String leftInterval1 = invIdx.maxLeftTimes.get(i);
				String rightInterval1 = invIdx.minRightTimes.get(i);
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
					String leftInterval2 = invIdx.maxLeftTimes.get(j);
					String rightInterval2 = invIdx.minRightTimes.get(j);
					PredicateVector pvec2 = pvecs.get(pvecIdx2);

					if (unary && !EntailGraphFactory.acceptablePredPairs
							.contains(pvec1.predicate + "#" + pvec2.predicate)) {
						// System.out.println("continued!");
						continue;
					}

					if (!pvec1.similarityInfos.containsKey(pvecIdx2)) {
						SimilaritiesInfo simInfo = new SimilaritiesInfo(pvec2.predicate);
						pvec1.similarityInfos.put(pvecIdx2, simInfo);
						EntailGraphFactoryAggregator.allEdgeCounts++;
					}

					int timePreceding1 = 0;
					int timePreceding2 = 0;
					if (EntailGraphFactoryAggregator.useTimeEx) {
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
			argPairIdxToOcc.put(idx, 0.0);
		}

		int pairIdx = argPairToIdx.get(featName);

		// If embBasedScore, then change count to count*score here! (instead of the
		// default count*1)
		if (EntailGraphFactoryAggregator.embBasedScores || EntailGraphFactoryAggregator.anchorBasedScores) {

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

	double getScore(String pred, String featName) {
		// (visit.1,visit.2)#person#location e1 e2 => s = -|e_1 + r - e_2 |_1
		// (visit.1,visit.2)#person_1#person_2 e1 e2 => ditto
		// (visit.1,visit.2)#person_2#person_1 e1 e2 => swap e1 and e2

		String[] ss = pred.split("#");
		String[] args = featName.split("#");

		if (ss.length != 3 || args.length != 2) {
			return 1e-40f;
		}

		String rawPred = ss[0];
		if (ss[1].endsWith("_2") && ss[2].endsWith("_1")) {
			// swap args
			String tmp = args[0];
			args[0] = args[1];
			args[1] = tmp;
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
		double[] e1Emb = EntailGraphFactoryAggregator.entsToEmbed.get(args[0]);
		double[] e2Emb = EntailGraphFactoryAggregator.entsToEmbed.get(args[1]);
		double[] rEmb = EntailGraphFactoryAggregator.relsToEmbed.get(rawPred);

		// System.out.println("embeds null? "+ e1Emb+" "+e2Emb+" "+rEmb);

		double ret = 0;

		// now, compute the score
		try {
			double sum = 0;
			for (int i = 0; i < e1Emb.length; i++) {
				sum += Math.abs(e1Emb[i] + rEmb[i] - e2Emb[i]);
			}

			sum *= -1;

			sum *= EntailGraphFactoryAggregator.sigmoidScaleParameter;
			sum += EntailGraphFactoryAggregator.sigmoidLocParameter;

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
				invertedIdxes.get(idx).addIdxVal(i, pvec.vals.get(arrIdx), pvec.minRightIntervals.get(arrIdx),
						pvec.maxLeftIntervals.get(arrIdx));
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
		EntailGraph p = new EntailGraph("none", "op", EntailGraphFactoryAggregator.minArgPairForPred, false);
		System.err.println("time: " + (System.currentTimeMillis() - t0));
	}

}
