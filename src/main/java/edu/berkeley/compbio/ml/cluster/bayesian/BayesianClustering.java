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

import com.davidsoergel.dsutils.CollectionIteratorFactory;
import com.davidsoergel.dsutils.GenericFactory;
import com.davidsoergel.dsutils.GenericFactoryException;
import com.davidsoergel.stats.DissimilarityMeasure;
import com.davidsoergel.stats.DistributionException;
import com.davidsoergel.stats.Multinomial;
import com.davidsoergel.stats.ProbabilisticDissimilarityMeasure;
import edu.berkeley.compbio.ml.cluster.AdditiveCentroidCluster;
import edu.berkeley.compbio.ml.cluster.AdditiveClusterable;
import edu.berkeley.compbio.ml.cluster.CentroidCluster;
import edu.berkeley.compbio.ml.cluster.Cluster;
import edu.berkeley.compbio.ml.cluster.ClusterException;
import edu.berkeley.compbio.ml.cluster.ClusterMove;
import edu.berkeley.compbio.ml.cluster.ClusterRuntimeException;
import edu.berkeley.compbio.ml.cluster.NoGoodClusterException;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Performs cluster classification with a naive bayesian classifier
 *
 * @author David Tulga
 * @author David Soergel
 * @version $Id$
 */
public class BayesianClustering<T extends AdditiveClusterable<T>> extends NeighborClustering<T>
	{
	// ------------------------------ FIELDS ------------------------------

	private static final Logger logger = Logger.getLogger(BayesianClustering.class);

	//private T[] centroids;
	//protected DistanceMeasure<T> measure;
	//private double[] priors;


	// --------------------------- CONSTRUCTORS ---------------------------

	/**
	 * Creates a new BayesianClustering with the following parameters
	 *
	 * @param theCentroids     Centroids of the clusters.  Note these will be used as is and modified; clone them first if
	 *                         you need to
	 * @param thePriors        Prior expectations for the clusters
	 * @param dm               The distance measure to use
	 * @param unknownThreshold the minimum probability to accept when adding a point to a cluster
	 */
	/*	public BayesianClustering(T[] theCentroids, double[] thePriors, DistanceMeasure<T> dm, double unknownThreshold)
	   {
	   centroids = theCentroids;
	   measure = dm;
	   priors = thePriors;
	   this.unknownThreshold = unknownThreshold;

	   for (int i = 0; i < centroids.length; i++)
		   {
		   Cluster<T> c = new AdditiveCluster<T>(dm, theCentroids[i]);
		   c.setId(i);

		   theClusters.add(c);
		   }
	   logger.debug("initialized " + centroids.length + " clusters");
	   }*/

	/**
	 * @param dm                       The distance measure to use
	 * @param unknownDistanceThreshold the minimum probability to accept when adding a point to a cluster
	 */
	public BayesianClustering(DissimilarityMeasure<T> dm, double unknownDistanceThreshold, boolean leaveOneOut)
		{
		super(dm, unknownDistanceThreshold, leaveOneOut);
		}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public void initializeWithRealData(Iterator<T> trainingIterator, int initSamples,
	                                   GenericFactory<T> prototypeFactory)
			throws GenericFactoryException, ClusterException
		{
		Map<String, CentroidCluster<T>> theClusterMap = new HashMap<String, CentroidCluster<T>>();

		//** The reason this stuff is here, rather than in train(), is that train() expects that the clusters are already defined.
		// but because of the way labelling works now, we have to consume the entire test iterator in order to know what the clusters should be.


		Multinomial<CentroidCluster> priorsMult = new Multinomial<CentroidCluster>();
		try
			{
			// consume the entire iterator, ignoring initsamples
			int i = 0;
			int sampleCount = 0;
			while (trainingIterator.hasNext())
				{
				if (sampleCount % 1000 == 0)
					{
					logger.info("Processed " + sampleCount + " training samples.");
					}
				sampleCount++;

				T point = trainingIterator.next();

				// generate one cluster per exclusive label.

				String clusterLabel = point.getWeightedLabels().getDominantKeyInSet(mutuallyExclusiveLabels);
				CentroidCluster<T> cluster = theClusterMap.get(clusterLabel);

				if (cluster == null)
					{
					final T centroid = prototypeFactory.create(point.getId());
					//	.create(point.getSourceId());  //** include the source id to facilitate leave-one-out testing later
					//** no, that makes no sense, a cluster may arise from multiple sources

					cluster = new AdditiveCentroidCluster<T>(i++, centroid);//measure
					//cluster.setId(i++);
					theClusterMap.put(clusterLabel, cluster);

					//** for now we make a uniform prior
					priorsMult.put(cluster, 1);
					}
				cluster.add(point);  // note this updates the cluster labels as well
				/*		if(cluster.getLabelCounts().uniqueSet().size() != 1)
				{
				throw new Error();
				}*/
				}


			logger.info("Done processing " + sampleCount + " training samples.");

			priorsMult.normalize();
			priors = priorsMult.getValueMap();
			}
		catch (DistributionException e)
			{
			throw new Error(e);
			}
		theClusters = theClusterMap.values();
		}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public void train(CollectionIteratorFactory<T> trainingCollectionIteratorFactory, int iterations)
		{
		// do nothing

		// after that, normalize the label probabilities
		for (Cluster c : theClusters)
			{
			c.updateDerivedWeightedLabelsFromLocal();
			}
		}

	// -------------------------- OTHER METHODS --------------------------


	/**
	 * {@inheritDoc}
	 */
	@Override
	public ClusterMove bestClusterMove(T p) throws NoGoodClusterException
		{
		ClusterMove result = new ClusterMove();
		//int i;
		result.secondBestDistance = Double.MAX_VALUE;
		result.bestDistance = Double.MAX_VALUE;
		//Cluster<T> best = null;
		//double temp = -1;
		//int j = -1;

		String disallowedLabel = p.getWeightedLabels().getDominantKeyInSet(mutuallyExclusiveLabels);

		for (CentroidCluster<T> cluster : theClusters)
			{
			double distance;
			if (leaveOneOut && disallowedLabel
					.equals(cluster.getWeightedLabels().getDominantKeyInSet(mutuallyExclusiveLabels)))
				{
				// ignore this cluster
				}
			else
				{
				// ** careful: how to deal with priors depends on the distance measure.
				// if it's probability, multiply; if log probability, add; for other distance types, who knows?
				if (measure instanceof ProbabilisticDissimilarityMeasure)
					{
					distance = ((ProbabilisticDissimilarityMeasure) measure)
							.distanceFromTo(p, cluster.getCentroid(), priors.get(cluster));
					}
				else
					{
					distance = measure.distanceFromTo(p, cluster.getCentroid());
					}

				if (distance <= result.bestDistance)
					{
					result.secondBestDistance = result.bestDistance;
					result.bestDistance = distance;
					result.bestCluster = cluster;
					//j = i;
					}
				else if (distance <= result.secondBestDistance)
					{
					result.secondBestDistance = distance;
					}
				}
			}

		if (result.bestCluster == null)
			{
			throw new ClusterRuntimeException(
					"None of the " + theClusters.size() + " clusters matched: " + p); // + ", last distance = " + temp);
			}
		if (result.bestDistance > unknownDistanceThreshold)
			{
			throw new NoGoodClusterException(
					"Best distance " + result.bestDistance + " > threshold " + unknownDistanceThreshold);
			}
		return result;
		}
	}
