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

import com.hp.hpl.jena.sparql.pfunction.library.container;

import constants.ConstantsAgg;
import constants.ConstantsParsing;
import entailment.PredicateArgumentExtractor;
import entailment.Util;
import entailment.vector.EntailGraphFactoryAggregator;
import entailment.vector.EntailGraphFactoryAggregator.TypeScheme;
import uk.co.flamingpenguin.jewel.cli.ArgumentValidationException;

public class NewsQAProcessing {

	public static void extractRelationsNQAAll() throws ArgumentValidationException, IOException, InterruptedException {

		String root_nQA = "newsqa/";

		// String fname_dev = root_nQA + "newsqa_dev_sents.txt";
		// PrintStream op_dev = new PrintStream(new File(root_nQA +
		// "newsqa_dev_extractions_v2.txt"));
		// PrintStream op_unary_dev = new PrintStream(new File(root_nQA +
		// "newsqa_dev_extractions_unary_v2.txt"));

		// String fname_test = root_nQA + "newsqa_test_sents.txt";
		// PrintStream op_test = new PrintStream(new File(root_nQA +
		// "newsqa_test_extractions_v2.txt"));
		// PrintStream op_unary_test = new PrintStream(new File(root_nQA +
		// "newsqa_test_extractions_unary_v2.txt"));

//		String fname_squad = root_nQA + "squad_validation_sents.txt";
//		PrintStream op_squad = new PrintStream(new File(root_nQA + "squad_extractions.txt"));
//		PrintStream op_squad_unary = new PrintStream(new File(root_nQA + "squad_extractions_unary.txt"));
		
		String fname_squad = root_nQA + "otherqa_dev_sents.txt";
		PrintStream op_squad = new PrintStream(new File(root_nQA + "otherqa_extractions.txt"));
		PrintStream op_squad_unary = new PrintStream(new File(root_nQA + "otherqa_extractions_unary.txt"));

		// extractRelationsNQA(fname_dev, op_dev, op_unary_dev);
		// extractRelationsNQA(fname_test, op_test, op_unary_test);

		extractRelationsNQA(fname_squad, op_squad, op_squad_unary);

	}

	public static void extractRelationsNQA(String fname, PrintStream op, PrintStream op_unary)
			throws IOException, ArgumentValidationException, InterruptedException {

		BufferedReader br = new BufferedReader(new FileReader(fname));
		String line;
		PredicateArgumentExtractor prEx = new PredicateArgumentExtractor("");

		ConstantsAgg.isTyped = true;
		EntailGraphFactoryAggregator.typeScheme = TypeScheme.FIGER;
		ConstantsParsing.snli = true;
		ConstantsParsing.parseQuestions = true;
		ConstantsParsing.writeDebugString = false;

		while ((line = br.readLine()) != null) {
			String opLine = "";
			String opLine_unary = "";
			String[] ss = line.split("\t");

			String id = ss[0];
			for (int i = 1; i < ss.length; i++) {
				String sen = ss[i];
				Map<String, String> tokenToType = Util.getSimpleNERTypeSent(sen);
				String mainRels;
				String unaryRels;
				try {
					String[] exPrss = prEx.extractPredArgsStrs(sen, 0, true, true, null);
					mainRels = exPrss[0];
					unaryRels = exPrss[4];
				} catch (Exception e) {
					mainRels = "";
					unaryRels = "";
				}

				System.out.println(mainRels);
				System.out.println(unaryRels);

				boolean first = true;
				for (String rel1 : mainRels.split("\n")) {

					if (rel1.equals("")) {
						continue;
					}

					rel1 = SNLIProcessing.linkTypeExtraction(rel1, tokenToType);
					// String[] sst = rel1.split(" ");
					// String triple = sst[0] + " " + sst[1] + " " + sst[2];

					if (!first) {
						opLine += "$$";
					}
					opLine += rel1;
					first = false;
				}
				if (i < ss.length - 1) {
					opLine += "\t";
				}

				// now unaries
				first = true;
				for (String rel1 : unaryRels.split("\n")) {
					if (rel1.equals("")) {
						continue;
					}

					rel1 = SNLIProcessing.linkTypeExtraction_unary(rel1, tokenToType);
					// String[] sst = rel1.split(" ");
					// String triple = sst[0] + " " + sst[1] + " " + sst[2];

					if (!first) {
						opLine_unary += "$$";
					}
					opLine_unary += rel1;
					first = false;
				}
				if (i < ss.length - 1) {
					opLine_unary += "\t";
				}
			}

			op.println(opLine);

			op_unary.println(opLine_unary);

			System.out.println("####################################");
		}
		br.close();
	}

	public static void convertNQAExtractionsToRels(String fname, String fname_unary, BufferedWriter op,
			BufferedWriter op_unary) throws IOException {
		String line = null;
		BufferedReader br = new BufferedReader(new FileReader(fname));
		System.out.println("here " + fname);
		while ((line = br.readLine()) != null) {
			// System.out.println(line);
			if (line.endsWith("\t")) {// empty question
				continue;
			}
			String[] liness = line.split("\t");
			System.out.println("len: " + liness.length);
			String[] parts0 = liness[liness.length - 1].split("\\$\\$");
			System.out.println("last part: " + liness[liness.length - 1]);

			for (int i = 0; i < liness.length - 1; i++) {

				String[] parts1 = liness[i].split("\\$\\$");

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
		}

		br.close();
		op.close();
		// now unaries
		BufferedReader br_unary = new BufferedReader(new FileReader(fname_unary));

		while ((line = br_unary.readLine()) != null) {
			// System.out.println(line);
			if (line.endsWith("\t")) {
				continue;
			}
			String[] liness = line.split("\t");

			String[] parts0 = liness[liness.length - 1].split("\\$\\$");

			for (int i = 0; i < liness.length - 1; i++) {
				String[] parts1 = liness[i].split("\\$\\$");
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
		}
		br_unary.close();
		op_unary.close();
	}

	// just change the format to be processable by the python code
	public static void convertNQAExtractionsToRelsAll() throws IOException {
		String root_NQA = "newsqa/";
		// String fname = root_NQA + "newsqa_dev_extractions.txt";
		// String fname_unary = root_NQA + "newsqa_dev_extractions_unary.txt";
		//
		// BufferedWriter op = new BufferedWriter(new FileWriter(root_NQA +
		// "newsqa_dev_rels.txt"));
		// BufferedWriter op_unary = new BufferedWriter(new FileWriter(root_NQA +
		// "newsqa_dev_rels_unary.txt"));

		// String fname = root_NQA + "newsqa_test_extractions.txt";
		// String fname_unary = root_NQA + "newsqa_test_extractions_unary.txt";
		//
		// BufferedWriter op = new BufferedWriter(new FileWriter(root_NQA +
		// "newsqa_test_rels.txt"));
		// BufferedWriter op_unary = new BufferedWriter(new FileWriter(root_NQA +
		// "newsqa_test_rels_unary.txt"));
		//
		// convertNQAExtractionsToRels(fname, fname_unary, op, op_unary);

		String fname = root_NQA + "squad_extractions.txt";
		String fname_unary = root_NQA + "squad_extractions_unary.txt";

		BufferedWriter op = new BufferedWriter(new FileWriter(root_NQA + "squad_rels.txt"));
		BufferedWriter op_unary = new BufferedWriter(new FileWriter(root_NQA + "squad_rels_unary.txt"));

		convertNQAExtractionsToRels(fname, fname_unary, op, op_unary);

	}

	public static void main(String[] args) throws IOException, ArgumentValidationException, InterruptedException {
		extractRelationsNQAAll();
		// convertNQAExtractionsToRelsAll();
	}
}
