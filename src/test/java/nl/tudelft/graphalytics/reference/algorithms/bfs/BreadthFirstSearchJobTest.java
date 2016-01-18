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
package nl.tudelft.graphalytics.reference.algorithms.bfs;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongList;
import nl.tudelft.graphalytics.domain.algorithms.BreadthFirstSearchParameters;
import nl.tudelft.graphalytics.reference.ValidationGraphParser;
import nl.tudelft.graphalytics.validation.GraphStructure;
import nl.tudelft.graphalytics.validation.bfs.BreadthFirstSearchOutput;
import nl.tudelft.graphalytics.validation.bfs.BreadthFirstSearchValidationTest;

import java.util.HashMap;
import java.util.Map;

/**
 * Validation tests for the reference BFS implementation.
 *
 * @author Tim Hegeman
 */
public class BreadthFirstSearchJobTest extends BreadthFirstSearchValidationTest {

	@Override
	public BreadthFirstSearchOutput executeDirectedBreadthFirstSearch(GraphStructure graph,
			BreadthFirstSearchParameters parameters) throws Exception {
		return execute(graph, parameters, true);
	}

	@Override
	public BreadthFirstSearchOutput executeUndirectedBreadthFirstSearch(GraphStructure graph,
			BreadthFirstSearchParameters parameters) throws Exception {
		return execute(graph, parameters, false);
	}
	
	private BreadthFirstSearchOutput execute(GraphStructure graph,
			BreadthFirstSearchParameters parameters, boolean directed) throws Exception {
		Long2ObjectMap<LongList> graphData = ValidationGraphParser.parseValidationGraph(graph);
		Long2LongMap output = new BreadthFirstSearchJob(graphData, directed, parameters).run();
		return new BreadthFirstSearchOutput(output);
	}

}