package in.sivareddy.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Writer;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.output.NullOutputStream;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public abstract class ProcessStreamInterface {
	private static Gson gson = new Gson();
	private static JsonParser jsonParser = new JsonParser();

	public abstract void processSentence(JsonObject sentence);

	public void processStream(InputStream stream, PrintStream out,
			int nthreads, boolean printOutput) throws IOException,
			InterruptedException {
		int lineNumber = 0;
		Writer writer = new OutputStreamWriter(out, "UTF-8");
		BufferedWriter fout = new BufferedWriter(writer);

		final BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(nthreads);
		ThreadPoolExecutor threadPool = new ThreadPoolExecutor(nthreads,
				nthreads, 600, TimeUnit.SECONDS, queue);
		
		threadPool.setRejectedExecutionHandler(new RejectedExecutionHandler() {
			@Override
			public void rejectedExecution(Runnable r,
					ThreadPoolExecutor executor) {
				// this will block if the queue is full
				try {
					executor.getQueue().put(r);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});

		BufferedReader br = new BufferedReader(new InputStreamReader(stream,
				"UTF8"));
		String line = null;

		line = br.readLine();

		while (line != null) {
			try {
				if (line.startsWith("#") || line.trim().equals("")) {
					line = br.readLine();
					continue;
				}
				lineNumber++;
//				System.err.println(lineNumber);
				if (lineNumber % 1000 == 0) {
					System.err.println(lineNumber);
				}
				line = "{\"sentence\" : \"" + line + "\"}";
//				System.out.println("line:"+ line);
				JsonObject jsonSentence = jsonParser.parse(line)
						.getAsJsonObject();
				Runnable worker = new PipelineRunnable(this, jsonSentence,
						fout, printOutput);
				threadPool.execute(worker);
				
			} catch (Exception e) {
				System.out.println("Could not process line: ");
				System.out.println(line);
//				e.printStackTrace();
			}
			line = br.readLine();

		}
		threadPool.shutdown();

		// Wait until all threads are finished
		while (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
			// pass.
		}
		fout.close();
	}

	public void processList(List<JsonObject> jsonSentences, PrintStream out,
			int nthreads, boolean printOutput) throws IOException,
			InterruptedException {
		Writer writer = null;
		if (out != null) {
			writer = new OutputStreamWriter(out, "UTF-8");
		} else {
			writer = new OutputStreamWriter(new NullOutputStream(), "UTF-8");
		}

		BufferedWriter fout = new BufferedWriter(writer);
		final BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(nthreads);
		ThreadPoolExecutor threadPool = new ThreadPoolExecutor(nthreads,
				nthreads, 600, TimeUnit.SECONDS, queue);

		threadPool.setRejectedExecutionHandler(new RejectedExecutionHandler() {
			@Override
			public void rejectedExecution(Runnable r,
					ThreadPoolExecutor executor) {
				// this will block if the queue is full
				try {
					executor.getQueue().put(r);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});

		for (JsonObject jsonSentence : jsonSentences) {
			Runnable worker = new PipelineRunnable(this, jsonSentence, fout,
					printOutput);
			threadPool.execute(worker);
		}
		threadPool.shutdown();

		// Wait until all threads are finished
		while (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
			// pass.
		}
		fout.close();
	}

	public static class PipelineRunnable implements Runnable {
		private final JsonObject sentence;
		private final ProcessStreamInterface engine;
		private final BufferedWriter fout;
		private final boolean printOutput;

		public PipelineRunnable(ProcessStreamInterface engine,
				JsonObject sentence, BufferedWriter fout, boolean printOutput) {
			this.engine = engine;
			this.sentence = sentence;
			this.fout = fout;
			this.printOutput = printOutput;
		}

		@Override
		public void run() {
			engine.processSentence(sentence);
			if (printOutput) {
				engine.print(gson.toJson(sentence), fout);
			}
		}
	}

	public synchronized void print(String string, BufferedWriter fout) {
		try {
			fout.write(string);
			fout.newLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
