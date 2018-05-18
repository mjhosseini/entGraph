package entailment.vector;


import java.util.ArrayList;

import org.maltparser.core.helper.HashMap;

public class InvertedIdx {
	int uniqueIdx;
	ArrayList<Integer> samplesIdxes;
	ArrayList<Double> vals;
	ArrayList<String> minRightTimes;
	ArrayList<String> maxLeftTimes;
	ArrayList<Double> PMIs;
	HashMap<Integer, Integer> sampleIdxToArrIdx;
	double norm1;
	
	public InvertedIdx(int uniqueIdx) {
		this.uniqueIdx = uniqueIdx;
		this.samplesIdxes = new ArrayList<Integer>();
		this.vals = new ArrayList<>();
		this.minRightTimes = new ArrayList<>();
		this.maxLeftTimes = new ArrayList<>();
		this.PMIs = new ArrayList<>();
		this.sampleIdxToArrIdx = new HashMap<Integer, Integer>();
	}
	
	void addIdxVal(int idx, double value, String minRightTime, String maxLeftTime){
		sampleIdxToArrIdx.put(idx, samplesIdxes.size());
		samplesIdxes.add(idx);
		vals.add(value);
		minRightTimes.add(minRightTime);
		maxLeftTimes.add(maxLeftTime);
		PMIs.add(-1.0);
	}
	
	void setNorm1(){
		this.norm1 = 0;
		for (double v:vals){
			this.norm1 += v;
		}
	}
	
}
