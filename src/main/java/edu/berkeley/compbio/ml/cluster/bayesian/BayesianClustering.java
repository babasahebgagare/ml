/*
 * Copyright (c) 2001-2008 David Soergel
 * 418 Richmond St., El Cerrito, CA  94530
 * dev@davidsoergel.com
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the author nor the names of any contributors may
 *       be used to endorse or promote products derived from this software
 *       without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


package edu.berkeley.compbio.ml.cluster.bayesian;

import com.davidsoergel.dsutils.GenericFactory;
import com.davidsoergel.dsutils.GenericFactoryException;
import com.davidsoergel.stats.DissimilarityMeasure;
import edu.berkeley.compbio.ml.cluster.AdditiveCentroidCluster;
import edu.berkeley.compbio.ml.cluster.AdditiveClusterable;
import edu.berkeley.compbio.ml.cluster.CentroidCluster;
import edu.berkeley.compbio.ml.cluster.ClusterRuntimeException;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Performs cluster classification with a naive bayesian classifier
 *
 * @author David Tulga
 * @author David Soergel
 * @version $Id$
 */
public class BayesianClustering<T extends AdditiveClusterable<T>> extends NearestNeighborClustering<T>
		//	implements SampleInitializedOnlineClusteringMethod<T>
	{
// ------------------------------ FIELDS ------------------------------

	private static final Logger logger = Logger.getLogger(BayesianClustering.class);


// --------------------------- CONSTRUCTORS ---------------------------

	/**
	 * @param dm                       The distance measure to use
	 * @param unknownDistanceThreshold the minimum probability to accept when adding a point to a cluster
	 */
	public BayesianClustering(DissimilarityMeasure<T> dm, double unknownDistanceThreshold,
	                          Set<String> potentialTrainingBins, Map<String, Set<String>> predictLabelSets,
	                          Set<String> leaveOneOutLabels, Set<String> testLabels, int testThreads)
		{
		super(dm, unknownDistanceThreshold, potentialTrainingBins, predictLabelSets, leaveOneOutLabels, testLabels,
		      testThreads);
		}

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface PrototypeBasedCentroidClusteringMethod ---------------------


	final Map<String, CentroidCluster<T>> theClusterMap = new HashMap<String, CentroidCluster<T>>();

	public void createClusters(final GenericFactory<T> prototypeFactory)
		{
		assert theClusters.isEmpty();

		try
			{
			int i = 0;
			for (String potentialTrainingBin : potentialTrainingBins)
				{
				final T centroid = prototypeFactory.create(potentialTrainingBin);

				final int clusterId = i++;
				CentroidCluster<T> cluster = new AdditiveCentroidCluster<T>(clusterId, centroid);
				theClusters.add(cluster);

				theClusterMap.put(potentialTrainingBin, cluster);
				}
			}
		catch (GenericFactoryException e)
			{
			logger.error("Error", e);
			throw new ClusterRuntimeException(e);
			}
		}

// -------------------------- OTHER METHODS --------------------------

	protected void trainWithKnownTrainingLabels(Iterator<T> trainingIterator)
		{

		int i = 0;

		//		ProgressReportingThreadPoolExecutor execService = new ProgressReportingThreadPoolExecutor();

		// the execService approach caches all the points.  In that the reason for the memory problem?

		while (trainingIterator.hasNext())
			{
			final T point = trainingIterator.next();
			final int clusterId = i++;
			// generate one cluster per exclusive training bin (regardless of the labels we want to predict).
			// the training samples must already be labelled with a bin ID.

			String clusterBinId = point.getWeightedLabels().getDominantKeyInSet(potentialTrainingBins);
			CentroidCluster<T> cluster = theClusterMap.get(clusterBinId);

			if (cluster == null)
				{
				throw new ClusterRuntimeException("The clusters were not all created prior to training");
				}

			// note this updates the cluster labels as well.
			// In particular, the point should already be labelled with a Training Label (not just a bin ID),
			// so that the cluster will know what labels it predicts.
			cluster.add(point);
			}

		theClusters = theClusterMap.values();
		}
	}
