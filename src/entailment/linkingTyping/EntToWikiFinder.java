package entailment.linkingTyping;

import java.net.URISyntaxException;

import java.util.ArrayList;
import java.util.List;


import it.cnr.isti.hpc.dexter.rest.client.DexterRestClient;
import it.cnr.isti.hpc.dexter.rest.domain.CandidateEntity;
import it.cnr.isti.hpc.dexter.rest.domain.CandidateSpot;
import it.cnr.isti.hpc.dexter.rest.domain.SpottedDocument;

public class EntToWikiFinder implements Runnable {
	

	static ArrayList<String> entToWikiStrs = new ArrayList<String>();
	static DexterRestClient client;
	String spot;
	int count;
	
	public EntToWikiFinder(String spot, int count) {
		this.spot = spot;
		this.count = count;
	}

	static {
		try {
			client = new DexterRestClient(
					"http://localhost:8080/dexter-webapp/api/rest");
			client.setWikinames(true);
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		String mainString = "";
		String wikiName = getWikiNamedEntity(spot);
		mainString += (spot + "::" + count + "::" + wikiName);
		EntToWikiHandler.mainStrs.add(mainString);
	}

	public static String getWikiNamedEntity(String str) {
		try {
			SpottedDocument sd = client.spot(str);
			List<CandidateSpot> spots = sd.getSpots();
			
			//#######Added for test
//			System.out.println(spots);
			System.out.println(str);
			for (CandidateSpot spot:spots){
				List<CandidateEntity> candidates = spot.getCandidates();
				if (candidates.size()>0){
					
//					for (CandidateEntity candEnt: candidates){
//						System.out.println(candEnt);
//						System.out.println(candEnt.getWikiname());
//					}
					
					String wikiEntity = candidates.get(0).getWikiname();
					System.out.println("wiki: "+wikiEntity);
				}
			}
			
			//#######Added for test
			if (spots.size() != 1) {
				// Then we're not sure if it makes sense, let's just forget
				// about it!
				return str;
			}
			CandidateSpot spot = spots.get(0);
			List<CandidateEntity> candidates = spot.getCandidates();
			if (candidates.size() == 0) {
				return str;
			}
			String wikiEntity = candidates.get(0).getWikiname();
			if (wikiEntity == null || wikiEntity.length() == 0) {
				return str;
			}
			return wikiEntity;
		} catch (Exception e) {
			e.printStackTrace();
			return str;
		}
	}

}
