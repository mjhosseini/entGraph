package entailment.vector;

public class SimilaritiesInfo {
	String predicate;// The neighbor's predicate

	SimilarityInfoBasics basics;

	double cosSim;
	double WeedsProbSim;
	double WeedsPMISim;
	double LinSim;
	double BIncSim;
//	double SRSim;// from Sherlocks's paper
//	double SRBinarySim;
	double timeSim;
	double weedPMIPr;
	double probELSim;

	// void setSims(double cosSim, double WeedsProbSim, double WeedsPMISim, double
	// LinSim, double BIncSim, double SRSim, double SRBinarySim, double timeSim){
	void setSims(double cosSim, double WeedsProbSim, double WeedsPMISim, double LinSim, double BIncSim, double timeSim, double weedPMIPr, double probELSim) {
		this.cosSim = cosSim;
		this.WeedsProbSim = WeedsProbSim;
		this.WeedsPMISim = WeedsPMISim;
		this.LinSim = LinSim;
		this.BIncSim = BIncSim;
//		this.SRSim = SRSim;
//		this.SRBinarySim = SRBinarySim;
		this.timeSim = timeSim;
		this.weedPMIPr = weedPMIPr;
		this.probELSim = probELSim;
	}

	public SimilaritiesInfo(String predicate) {
		this.predicate = predicate;
		this.basics = new SimilarityInfoBasics();
	}

	public void addSims(double dot, double freq, double PMI, double minFreq, double timePreceding) {
		this.basics.addSims(dot, freq, PMI, minFreq, timePreceding);
	}

}

class SimilarityInfoBasics {
	double dotProd = 0;
	double sumFreq = 0;
	double sumPMI = 0;
//	int SR = 0;
//	int SRBinary = 0;
	double timePreceding = 0;

	void addSims(double dot, double freq, double PMI, double minFreq, double timePreceding) {
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
