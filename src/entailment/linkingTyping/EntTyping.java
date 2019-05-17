package entailment.linkingTyping;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

public class EntTyping {
	String googleFileName, stanFileName, wikiEntsFileName;
	HashMap<String, Integer> entToCount = new HashMap<String, Integer>();
	final int numAcceptableGoogleTypes = 30;

	EntTyping(String googleFileName, String stanFileName, String wikiEntsFileName) {
		this.googleFileName = googleFileName;
		this.stanFileName = stanFileName;
		this.wikiEntsFileName = wikiEntsFileName;
		try {
			entToCount = loadEntCounts(wikiEntsFileName);
			loadEntTypes();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	static HashMap<String, Integer> loadEntCounts(String fnameEntCounts) throws IOException {
		HashMap<String, Integer> entToCount = new HashMap<>();
		BufferedReader br = new BufferedReader(new FileReader(fnameEntCounts));
		String line;
		while ((line = br.readLine()) != null) {
			String[] toks = line.split("::");
			try {
				entToCount.put(toks[0], Integer.parseInt(toks[1]));
			} catch (Exception e) {
				System.err.println("bad ent count: " + line);
				continue;
			}
		}
		br.close();
		return entToCount;
	}
	
	//The input list is empty and we just fill it out with the ents we see!
	static HashMap<String, String> getEntTypeFromList(String fnameTypeList, int numAcceptableTypes,ArrayList<String> allEntsArr, HashMap<String, Integer> entToCount) throws IOException{
		
		BufferedReader br1 = new BufferedReader(new FileReader(fnameTypeList));
		HashMap<String, Integer> typeCounts = new HashMap<String, Integer>();
		HashMap<String, ArrayList<String>> gtypesAll = new HashMap<String, ArrayList<String>>();
		HashMap<String, String> gtypes = new HashMap<String, String>();
		JsonParser parser = new JsonParser();
		String line;
		
		while ((line = br1.readLine()) != null) {
			if (line.contains("nlp.pipeline")) {
				continue;
			}
			String[] toks = line.split("::");
			if (toks.length != 2) {
				toks = line.split(":");
				line = toks[0] + "::" + toks[1];
			}
//			op.println(line);

			String ent = toks[0];
			allEntsArr.add(ent);
			ArrayList<String> tar = new ArrayList<String>();
			if (toks[1].endsWith("None") || toks[1].endsWith("EXCEPTION") || toks[1].equals("null")) {
				tar.add("None");
			} else {
				try {
					JsonArray jar = parser.parse(toks[1]).getAsJsonArray();
					for (int i = 0; i < jar.size(); i++) {
						String t = jar.get(i).getAsString();
						tar.add(t);
						if (!typeCounts.containsKey(t)) {
							typeCounts.put(t, 0);
						}
						
						int count = 1;
						if (entToCount.containsKey(ent)){
							count = entToCount.get(ent);
//							System.out.println(ent+"::"+count);
						}
						
						
						typeCounts.replace(t, typeCounts.get(t) + count);
					}
				} catch (Exception e) {
					System.err.println("no jar for: " + toks[0]);
				}

			}

			gtypesAll.put(ent, tar);
		}

		typeCounts.put("None", 1);

		br1.close();
//		op.close();

		ArrayList<SimpleSpot> typeSpots = new ArrayList<SimpleSpot>();
		for (String t : typeCounts.keySet()) {
			typeSpots.add(new SimpleSpot(t, typeCounts.get(t)));
		}

		Collections.sort(typeSpots, Collections.reverseOrder());
		System.err.println("occs: ");
		HashMap<String, Integer> typeToIdx = new HashMap<>();
		int i=0;
		for (SimpleSpot ss : typeSpots) {
			System.err.println(ss.spot + " " + ss.count);
			typeToIdx.put(ss.spot, i);
			i++;
			if (i==1000){
				break;
			}
		}
		
		for (String s : gtypesAll.keySet()) {
			ArrayList<String> types = gtypesAll.get(s);
			if (types.size() == 0) {
				System.err.println("no type: " + s);
				types.add("None");
			}
			String mostSpecificType = types.get(0);
			for (String t : types) {
				if (!typeToIdx.containsKey(t)){
					continue;
				}
				if (!typeToIdx.containsKey(mostSpecificType)){
					mostSpecificType = t;
					continue;
				}
				int idx1 = typeToIdx.get(t);
				int idx0 = typeToIdx.get(mostSpecificType);
				if ((typeCounts.get(t) < typeCounts.get(mostSpecificType) && idx1<numAcceptableTypes )
					||	(typeCounts.get(t) > typeCounts.get(mostSpecificType) && idx0>=numAcceptableTypes )) {
					mostSpecificType = t;
				}
			}
			
			//otherwise, it doesn't have any types!
			if (typeToIdx.containsKey(mostSpecificType) && typeToIdx.get(mostSpecificType)<numAcceptableTypes){
				gtypes.put(s, mostSpecificType);
			}
			
		}
		
		return gtypes;
		
	}

	void loadEntTypes() throws IOException {
		
		BufferedReader br2 = new BufferedReader(new FileReader(stanFileName));

		// load google
		String line;
		
		
		HashMap<String, String> stanTypes = new HashMap<String, String>();
		HashMap<String, String> entTypes = new HashMap<String, String>();// This
																			// is
																			// the
																			// final
																			// output
//		PrintStream op = new PrintStream(new File("op1.txt"));
		PrintStream op2 = new PrintStream(new File("op2.txt"));

		ArrayList<String> allEntsArr = new ArrayList<>();

		while ((line = br2.readLine()) != null) {
			if (line.contains("nlp.pipeline")) {
				continue;
			}
			op2.println(line);
			String[] toks = line.split("::");
			stanTypes.put(toks[0], toks[1]);
		}

		br2.close();
		op2.close();
		
		HashMap<String, String> gtypes = getEntTypeFromList(googleFileName, numAcceptableGoogleTypes, allEntsArr,entToCount);

		for (String s : gtypes.keySet()) {
			String gtype = gtypes.get(s);
			String stanType = stanTypes.get(s);
			if (stanType == null) {
				stanType = "None";
			}

			gtype = gtype.toLowerCase();
			stanType = stanType.toLowerCase();

			String thisType = gtype;
			// System.out.println(s+" "+stanType+" "+gtype);
			if (stanType.equals("date") || gtype.endsWith("none")) {
				thisType = stanType;
			}
			// System.out.println(thisType);
			entTypes.put(s, thisType);
		}

		for (String s : allEntsArr) {
			System.out.println(s + "::" + entTypes.get(s));
		}
	}

	public static void main(String[] args) throws IOException {
		
//		if (args.length != 3) {
//			args = new String[] { "wt_google.txt", "wt_stan.txt", "wikiEnts.txt" };
//		}
//		new EntTyping(args[0], args[1], args[2]);
		HashMap<String, Integer> genCounts = loadEntCounts("gens.txt");
		ArrayList<String> allGens = new ArrayList<>();
		HashMap<String, String> genTypeFromList = getEntTypeFromList("genTypes_all.txt", 35, allGens, genCounts);
		for (String s:allGens){
			if (genTypeFromList.containsKey(s)){
				System.out.println(s+"::"+genTypeFromList.get(s));
			}
		}
		
	}
}
