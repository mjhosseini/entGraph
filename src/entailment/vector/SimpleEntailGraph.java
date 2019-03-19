package entailment.vector;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import constants.ConstantsAgg;

//Useful summary out of EntailGraph, mainly for the sake of memory consumption 
public class SimpleEntailGraph {
	Map<String, Integer> predToIdx = new HashMap<>();
	private List<SimplePredicateVector> pvecs;
	boolean writeSims;
	PrintStream graphOp2;
	String types;
	
	public List<SimplePredicateVector> getPvecs() {
		return pvecs;
	}

	public void setPvecs(List<SimplePredicateVector> pvecs) {
		this.pvecs = pvecs;
	}

	public SimpleEntailGraph() {//only because of the extension!

	}
	
	public SimpleEntailGraph(EntailGraph entGraph) {
		this.types = entGraph.types;
		this.predToIdx = entGraph.predToIdx;
		this.pvecs = new ArrayList<>();
		boolean hasAnything = false;
		HashSet<String> allPreds = new HashSet<>();
		
		for (SimplePredicateVector pvec:entGraph.getPvecs()){
			if (pvec.similarityInfos.size()>0){//has a neighbor
				if (this.writeSims){
					System.out.println("has a neighbor: "+pvec.similarityInfos.size()+" "+pvec.predicate+" "+entGraph.types);
				}
				hasAnything = true;
				String thisPred = pvec.predicate.substring(0, pvec.predicate.indexOf("#"));
				allPreds.add(thisPred);
			}
			
			for (SimilaritiesInfo simInfo: pvec.similarityInfos.values()){
				simInfo.basics = null;
			}
			
			SimplePredicateVector spv = new SimplePredicateVector(pvec.predicate, pvec.uniqueId, pvec.similarityInfos);
			pvecs.add(spv);
		}
		this.writeSims = entGraph.writeSims && hasAnything;
		if (writeSims) {
			try {
//				System.out.println("writing: "+entGraph.types+" "+hasAnything);
				this.graphOp2 = new PrintStream(new File(entGraph.opFileName + "_sim.txt"));
//				this.graphOp2 = new PrintStream(new FileOutputStream(entGraph.opFileName + "_sim.txt",true));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		
		if (!ConstantsAgg.isTyped){
			System.err.println("final covered preds");
			int numDSPredsCovered = 0;
			for (String s:EntailGraphFactoryAggregator.dsPreds){
				if (allPreds.contains(s)){
					numDSPredsCovered++;
				}
				else{
					System.err.println("not covered: "+s);
				}
			}
			System.err.println("num preds covered: "+numDSPredsCovered);
		}
	}
}
