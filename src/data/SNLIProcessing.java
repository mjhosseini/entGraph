package data;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import constants.ConstantsAgg;
import constants.ConstantsParsing;
import entailment.PredicateArgumentExtractor;
import entailment.Util;
import entailment.vector.EntailGraphFactoryAggregator;
import entailment.vector.EntailGraphFactoryAggregator.TypeScheme;
import uk.co.flamingpenguin.jewel.cli.ArgumentValidationException;

public class SNLIProcessing {

	public static void extractRelationsSNLIAll() throws ArgumentValidationException, IOException, InterruptedException {

		String root_mnli = "../../python/gfiles/multinli_1.0/";
		// String fname_train_mnli = root_mnli + "multinli_1.0_train.txt";
		String fname_dev_matched = root_mnli + "multinli_1.0_dev_matched.txt";
		String fname_test_matched = root_mnli + "multinli_0.9_test_matched_unlabeled.txt";
		String fname_dev_mismatched = root_mnli + "multinli_1.0_dev_mismatched.txt";
		String fname_test_mismatched = root_mnli + "multinli_0.9_test_mismatched_unlabeled.txt";

		// PrintStream op_train_mnli = new PrintStream(new File(root_mnli +
		// "multinli_extractions_train.txt"));

		PrintStream op_dev_matched = new PrintStream(new File(root_mnli + "multinli_extractions_dev_matched.txt"));
		PrintStream op_test_matched = new PrintStream(new File(root_mnli + "multinli_extractions_test_matched.txt"));
		PrintStream op_dev_mismatched = new PrintStream(
				new File(root_mnli + "multinli_extractions_dev_mismatched.txt"));
		PrintStream op_test_mismatched = new PrintStream(
				new File(root_mnli + "multinli_extractions_test_mismatched.txt"));

		// PrintStream op_train_unary_mnli = new PrintStream(new File(root_mnli +
		// "multinli_extractions_train_unary.txt"));
		PrintStream op_dev_unary_matched = new PrintStream(
				new File(root_mnli + "multinli_extractions_dev_unary_matched.txt"));
		PrintStream op_test_unary_matched = new PrintStream(
				new File(root_mnli + "multinli_extractions_test_unary_matched.txt"));
		PrintStream op_dev_unary_mismatched = new PrintStream(
				new File(root_mnli + "multinli_extractions_dev_unary_mismatched.txt"));
		PrintStream op_test_unary_mismatched = new PrintStream(
				new File(root_mnli + "multinli_extractions_test_unary_mismatched.txt"));

		SNLIProcessing.extractRelationsSNLI(fname_dev_matched, op_dev_matched, op_dev_unary_matched);
		SNLIProcessing.extractRelationsSNLI(fname_test_matched, op_test_matched, op_test_unary_matched);
		SNLIProcessing.extractRelationsSNLI(fname_dev_mismatched, op_dev_mismatched, op_dev_unary_mismatched);
		SNLIProcessing.extractRelationsSNLI(fname_test_mismatched, op_test_mismatched, op_test_unary_mismatched);
		// SNLIProcessing.extractRelationsSNLI(fname_train_mnli, op_train_mnli,
		// op_train_unary_mnli);

		// now, snli
		String root = "../../python/gfiles/snli_1.0/";
		String fname_dev = root + "snli_1.0_dev.txt";
		String fname_train = root + "snli_1.0_train.txt";
		String fname_test = root + "snli_1.0_test.txt";

		PrintStream op_dev = new PrintStream(new File(root + "snli_extractions_dev.txt"));
		PrintStream op_train = new PrintStream(new File(root + "snli_extractions_train.txt"));
		PrintStream op_test = new PrintStream(new File(root + "snli_extractions_test.txt"));

		PrintStream op_dev_unary = new PrintStream(new File(root + "snli_extractions_dev_unary.txt"));
		PrintStream op_train_unary = new PrintStream(new File(root + "snli_extractions_train_unary.txt"));
		PrintStream op_test_unary = new PrintStream(new File(root + "snli_extractions_test_unary.txt"));

		SNLIProcessing.extractRelationsSNLI(fname_dev, op_dev, op_dev_unary);
		SNLIProcessing.extractRelationsSNLI(fname_train, op_train, op_train_unary);
		SNLIProcessing.extractRelationsSNLI(fname_test, op_test, op_test_unary);
	}

	public static void extractRelationsSNLI(String fname, PrintStream op, PrintStream op_unary)
			throws IOException, ArgumentValidationException, InterruptedException {
		BufferedReader br = new BufferedReader(new FileReader(fname));
		String line;
		PredicateArgumentExtractor prEx = new PredicateArgumentExtractor("");
		br.readLine();

		ConstantsAgg.isTyped = true;
		EntailGraphFactoryAggregator.typeScheme = TypeScheme.FIGER;
		ConstantsParsing.snli = true;
		ConstantsParsing.writeDebugString = false;

		while ((line = br.readLine()) != null) {
			String opLine = "";
			String opLine_unary = "";
			String[] ss = line.split("\t");

			String sen1 = ss[5];
			String sen2 = ss[6];
			String label = ss[0];

			List<Map<String, String>> tokenToTypes = new ArrayList<>();
			Map<String, String> tokenToType1 = Util.getSimpleNERTypeSent(sen1);
			Map<String, String> tokenToType2 = Util.getSimpleNERTypeSent(sen2);
			tokenToTypes.add(tokenToType1);
			tokenToTypes.add(tokenToType2);

			List<String> mainRelsList = new ArrayList<>();
			List<String> unaryRelsList = new ArrayList<>();

			System.out.println(label + "##\t" + sen1 + "##\t" + sen2);
			String mainRels;
			String unaryRels;
			try {
				String[] exPrss = prEx.extractPredArgsStrs(sen1, 0, true, true, null);
				mainRels = exPrss[0];
				unaryRels = exPrss[4];
			} catch (Exception e) {
				mainRels = "";
				unaryRels = "";
			}

			mainRelsList.add(mainRels);
			unaryRelsList.add(unaryRels);
			System.out.println(mainRels);
			System.out.println(unaryRels);

			try {
				String[] exPrss = prEx.extractPredArgsStrs(sen2, 0, true, true, null);
				mainRels = exPrss[0];
				unaryRels = exPrss[4];
			} catch (Exception e) {
				mainRels = "";
				unaryRels = "";
			}

			mainRelsList.add(mainRels);
			unaryRelsList.add(unaryRels);
			System.out.println(mainRels);

			boolean firstM = true;
			int i = 0;
			for (String mainRelss : mainRelsList) {
				boolean first = true;
				for (String rel1 : mainRelss.split("\n")) {
					if (rel1.equals("")) {
						continue;
					}

					rel1 = SNLIProcessing.linkTypeExtraction(rel1, tokenToTypes.get(i));
					// String[] sst = rel1.split(" ");
					// String triple = sst[0] + " " + sst[1] + " " + sst[2];

					if (!first) {
						opLine += "$$";
					}
					opLine += rel1;
					first = false;
				}
				if (firstM) {
					opLine += "\t";
				}
				firstM = true;
				i++;
			}

			op.println(opLine);

			// Now unaries
			firstM = true;
			i = 0;
			for (String unaryRelss : unaryRelsList) {
				boolean first = true;
				for (String rel1 : unaryRelss.split("\n")) {
					if (rel1.equals("")) {
						continue;
					}

					rel1 = SNLIProcessing.linkTypeExtraction_unary(rel1, tokenToTypes.get(i));
					// String[] sst = rel1.split(" ");
					// String triple = sst[0] + " " + sst[1] + " " + sst[2];

					if (!first) {
						opLine_unary += "$$";
					}
					opLine_unary += rel1;
					first = false;
				}
				if (firstM) {
					opLine_unary += "\t";
				}
				firstM = true;
				i++;
			}

			op_unary.println(opLine_unary);

			System.out.println("####################################");
		}
		br.close();
	}

	static String linkTypeExtraction(String rel1, Map<String, String> tokenToType1) {

		if (!rel1.equals("")) {
			String[] rel1ss = rel1.split(" ");
			String[] lemmas = Util.getPredicateNormalized(rel1ss[0], true);
			rel1ss[0] = lemmas[0];

			// no backup for figerTypes
			System.out.println("here rel1: " + rel1);
			String lt1 = Util.linkAndType(rel1ss[1], rel1ss[4].charAt(0) == 'E',
					EntailGraphFactoryAggregator.typeScheme != TypeScheme.FIGER, tokenToType1);
			String lt2 = Util.linkAndType(rel1ss[2], rel1ss[4].charAt(1) == 'E',
					EntailGraphFactoryAggregator.typeScheme != TypeScheme.FIGER, tokenToType1);

			if (lemmas[1].equals("false")) {
				rel1 = rel1ss[0] + " " + lt1 + " " + lt2;
			} else {
				rel1 = rel1ss[0] + " " + lt2 + " " + lt1;
			}
		}
		return rel1;
	}

	static String linkTypeExtraction_unary(String rel1, Map<String, String> tokenToType1) {

		if (!rel1.equals("")) {
			String[] rel1ss = rel1.split(" ");
			String lemma = Util.getPredicateNormalized_unary(rel1ss[0], true);
			rel1ss[0] = lemma;

			// no backup for figerTypes
			System.out.println("here rel1_unary: " + rel1);
			String lt1 = Util.linkAndType(rel1ss[1], rel1ss[3].charAt(0) == 'E',
					EntailGraphFactoryAggregator.typeScheme != TypeScheme.FIGER, tokenToType1);

			rel1 = rel1ss[0] + " " + lt1;
		}
		return rel1;
	}

	public static void convertSNLIExtractionsToRels(String fname, String fname_unary, BufferedWriter op,
			BufferedWriter op_unary) throws IOException {
		String line = null;
		BufferedReader br = new BufferedReader(new FileReader(fname));
		System.out.println("here " + fname);
		while ((line = br.readLine()) != null) {
			// System.out.println(line);
			String[] liness = line.split("\t");
			if (liness.length != 2) {
				continue;
			}
			String[] parts0 = liness[0].split("\\$\\$");
			String[] parts1 = liness[1].split("\\$\\$");
			for (String rel1 : parts0) {
				String[] ss1 = rel1.split(" ");
				for (String rel2 : parts1) {
					String[] ss2 = rel2.split(" ");
					String rel2p = rel2;
					if (ss1.length == 3 && ss2.length == 3) {
						rel2p = ss2[0] + " " + ss1[1] + " " + ss1[2];
					}
					op.write(rel2p + "\t" + rel1 + "\t" + "False\n");
					op.write(rel1 + "\t" + rel2p + "\t" + "False\n");
				}
			}
		}
		br.close();

		// now unaries
		BufferedReader br_unary = new BufferedReader(new FileReader(fname_unary));
		while ((line = br_unary.readLine()) != null) {
			// System.out.println(line);
			String[] liness = line.split("\t");
			if (liness.length != 2) {
				continue;
			}
			String[] parts0 = liness[0].split("\\$\\$");
			String[] parts1 = liness[1].split("\\$\\$");
			for (String rel1 : parts0) {
				String[] ss1 = rel1.split(" ");
				for (String rel2 : parts1) {
					String[] ss2 = rel2.split(" ");
					String rel2p = rel2;
					if (ss1.length == 2 && ss2.length == 2) {
						rel2p = ss2[0] + " " + ss1[1];
					}
					op_unary.write(rel2p + "\t" + rel1 + "\t" + "False\n");
					op_unary.write(rel1 + "\t" + rel2p + "\t" + "False\n");
				}
			}
		}
		br_unary.close();
	}

	// just change the format to be processable by the python code
	public static void convertSNLIExtractionsToRelsAll() throws IOException {
		String root_snli = "../../python/gfiles/snli_1.0/";
		String fname_dev = root_snli + "snli_extractions_dev.txt";
		String fname_train = root_snli + "snli_extractions_train.txt";
		String fname_test = root_snli + "snli_extractions_test.txt";
		String fname_dev_unary = root_snli + "snli_extractions_dev_unary.txt";
		String fname_train_unary = root_snli + "snli_extractions_train_unary.txt";
		String fname_test_unary = root_snli + "snli_extractions_test_unary.txt";

		String root_mnli = "../../python/gfiles/multinli_1.0/";

		String fname_train_m = root_mnli + "multinli_extractions_train.txt";
		String fname_dev_matched = root_mnli + "multinli_extractions_dev_matched.txt";
		String fname_test_matched = root_mnli + "multinli_extractions_test_matched.txt";
		String fname_dev_mismatched = root_mnli + "multinli_extractions_dev_mismatched.txt";
		String fname_test_mismatched = root_mnli + "multinli_extractions_test_mismatched.txt";

		String fname_train_unary_m = root_mnli + "multinli_extractions_train_unary.txt";
		String fname_dev_unary_matched = root_mnli + "multinli_extractions_dev_unary_matched.txt";
		String fname_test_unary_matched = root_mnli + "multinli_extractions_test_unary_matched.txt";
		String fname_dev_unary_mismatched = root_mnli + "multinli_extractions_dev_unary_mismatched.txt";
		String fname_test_unary_mismatched = root_mnli + "multinli_extractions_test_unary_mismatched.txt";

		BufferedWriter op = new BufferedWriter(new FileWriter(root_snli + "msnli_rels2.txt"));
		BufferedWriter op_unary = new BufferedWriter(new FileWriter(root_snli + "msnli_rels_unary2.txt"));

		// convertSNLIExtractionsToRels(fname_dev, fname_dev_unary, op, op_unary);
		// convertSNLIExtractionsToRels(fname_train, fname_train_unary, op, op_unary);
		// convertSNLIExtractionsToRels(fname_test, fname_test_unary, op, op_unary);

		convertSNLIExtractionsToRels(fname_train_m, fname_train_unary_m, op, op_unary);

		// convertSNLIExtractionsToRels(fname_dev_matched, fname_dev_unary_matched, op,
		// op_unary);
		// convertSNLIExtractionsToRels(fname_test_matched, fname_test_unary_matched,
		// op, op_unary);
		// convertSNLIExtractionsToRels(fname_dev_mismatched,
		// fname_dev_unary_mismatched, op, op_unary);
		// convertSNLIExtractionsToRels(fname_test_mismatched,
		// fname_test_unary_mismatched, op, op_unary);

	}

	public static void main(String[] args) throws IOException, ArgumentValidationException, InterruptedException {
		ConstantsParsing.snli = true;
		// extractRelationsSNLIAll();
		convertSNLIExtractionsToRelsAll();
	}

}
