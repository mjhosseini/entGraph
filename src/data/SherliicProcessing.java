package data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.maltparser.core.helper.HashSet;

import constants.ConstantsParsing;
import entailment.PredicateArgumentExtractor;
import entailment.Util;
import entailment.vector.EntailGraphFactoryAggregator;
import entailment.vector.EntailGraphFactoryAggregator.TypeScheme;
import uk.co.flamingpenguin.jewel.cli.ArgumentValidationException;

public class SherliicProcessing {
	
	static String root = "data0/ent/";
	
	static void makeLevyFormatDS(String origFile, boolean addEnd, String outFile) throws IOException {
		String line = null;
		BufferedReader br = new BufferedReader(new FileReader(origFile));
		PrintStream op = new PrintStream(new File(root + outFile));
		br.readLine();
		int numEnds = 0;
		Set<String> allTypes = new HashSet<>();
		while ((line = br.readLine()) != null) {
//			System.out.println(line);
			String[] ss = line.split(",");
			String arg1 = ss[15].split("/")[0].trim().replace("\"", "");
			arg1 = Util.deAccent(arg1);
			String arg2 = ss[16].split("/")[0].trim().replace("\"", "");
			arg2 = Util.deAccent(arg2);
//			System.out.println(arg1);
//			System.out.println(arg2);
			String label = ss[17].equals("yes")?"True":"False";
			String rel1 = ss[6].trim();
			String rel2 = ss[10].trim();
			System.out.println("ss[13]: " + ss[13]);
			boolean reverse1 = ss[13].toLowerCase().equals("true");
			boolean reverse2 = ss[14].toLowerCase().equals("true");
			String end1 = ss[8];
			String end2 = ss[12];
			allTypes.add(ss[5].split("\\[")[0]);
			allTypes.add(ss[7].split("\\[")[0]);
			
			
			if (!end1.equals("") || !end2.equals("")) {
				numEnds++;
			}
			
			if (!addEnd) {
				end1 = "";
				end2 = "";
			}
			
			String sent1 = makeSentence(rel1, arg1, arg2, reverse1, end1);
			String sent2 = makeSentence(rel2, arg1, arg2, reverse2, end2);
			
			op.println(sent2 + "\t" + sent1 +"\t" + label);
			
		}
		
		System.out.println("numEnds: " + numEnds);
		System.out.println("allTypes: " + allTypes);
		System.out.println("num all types: " + allTypes.size());
		
		br.close();
	}
	
	
	static void extractRelationsCCG(String fname, boolean longestRel) throws IOException {

		//['Google', 'is taking on', 'Democratic Party'], ['Google', 'is giving', 'Democratic Party something']
		//dev.txt vs dev_orig.txt
		//The above is used for parsing. But since we need the args to be the same, we don't use it in the file 
		BufferedReader br = new BufferedReader(new FileReader(root + fname + "_orig.txt"));

		String line, line2;
		PredicateArgumentExtractor prExt = new PredicateArgumentExtractor(null);
		// PrintWriter op = new PrintWriter(new File(root + fname + "_rels_l8.txt"));
		PrintWriter op = new PrintWriter(new File(root + fname + "_rels_v2.txt"));
		// PrintWriter opLDA = new PrintWriter(new File(root + fname + "_LDA" +
		// DistrTyping.numTopics + "rels_l.txt"));

		while ((line = br.readLine()) != null) {
			line2 = line;
			line = line.replace(",", "");
			// System.err.println(line);
			// System.err.println(line2);
			String[] ss = line.split("\t");


			Map<String, String> tokenToType1 = Util.getSimpleNERTypeSent(ss[0] + ".");
			Map<String, String> tokenToType2 = Util.getSimpleNERTypeSent(ss[1] + ".");
			
			System.out.println("tokenToType1: " + tokenToType1);
			System.out.println("tokenToType2: " + tokenToType2);

			String rel1 = "", rel2 = "";
			String[] ss2 = line2.split("\t");
			String[] rel1Args = new String[] { ss2[0].split(",")[0].trim().toLowerCase(),
					ss2[0].split(",")[2].trim().toLowerCase() };
			String[] rel2Args = new String[] { ss2[1].split(",")[0].trim().toLowerCase(),
					ss2[1].split(",")[2].trim().toLowerCase() };
			try {
				rel1 = prExt.extractPredArgsStrsForceFinding(ss[0] + ".", rel1Args[0], rel1Args[1], longestRel, true);
				rel2 = prExt.extractPredArgsStrsForceFinding(ss[1] + ".", rel2Args[0], rel2Args[1], longestRel, true);
			} catch (ArgumentValidationException | InterruptedException e) {
				e.printStackTrace();
			}
			if (rel1.equals("")) {
				System.out.println("nothing: " + ss[0]);
			}
			if (rel2.equals("")) {
				System.out.println("nothing: " + ss[1]);
			}

			System.out.println(line);
			System.out.println("rel2: " + rel2);
			System.out.println("rel1: " + rel1);

			// String LDArel1 = "", LDArel2 = "";
			// String LDAtypes1 = "";// , LDAtypes2 = "";

			if (!rel1.equals("")) {
				String[] rel1ss = rel1.split(" ");
				String[] lemmas = Util.getPredicateNormalized(rel1ss[0], true);
				rel1ss[0] = lemmas[0];

				// no backup for figerTypes
				String lt1 = Util.linkAndType(rel1ss[1], rel1ss[4].charAt(0) == 'E',
						EntailGraphFactoryAggregator.typeScheme != TypeScheme.FIGER, tokenToType1);
				String lt2 = Util.linkAndType(rel1ss[2], rel1ss[4].charAt(1) == 'E',
						EntailGraphFactoryAggregator.typeScheme != TypeScheme.FIGER, tokenToType1);

				System.out.println(line + " " + lt1 + " " + lt2);

				if (lemmas[1].equals("false")) {
					// LDArel1 = rel1ss[0] + " " + rel1ss[1] + " " + rel1ss[2];// no change. e.g.:
					// (write.1,write.2)
					// dramatist hamlet
					// LDAtypes1 = getLDATypesStr(rel1ss[0], rel1ss[1], rel1ss[2]);
					rel1 = rel1ss[0] + " " + lt1 + " " + lt2;
				} else {
					// LDArel1 = rel1ss[0] + " " + rel1ss[2] + " " + rel1ss[1];
					// LDAtypes1 = getLDATypesStr(rel1ss[0], rel1ss[2], rel1ss[1]);
					rel1 = rel1ss[0] + " " + lt2 + " " + lt1;
				}
			}

			if (!rel2.equals("")) {
				String[] rel2ss = rel2.split(" ");
				String[] lemmas = Util.getPredicateNormalized(rel2ss[0], true);
				rel2ss[0] = lemmas[0];

				String lt1 = Util.linkAndType(rel2ss[1], rel2ss[4].charAt(0) == 'E',
						EntailGraphFactoryAggregator.typeScheme != TypeScheme.FIGER, tokenToType2);
				String lt2 = Util.linkAndType(rel2ss[2], rel2ss[4].charAt(1) == 'E',
						EntailGraphFactoryAggregator.typeScheme != TypeScheme.FIGER, tokenToType2);

				System.out.println(line + " " + lt1 + " " + lt2);

				if (lemmas[1].equals("false")) {
					// LDArel2 = rel2ss[0] + " " + rel2ss[1] + " " + rel2ss[2];// no change. e.g.:
					// (write.1,write.2)
					// dramatist hamlet
					// LDAtypes2 = getLDATypesStr(rel2ss[0], rel2ss[1], rel2ss[2]);
					rel2 = rel2ss[0] + " " + lt1 + " " + lt2;
				} else {
					// LDArel2 = rel2ss[0] + " " + rel2ss[2] + " " + rel2ss[1];
					// LDAtypes2 = getLDATypesStr(rel2ss[0], rel2ss[2], rel2ss[1]);
					rel2 = rel2ss[0] + " " + lt2 + " " + lt1;
				}
			}

			op.println(rel1 + "\t" + rel2 + "\t" + ss[2]);

			// We'll assume that the LDAtypes are inherited only from the LHS of Levy (q)
			// opLDA.println(LDArel1 + "\t" + LDArel2 + "\t" + ss[2] + "\t" + LDAtypes1);
		}
		br.close();
		op.close();

	}
	
	
	static String makeSentence(String rel, String arg1, String arg2, boolean reverse, String end) {
		if (reverse) {
			String tmp = arg1;
			arg1 = arg2;
			arg2 = tmp;
		}
		String ret = arg1 + ", " + rel +", " + arg2 + " " + end;
		ret = ret.trim();
		return ret;
	}
	
	static void makeLevyFormatDSAll() throws IOException {
		makeLevyFormatDS("/Users/javadhosseini/Desktop/D/research/data/sherliic/dev.csv", true, "dev_sherliic_orig_san.txt");
		makeLevyFormatDS("/Users/javadhosseini/Desktop/D/research/data/sherliic/dev.csv", false, "dev_sherliic_san.txt");
		makeLevyFormatDS("/Users/javadhosseini/Desktop/D/research/data/sherliic/test.csv", true, "test_sherliic_orig_san.txt");
		makeLevyFormatDS("/Users/javadhosseini/Desktop/D/research/data/sherliic/test.csv", false, "test_sherliic_san.txt");
	}
	
	static void test() {
		String s = "Cote d’Ivoire";
		String s2 = "Indāpur Jejūri";
		String s3 = "Bjørn";
//		System.out.println(StringUtils.stripAccents(s));
		System.out.println(Util.deAccent(s3));
	}
	
	public static void main(String[] args) throws IOException {
//		test();
		makeLevyFormatDSAll();
//		
		ConstantsParsing.nbestParses = 10;
//		extractRelationsCCG("dev_sherliic", true);
//		extractRelationsCCG("test_sherliic", true);
		
	}
	
}
