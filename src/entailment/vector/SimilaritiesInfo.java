package entailment.vector;

public class SimilaritiesInfo {
	String predicate;// The neighbor's predicate

	SimilarityInfoBasics basics;

	float cosSim;
	float WeedsProbSim;
	float WeedsPMISim;
	float LinSim;
	float BIncSim;
//	float SRSim;// from Sherlocks's paper
//	float SRBinarySim;
	float timeSim;
	float weedPMIPr;

	// void setSims(float cosSim, float WeedsProbSim, float WeedsPMISim, float
	// LinSim, float BIncSim, float SRSim, float SRBinarySim, float timeSim){
	void setSims(float cosSim, float WeedsProbSim, float WeedsPMISim, float LinSim, float BIncSim, float timeSim, float weedPMIPr) {
		this.cosSim = cosSim;
		this.WeedsProbSim = WeedsProbSim;
		this.WeedsPMISim = WeedsPMISim;
		this.LinSim = LinSim;
		this.BIncSim = BIncSim;
//		this.SRSim = SRSim;
//		this.SRBinarySim = SRBinarySim;
		this.timeSim = timeSim;
		this.weedPMIPr = weedPMIPr;
	}

	public SimilaritiesInfo(String predicate) {
		this.predicate = predicate;
		this.basics = new SimilarityInfoBasics();
	}

	public void addSims(float dot, float freq, float PMI, float minFreq, float timePreceding) {
		this.basics.addSims(dot, freq, PMI, minFreq, timePreceding);
	}

}

class SimilarityInfoBasics {
	float dotProd = 0;
	float sumFreq = 0;
	float sumPMI = 0;
//	int SR = 0;
//	int SRBinary = 0;
	float timePreceding = 0;

	void addSims(float dot, float freq, float PMI, float minFreq, float timePreceding) {
		this.dotProd += dot;
		this.sumFreq += freq;
		if (sumFreq==0){
			System.out.println("weird: "+" "+freq);
		}
		if (PMI > 0) {
			this.sumPMI += PMI;
		}
//		this.SRBinary++;
//		this.SR += minFreq;
		this.timePreceding += timePreceding;

	}
}
