package data;
//No longer in use!
public class BerantProcessing {
//	static Map<String, String> berToFiger;
//
//	static {
//		try {
//			readBerantFigerTypeMapping("freebase_types/types_ber.map");
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
//
//	static void readBerantFigerTypeMapping(String fname) throws IOException {
//		String line = null;
//		BufferedReader br = new BufferedReader(new FileReader(fname));
//		berToFiger = new HashMap<>();
//		while ((line = br.readLine()) != null) {
//			StringTokenizer st = new StringTokenizer(line);
//			String t1 = st.nextToken();
//			String ft = st.nextToken().substring(1);
//			ft = ft.split("/")[0];
//			berToFiger.put(t1, ft);
//		}
//		br.close();
//	}
//
//	static String getPastParticiple(String v) {
//		XMLLexicon lexicon = new XMLLexicon("data/simplenlg/default-lexicon.xml");
//		System.out.println("cat: " + lexicon.getWord(v).getCategory() + " " + v);
//		WordElement word = lexicon.getWord(v, LexicalCategory.VERB);
//		// System.out.println(word);
//		InflectedWordElement infl = new InflectedWordElement(word);
//		infl.setFeature(Feature.TENSE, Tense.PAST);
//		Realiser realiser = new Realiser(lexicon);
//		String past = realiser.realise(infl).getRealisation();
//		return past;
//	}
//	
//	static Set<String> allPreds = new HashSet<>();
//
//	static String[] getNodeAndSent(String berStr, Map<String,String> lemmaToPlain) {
//		berStr = berStr.substring(1, berStr.length() - 1);
//		String node = berStr;
//		String[] ss = berStr.split("::");
//		for (int i = 0; i < ss.length; i++) {
//			ss[i] = ss[i].replace(", ", "");
//		}
//		
//		if (lemmaToPlain.containsKey(ss[0].trim())) {
//			System.out.println("replacing: "+ss[0]+" by "+lemmaToPlain.get(ss[0].trim()));
//			ss[0] = lemmaToPlain.get(ss[0].trim()).trim();
//		}
//
//		String[] ess = ss[0].split(" ");
//		allPreds.add(ss[0]);
//		System.out.println("ss0: " + ss[0]);
//		
//		
//		
////		if (ess[ess.length - 1].equals("by")) {
////			ess[ess.length - 2] = getPastParticiple(ess[ess.length - 2]);
////			System.out.println("past parti: " + ess[ess.length - 2]);
////		}
////
////		if (ess[0].equals("have")) {
////			ess[1] = getPastParticiple(ess[1]);
////			System.out.println("past parti: " + ess[1]);
////		}
//
//		ss[0] = "";
//		for (String s : ess) {
//			ss[0] += s + " ";
//		}
//		ss[1] = ss[1].trim();
//
//		String sent = ss[1] + ", " + ss[0] + ", " + ss[2];
//		sent = StringUtils.capitalize(sent);
//		return new String[] { node, sent };
//	}
//
//	static String extractTypedRels(String sent, PredicateArgumentExtractor prExt)
//			throws ArgumentValidationException, IOException, InterruptedException {
//		// String[] ss = sent.replace("_1", "").replace("_2", "").split(",");
//		String[] ss = sent.split(",");
//		if (ss.length != 3) {
//			System.err.println(sent);
//			System.exit(0);
//		}
//		for (int i = 0; i < ss.length; i++) {
//			ss[i] = ss[i].trim().toLowerCase();
//		}
//
//		// String mySent = sent.replace(",", "").replace("_1", "").replace("_2", "");
//		String mySent = sent.replace(",", "");
//		String rel1 = prExt.extractPredArgsStrsForceFinding(mySent, ss[0], ss[2], false);
//		if (rel1.equals("")) {
//			System.out.println("nothing found for " + mySent + " " + ss[0] + " " + ss[2]);
//			return "";
//		}
//
//		String[] rel1ss = rel1.split(" ");
//		String[] lemmas = Util.getPredicateLemma(rel1ss[0], true);
//		rel1ss[0] = lemmas[0];
//
//		if (lemmas[1].equals("false")) {
//			rel1 = rel1ss[0] + " " + rel1ss[1] + " " + rel1ss[2];
//		} else {
//			// System.out.println("pred inverse:");
//			rel1 = rel1ss[0] + " " + rel1ss[2] + " " + rel1ss[1];
//		}
//
//		rel1ss = rel1.split(" ");
//		for (int i = 0; i < rel1ss.length; i++) {
//			rel1ss[i] = rel1ss[i].replace("-", "_");
//		}
//		// now, we have (purchase.1,purchase.2) Company::organization
//		// company::organization
//
//		System.out.println("ss2: " + ss[2]);
//
//		System.out.println("rel1ss: " + rel1ss[1] + " " + ss[0]);
//
//		if (!rel1ss[1].equals(ss[1])) {// aligned
//			rel1 = rel1ss[0] + " " + rel1ss[1] + "::" + berToFiger.get(rel1ss[1].replace("_1", "").replace("_2", ""))
//					+ " " + rel1ss[2] + "::" + berToFiger.get(rel1ss[2].replace("_1", "").replace("_2", ""));
//		} else {
//			System.out.println("not aligned");
//			rel1 = rel1ss[0] + " " + rel1ss[2] + "::" + berToFiger.get(rel1ss[2].replace("_1", "").replace("_2", ""))
//					+ " " + rel1ss[1] + "::" + berToFiger.get(rel1ss[1].replace("_1", "").replace("_2", ""));
//		}
//
//		return rel1;
//
//	}
//	
//	static Map<String,String> readLemmaToPlain() throws IOException{
//		BufferedReader br = new BufferedReader(new FileReader("data/Ber_ACL/predLemmaToPlain.txt"));
//		String line = null;
//		
//		Map<String,String> ret = new HashMap<>();
//		
//		while ((line=br.readLine())!=null) {
//			String[] ss = line.split("#");
//			if (ss.length==2) {
//				ret.put(ss[0], ss[1]);
//			}
//		}
//		br.close();
//		return ret;
//	}
//
//	static void convertToLevyFormat(Set<String> allPos, String fname, PrintStream op, PrintStream op2, Map<String,String> lemmaToPlain)
//			throws IOException, ArgumentValidationException, InterruptedException {
//		String line = null;
//		BufferedReader br = new BufferedReader(new FileReader(fname));
//		PredicateArgumentExtractor prExt = new PredicateArgumentExtractor(null);
//
//		while ((line = br.readLine()) != null) {
//
//			//A hack because a very important relation won't parse otherwise
//			line = line.replace("be elect president of", "be elected as president of");
//			line = line.replace("award", "awarded");
//			line = line.replace("be purchase from", "be purchased from");
//			
//			// let's ignore the scores
//			String[] ss = line.split("\t");
//			line = ss[0] + "\t" + ss[1];
//			
//			String[] nodeSent1 = getNodeAndSent(ss[0], lemmaToPlain);
//			String[] nodeSent2 = getNodeAndSent(ss[1], lemmaToPlain);
//
//			// ignore if first entity is _2 (it's just duplicate of its reverse...)
//			// <outnumber::animal_2::animal_1> <be susceptible than::animal_2::animal_1>
//			if (nodeSent1[1].split(" ")[0].contains("_2")) {
//				System.out.println("ignoring line: " + line);
//				continue;
//			}
//
//			boolean label = allPos.contains(line);
//			
//			op.println(
//					nodeSent2[1].toLowerCase() + "\t" + nodeSent1[1].toLowerCase() + "\t" + (label ? "True" : "False"));
//			System.out.println(nodeSent2[1] + "\t" + nodeSent1[1] + "\t" + (label ? "True" : "False"));
//			String rel1 = extractTypedRels(nodeSent1[1], prExt);
//			String rel2 = extractTypedRels(nodeSent2[1], prExt);
//			System.out.println("final: " + rel2 + "\t" + rel1 + "\t" + (label ? "True" : "False"));
//			op2.println(rel2 + "\t" + rel1 + "\t" + (label ? "True" : "False"));
//		}
//
//		br.close();
//	}
//
//	static Set<String> readAllPos() throws IOException {
//		Set<String> ret = new HashSet<>();
//
//		File folder = new File("data/Ber_ACL/pos_graphs");
//		File[] files = folder.listFiles();
//		for (File f : files) {
//			BufferedReader br = new BufferedReader(new FileReader(f.getAbsolutePath()));
//			String line = null;
//			while ((line = br.readLine()) != null) {
//				ret.add(line);
//			}
//		}
//		return ret;
//	}
//	
//	static void splitIntoClasses() throws IOException {
//		BufferedReader br = new BufferedReader(new FileReader("data/Ber_ACL/ber_all_rels.txt"));
//		BufferedReader br2 = new BufferedReader(new FileReader("data/Ber_ACL/ber_all.txt"));
//		
//		String line, line2;
//		String currentTypePair = "";
//		
//		Map<String,PrintStream[]> typePair2Files = new HashMap<>();
//		
//		while ((line=br.readLine())!=null) {
//			line2 = br2.readLine();
//			String t1 = line.split("\t")[0].split(" ")[1].split("::")[1];
//			String t2 = line.split("\t")[0].split(" ")[2].split("::")[1];
//			String typePair1 = t1+"#"+t2;
//			String typePair2 = t2+"#"+t1;
//			PrintStream currentOp = null;
//			PrintStream currentOp2 = null;
//			if (typePair2Files.containsKey(typePair1)) {
//				currentOp = typePair2Files.get(typePair1)[0];
//				currentOp2 = typePair2Files.get(typePair1)[1];
//			}
//			
//			if (typePair2Files.containsKey(typePair2)) {
//				currentOp = typePair2Files.get(typePair2)[0];
//				currentOp2 = typePair2Files.get(typePair2)[1];
//			}
//			
//			if (currentOp==null) {
//				currentTypePair = typePair1;
//				currentOp = new PrintStream(new File("data/Ber_ACL/ber_splits/"+currentTypePair+"_rels.txt"));
//				currentOp2  = new PrintStream(new File("data/Ber_ACL/ber_splits/"+currentTypePair+".txt"));
//				
//				typePair2Files.put(currentTypePair, new PrintStream[] {currentOp,currentOp2});
//				
//			}
//			currentOp.println(line);
//			currentOp2.println(line2);
//		}
//	}
//	
//	static void makeDS() throws ArgumentValidationException, IOException, InterruptedException {
//		String s = extractTypedRels("Leader, be elected as president of, country",new PredicateArgumentExtractor(null));
//		System.out.println(s);
//
//		PrintStream op = new PrintStream(new File("data/Ber_ACL/ber_all.txt"));
//		PrintStream op2 = new PrintStream(new File("data/Ber_ACL/ber_all_rels.txt"));
//
//		Set<String> allPos = readAllPos();
//		Map<String, String> lemmaToPlain = readLemmaToPlain();
//
//		File folder = new File("data/Ber_ACL/local-scores");
//		File[] files = folder.listFiles();
//		for (File f : files) {
//			convertToLevyFormat(allPos, f.getPath(), op, op2, lemmaToPlain);
//		}
//		op.close();
//		op2.close();
//	}
//
//	public static void main(String[] args) throws IOException, ArgumentValidationException, InterruptedException {
////		makeDS();
////		System.out.println("all preds:");
////		for (String s: allPreds) {
////			System.out.println(s);
////		}
//		splitIntoClasses();
//		
//	}

}
