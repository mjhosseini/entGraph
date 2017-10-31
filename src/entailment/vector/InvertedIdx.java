package entailment.vector;


import java.util.ArrayList;

import org.maltparser.core.helper.HashMap;

public class InvertedIdx {
	int uniqueIdx;
	ArrayList<Integer> samplesIdxes;
	ArrayList<Float> vals;
	ArrayList<String> minRightTimes;
	ArrayList<String> maxLeftTimes;
	ArrayList<Float> PMIs;
	HashMap<Integer, Integer> sampleIdxToArrIdx;
	float norm1;
	
	public InvertedIdx(int uniqueIdx) {
		this.uniqueIdx = uniqueIdx;
		this.samplesIdxes = new ArrayList<Integer>();
		this.vals = new ArrayList<Float>();
		this.minRightTimes = new ArrayList<>();
		this.maxLeftTimes = new ArrayList<>();
		this.PMIs = new ArrayList<Float>();
		this.sampleIdxToArrIdx = new HashMap<Integer, Integer>();
	}
	
	void addIdxVal(int idx, float value, String minRightTime, String maxLeftTime){
		sampleIdxToArrIdx.put(idx, samplesIdxes.size());
		samplesIdxes.add(idx);
		vals.add(value);
		minRightTimes.add(minRightTime);
		maxLeftTimes.add(maxLeftTime);
		PMIs.add(-1.0f);
	}
	
	void setNorm1(){
		this.norm1 = 0;
		for (float v:vals){
			this.norm1 += v;
		}
	}
	
}
