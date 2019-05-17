package entailment.linkingTyping;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Properties;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import constants.ConstantsAgg;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import entailment.Util;

public class StanfordNERRunner implements Runnable {

	int threadIdx;
	String fname;
	StanfordCoreNLP stanfordPipeline;

	public StanfordNERRunner(int threadIdx, String fname) {
		this.threadIdx = threadIdx;
		this.fname = fname;
		renewStanfordParser();
	}

	public void renewStanfordParser() {
		Properties props = new Properties();
		props.put("annotators", "tokenize,ssplit,pos,lemma,ner");
		props.setProperty("tokenize.options", "untokenizable=noneKeep");
		stanfordPipeline = new StanfordCoreNLP(props);
	}

	@Override
	public void run() {
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fname), "UTF-8"));

			int lineNumbers = 0;
			JsonParser jsonParser = new JsonParser();

			String line;
			while ((line = br.readLine()) != null) {

				if (lineNumbers > 0 && lineNumbers % 1000000 == 0 && ConstantsAgg.backupToStanNER) {
					Util.renewStanfordParser();
				}

				if (lineNumbers % 100000 == 0 && lineNumbers > 0) {
					System.out.println("NER thread " + threadIdx + ": " + lineNumbers);
					// break;
				}

				lineNumbers++;

				if (line.startsWith("exception for") || line.contains("nlp.pipeline")) {
					continue;
				}

				JsonObject jObj = jsonParser.parse(line).getAsJsonObject();
				int lineId = jObj.get("lineId").getAsInt();
				StanfordNERHandler.maxLineId = Math.max(lineId, StanfordNERHandler.maxLineId);

				if (lineId % StanfordNERHandler.numThreads != threadIdx) {
					continue;
				}

				String newsLine = jObj.get("s").getAsString();

				Map<String, String> tokenToType = Util.getSimpleNERTypeSent(newsLine);
				StanfordNERHandler.lineIdToStanTypes.put(lineId, tokenToType);

			}
			br.close();
		} catch (JsonSyntaxException | IOException e) {
			e.printStackTrace();
		}
	}

}
