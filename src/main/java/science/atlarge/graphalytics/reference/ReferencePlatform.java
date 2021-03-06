/*
 * Copyright 2015 Delft University of Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package science.atlarge.graphalytics.reference;

import java.io.*;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.Map;

import org.apache.commons.io.output.TeeOutputStream;
import science.atlarge.graphalytics.domain.algorithms.*;
import science.atlarge.graphalytics.execution.BenchmarkRunSetup;
import science.atlarge.graphalytics.execution.RunSpecification;
import science.atlarge.graphalytics.execution.RuntimeSetup;
import science.atlarge.graphalytics.domain.graph.FormattedGraph;
import science.atlarge.graphalytics.domain.graph.LoadedGraph;
import science.atlarge.graphalytics.report.result.BenchmarkMetric;
import science.atlarge.graphalytics.report.result.BenchmarkMetrics;
import science.atlarge.graphalytics.execution.Platform;
import science.atlarge.graphalytics.execution.PlatformExecutionException;
import science.atlarge.graphalytics.domain.benchmark.BenchmarkRun;
import science.atlarge.graphalytics.domain.graph.PropertyList;
import science.atlarge.graphalytics.domain.graph.PropertyType;
import science.atlarge.graphalytics.reference.algorithms.bfs.BreadthFirstSearchJob;
import science.atlarge.graphalytics.reference.algorithms.cdlp.CommunityDetectionLPJob;
import science.atlarge.graphalytics.reference.algorithms.lcc.LocalClusteringCoefficientJob;
import science.atlarge.graphalytics.reference.algorithms.pr.PageRankJob;
import science.atlarge.graphalytics.reference.algorithms.sssp.SingleSourceShortestPathJob;
import science.atlarge.graphalytics.reference.algorithms.wcc.WeaklyConnectedComponentsJob;
import science.atlarge.graphalytics.util.graph.PropertyGraph;
import science.atlarge.graphalytics.util.graph.PropertyGraphParser;
import science.atlarge.graphalytics.util.graph.PropertyGraphParser.ValueParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static java.nio.file.Files.readAllBytes;

/**
 * Reference implementation of the Graphalytics benchmark.
 *
 * @author Tim Hegeman
 */
public class ReferencePlatform implements Platform {

	private static final Logger LOG = LogManager.getLogger();
	private static PrintStream sysOut;
	private static PrintStream sysErr;

	@Override
	public void verifySetup() {}

	@Override
	public LoadedGraph loadGraph(FormattedGraph formattedGraph) throws Exception {
		return new LoadedGraph(formattedGraph, formattedGraph.getVertexFilePath(), formattedGraph.getEdgeFilePath());
	}

	@Override
	public void deleteGraph(LoadedGraph loadedGraph) {}

	@Override
	public void prepare(RunSpecification runSpecification) {}

	@Override
	public void startup(RunSpecification runSpecification) {
		BenchmarkRunSetup benchmarkRunSetup = runSpecification.getBenchmarkRunSetup();
		startBenchmarkLogging(benchmarkRunSetup.getLogDir().resolve("platform").resolve("driver.logs"));
	}

	@SuppressWarnings("unchecked")
	@Override
	public void run(RunSpecification runSpecification) throws PlatformExecutionException {

		BenchmarkRun benchmarkRun = runSpecification.getBenchmarkRun();
		BenchmarkRunSetup benchmarkRunSetup = runSpecification.getBenchmarkRunSetup();
		RuntimeSetup runtimeSetup = runSpecification.getRuntimeSetup();

		Algorithm algorithm = benchmarkRun.getAlgorithm();
		Object parameters = benchmarkRun.getAlgorithmParameters();
		Map<Long, ? extends Object> output;

		PropertyGraph graph = null;
		try {
			graph = convertToPropertyGraph(runtimeSetup.getLoadedGraph().getFormattedGraph());
		} catch (Exception e) {
			e.printStackTrace();
		}

		LOG.info("Processing starts at: " + System.currentTimeMillis());
		switch (algorithm) {
			case BFS:
				output = new BreadthFirstSearchJob((PropertyGraph<Void, Void>) graph, (BreadthFirstSearchParameters)parameters).run();
				break;
			case CDLP:
				output = new CommunityDetectionLPJob((PropertyGraph<Void, Void>) graph, (CommunityDetectionLPParameters)parameters).run();
				break;
			case WCC:
				output = new WeaklyConnectedComponentsJob(graph).run();
				break;
			case PR:
				output = new PageRankJob((PropertyGraph<Void, Void>) graph, (PageRankParameters)parameters).run();
				break;
			case LCC:
				output = new LocalClusteringCoefficientJob(graph).run();
				break;
			case SSSP:
				output = new SingleSourceShortestPathJob((PropertyGraph<Void, Double>) graph, (SingleSourceShortestPathsParameters)parameters).run();
				break;
			default:
				throw new PlatformExecutionException("Unsupported algorithm: " + algorithm);
		}

		if (benchmarkRunSetup.isOutputRequired()) {
			try {
				String outputFile = benchmarkRunSetup.getOutputDir().resolve(benchmarkRun.getName()).toAbsolutePath().toString();
				writeOutput(outputFile, output);
			} catch(IOException e) {
				throw new PlatformExecutionException("An error while writing to output file", e);
			}
		}
		LOG.info("Processing ends at: " + System.currentTimeMillis());
	}

	@Override
	public BenchmarkMetrics finalize(RunSpecification runSpecification) {
		stopPlatformLogging();
		BenchmarkRunSetup benchmarkRunSetup = runSpecification.getBenchmarkRunSetup();
		Path path = benchmarkRunSetup.getLogDir().resolve("platform").resolve("driver.logs");
		String logs = null;
		try {
			logs = new String(readAllBytes(path));
		} catch (IOException e) {
			e.printStackTrace();
			throw new IllegalStateException("Can't read file at " + path);
		}

		Long startTime = null;
		Long endTime = null;

		for (String line : logs.split("\n")) {
			try {
				if (line.contains("Processing starts at: ")) {
					String[] lineParts = line.split("\\s+");
					startTime = Long.parseLong(lineParts[lineParts.length - 1]);
				}

				if (line.contains("Processing ends at: ")) {
					String[] lineParts = line.split("\\s+");
					endTime = Long.parseLong(lineParts[lineParts.length - 1]);
				}
			} catch (Exception e) {
				LOG.error(String.format("Cannot parse line: %s", line, e));
			}

		}

		if(startTime != null && endTime != null) {

			BenchmarkMetrics metrics = new BenchmarkMetrics();
			Long procTimeMS =  new Long(endTime - startTime);
			BigDecimal procTimeS = (new BigDecimal(procTimeMS)).divide(new BigDecimal(1000), 3, BigDecimal.ROUND_CEILING);
			metrics.setProcessingTime(new BenchmarkMetric(procTimeS, "s"));

			return metrics;
		} else {
			return new BenchmarkMetrics();
		}
	}

	@Override
	public void terminate(RunSpecification runSpecification) {

	}

	private PropertyGraph convertToPropertyGraph(FormattedGraph formattedGraph) throws Exception {
		LOG.info("Loading graph: " + formattedGraph.getName() + ".");

		ValueParser vertexParser = getValueParser(formattedGraph.getVertexProperties());
		ValueParser edgeParser = getValueParser(formattedGraph.getEdgeProperties());

		PropertyGraph graph = PropertyGraphParser.parsePropertyGraph(
				formattedGraph.getVertexFilePath(),
				formattedGraph.getEdgeFilePath(),
				formattedGraph.isDirected(),
				vertexParser,
				edgeParser);

		LOG.info("Loaded graph: " + formattedGraph.getName() + ".");

		return graph;
	}


	private ValueParser getValueParser(PropertyList props) {
		if (props.size() == 0) {
			return new VoidParser();
		} else if (props.size() == 1 && props.get(0).getType().equals(PropertyType.REAL)) {
			return new DoubleParser();
		} else {
			throw new IllegalArgumentException("failed to find property value parser for properties: " + props);
		}
	}

	private static void startBenchmarkLogging(Path fileName) {
		sysOut = System.out;
		sysErr = System.err;
		try {
			File file = null;
			file = fileName.toFile();
			file.getParentFile().mkdirs();
			file.createNewFile();
			FileOutputStream fos = new FileOutputStream(file);
			TeeOutputStream bothStream =new TeeOutputStream(System.out, fos);
			PrintStream ps = new PrintStream(bothStream);
			System.setOut(ps);
			System.setErr(ps);
		} catch(Exception e) {
			e.printStackTrace();
			throw new IllegalArgumentException("cannot redirect to output file");
		}
	}

	public static void stopPlatformLogging() {
		System.setOut(sysOut);
		System.setErr(sysErr);
	}

	private void writeOutput(String path, Map<Long, ? extends Object> output) throws IOException {
		try (PrintWriter w = new PrintWriter(new FileOutputStream(path))) {
			for (Map.Entry<Long, ? extends Object> entry: output.entrySet()) {
				w.print(entry.getKey());
				w.print(" ");
				w.print(entry.getValue());
				w.println();
			}
		}
	}

	private class VoidParser implements ValueParser<Void> {
		@Override
		public Void parse(String[] tokens) throws IOException {
			return null;
		}
	}

	private class DoubleParser implements ValueParser<Double> {
		@Override
		public Double parse(String[] tokens) throws IOException {
			return Double.parseDouble(tokens[0]);
		}
	}

	@Override
	public String getPlatformName() {
		return "reference";
	}

}
