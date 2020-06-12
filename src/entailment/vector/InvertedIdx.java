package entailment.vector;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.maltparser.core.helper.HashMap;

import constants.ConstantsAgg;

public class InvertedIdx {
	int uniqueIdx;
	List<Integer> samplesIdxes; // predicates. e.g., 2, 5, 6
	List<Double> vals; // freqs
	List<String> minRightTimes;
	List<String> maxLeftTimes;
	List<Double> PMIs;
	Map<Integer, Integer> sampleIdxToArrIdx; // e.g., 2->0, 5->1, 6->2
	double norm1;
	double sumPMIs;
	
	public InvertedIdx(int uniqueIdx) {
		this.uniqueIdx = uniqueIdx;
		this.samplesIdxes = new ArrayList<Integer>();
		this.vals = new ArrayList<>();
		if (ConstantsAgg.useTimeEx) {
			this.minRightTimes = new ArrayList<>();
			this.maxLeftTimes = new ArrayList<>();
		}
		this.PMIs = new ArrayList<>();
		this.sampleIdxToArrIdx = new HashMap<Integer, Integer>();
	}
	
	void addIdxVal(int idx, double value, String minRightTime, String maxLeftTime){
		sampleIdxToArrIdx.put(idx, samplesIdxes.size());
		samplesIdxes.add(idx);
		vals.add(value);
		if (ConstantsAgg.useTimeEx) {
			minRightTimes.add(minRightTime);
			maxLeftTimes.add(maxLeftTime);
		}
		PMIs.add(-1.0);
	}
	
	void setNorm1(){
		this.norm1 = 0;
		for (double v:vals){
			this.norm1 += v;
		}
	}
	
	void setSumPMIs() {
		this.sumPMIs = 0;
		for (double v:PMIs) {
			if (v>0) {
				this.sumPMIs += v;
			}
		}
	}
	
}
