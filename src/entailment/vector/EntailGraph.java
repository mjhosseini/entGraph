package entailment.vector;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class EntailGraph extends SimpleEntailGraph{

	// static final int numSimilarsToShow = 40;
	static final int numSimilarsToShow = 100000000;// inf!
	ArrayList<String> argPairs = new ArrayList<String>();
	HashMap<String, Integer> argPairToIdx = new HashMap<>();
	HashMap<Integer, Float> argPairIdxToCount = new HashMap<>();
	private ArrayList<PredicateVector> pvecs = new ArrayList<PredicateVector>();
	
	int numTuples;
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
		for (PredicateVector pvec: pvecs){
			ret.add(pvec);
		}
		return ret;
	}

	public void setPvecs(ArrayList<SimplePredicateVector> pvecs) { 
		this.pvecs = new ArrayList<>();
		for (SimplePredicateVector pvec: pvecs){
			this.pvecs.add((PredicateVector)pvec); 
		}
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
		
//		int nnz = 0;
//		for (PredicateVector pvec: pvecs){
//			nnz += pvec.argIdxes.size();
//		}
//		
//		System.out.println("nnz0: "+nnz);
		
		// System.out.println("process: "+types+" "+writeInfo+" "+writeSims);
		if (pvecs.size() <= 1) {
			return;// not interested in graphs with one node!!!
		}

		// Let's count occurrences per arg-pair

		for (PredicateVector pvec : pvecs) {
			pvec.cutoffInfreqArgPairs();
		}

		HashMap<Integer, Integer> occToCount = new HashMap<Integer, Integer>();
		for (PredicateVector pvec : pvecs) {
			int occ = pvec.argIdxes.size();
			if (!occToCount.containsKey(occ)) {
				occToCount.put(occ, 0);
			}
			occToCount.replace(occ, occToCount.get(occ) + 1);
		}

		int sumCount = 0;
		for (int i = 0; i < 100; i++) {
			if (!occToCount.containsKey(i)) {
				continue;
			}
			// System.err.println("num preds with " + i + " args: " +
			// occToCount.get(i));
			sumCount += occToCount.get(i);
			// System.err.println("num preds with more args: " + (pvecs.size() -
			// sumCount));
		}

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
		
		setPvecNorms();
		
		try {
			if (writeInfo) {
				this.graphOp1 = new PrintStream(new File(opFileName + ".txt"));
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

		for (PredicateVector pvec: pvecs){
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

		int allOccurrences1 = 0;
		int allOccurrences2 = 0;
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
			pvec.PMIs = new ArrayList<Float>();
			for (int i = 0; i < pvec.argIdxes.size(); i++) {
				pvec.PMIs.add(-1.0f);
			}
			for (int argIdx : pvec.argIdxes) {

				float probPred = pvec.norm1 / this.numTuples;
				InvertedIdx invIdx = invertedIdxes.get(argIdx);

				float probArgPair = invIdx.norm1 / this.numTuples;
				int arrIdx = pvec.argIdxToArrayIdx.get(argIdx);
				float pRel = (float) pvec.vals.get(arrIdx) / numTuples;
				float PMI = (float) Math.log(pRel / (probPred * probArgPair));

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
		
//		int nnz2 = 0;
//		for (PredicateVector pvec: pvecs){
//			nnz2 += pvec.argIdxes.size();
//		}
//		
//		System.out.println("nnz2: "+nnz2);
		
		
		int ii = 0;
		for (InvertedIdx invIdx : invertedIdxes) {
			if (ii % 10000 == 0) {
				System.err.println(ii);
			}
			for (int i = 0; i < invIdx.samplesIdxes.size(); i++) {
				int pvecIdx1 = invIdx.samplesIdxes.get(i);
				float val1 = invIdx.vals.get(i);
				
				float PMI1 = invIdx.PMIs.get(i);
				String leftInterval1 = invIdx.maxLeftTimes.get(i);
				String rightInterval1 = invIdx.minRightTimes.get(i);
				PredicateVector pvec1 = pvecs.get(pvecIdx1);
				
				if (val1==0){
					System.err.println("weird val1: "+pvec1.predicate+" ");
				}
				
				// System.out.println("intervals: "+ pvec1.predicate + " "+
				// leftInterval1+" "+rightInterval1);
				for (int j = i + 1; j < invIdx.samplesIdxes.size(); j++) {
					int pvecIdx2 = invIdx.samplesIdxes.get(j);
					float val2 = invIdx.vals.get(j);
					float PMI2 = invIdx.PMIs.get(j);
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
						
						System.err.println("alledges: " + EntailGraphFactoryAggregator.allEdgeCounts+" mb: "+usedMb);
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

	void addBinaryRelation(String pred, String featName, String timeInterval, float count) {
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

			argPairIdxToCount.put(idx, 0.0f);
		}

		int pairIdx = argPairToIdx.get(featName);
		pvec.addArgPair(pairIdx, timeInterval, count);

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
