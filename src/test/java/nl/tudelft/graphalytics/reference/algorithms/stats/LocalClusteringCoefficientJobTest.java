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
package nl.tudelft.graphalytics.reference.algorithms.stats;

import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongList;
import nl.tudelft.graphalytics.reference.ValidationGraphParser;
import nl.tudelft.graphalytics.validation.GraphStructure;
import nl.tudelft.graphalytics.validation.algorithms.stats.LocalClusteringCoefficientOutput;
import nl.tudelft.graphalytics.validation.algorithms.stats.LocalClusteringCoefficientValidationTest;;

/**
 * Validation tests for the reference local clustering coefficient calculation implementation.
 *
 * @author Stijn Heldens
 */
public class LocalClusteringCoefficientJobTest extends LocalClusteringCoefficientValidationTest {

	@Override
	public LocalClusteringCoefficientOutput executeDirectedLocalClusteringCoefficient(GraphStructure graph)
			throws Exception {
		return execute(graph, true);
	}

	@Override
	public LocalClusteringCoefficientOutput executeUndirectedLocalClusteringCoefficient(GraphStructure graph)
			throws Exception {
		return execute(graph, false);
	}
	
	private LocalClusteringCoefficientOutput execute(GraphStructure graph, boolean directed) throws Exception {
		Long2ObjectMap<LongList> graphData = ValidationGraphParser.parseValidationGraph(graph);
		Long2DoubleMap output = new LocalClusteringCoefficientJob(graphData, directed).run();
		return new LocalClusteringCoefficientOutput(output);
	}

}
