package edu.berkeley.compbio.ml.cluster.bayesian;

import com.davidsoergel.dsutils.collections.WeightedSet;
import com.davidsoergel.stats.DissimilarityMeasure;
import com.davidsoergel.stats.DistributionException;
import com.davidsoergel.stats.Multinomial;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.TreeMultimap;
import edu.berkeley.compbio.ml.cluster.AdditiveClusterable;
import edu.berkeley.compbio.ml.cluster.CentroidCluster;
import edu.berkeley.compbio.ml.cluster.Cluster;
import edu.berkeley.compbio.ml.cluster.ClusterMove;
import edu.berkeley.compbio.ml.cluster.ClusterRuntimeException;
import edu.berkeley.compbio.ml.cluster.ClusteringTestResults;
import edu.berkeley.compbio.ml.cluster.NoGoodClusterException;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class TacoaClustering<T extends AdditiveClusterable<T>> extends MultiNeighborClustering<T>
	{
// ------------------------------ FIELDS ------------------------------

	private static final Logger logger = Logger.getLogger(TacoaClustering.class);

	private double bestScoreRatioThreshold;


// --------------------------- CONSTRUCTORS ---------------------------

	/**
	 * @param dm The distance measure to use
	 */


	public TacoaClustering(DissimilarityMeasure<T> dm, Set<String> potentialTrainingBins,
	                       Map<String, Set<String>> predictLabelSets, Set<String> leaveOneOutLabels,
	                       Set<String> testLabels, int maxNeighbors, double bestScoreRatioThreshold)
		{
		super(dm, Double.POSITIVE_INFINITY, potentialTrainingBins, predictLabelSets, leaveOneOutLabels, testLabels,
		      maxNeighbors);
		this.bestScoreRatioThreshold = bestScoreRatioThreshold;
		}

// -------------------------- OTHER METHODS --------------------------


/*	protected Map<String, Set<String>> findPopulatedPredictLabelSets(ClusteringTestResults tr) throws DistributionException
		{
		Multiset<String> populatedTrainingLabels = new HashMultiset<String>();

		if (predictLabelSets.size() > 1)
			{
			throw new ClusterRuntimeException(
					"TacoaClustering can't yet handle more than one exclusive label set at a time: " + predictLabelSets
							.keySet());
			}

		Set<String> predictLabels = predictLabelSets.values().iterator().next();

		for (CentroidCluster<T> theCluster : theClusters)
			{
			final String label = theCluster.getDerivedLabelProbabilities().getDominantKeyInSet(predictLabels);
			populatedTrainingLabels.add(label);
			tr.incrementTotalTrainingMass(theCluster.getWeightedLabels().getItemCount());
			}

		// ** we're going to hack the prior probabilities using the number of clusters per label
		// TacoaDistanceMeasure takes the prior to be per label, not per cluster
		// so, the "distance" between a sample and a cluster depends on the label set we're trying to predict
		// this is why we can deal with only one label set at a time

		clusterPriors = new HashMap<CentroidCluster<T>, Double>();
		Multinomial<String> labelPriors = new Multinomial<String>(populatedTrainingLabels);
		for (CentroidCluster<T> theCluster : theClusters)
			{
			final String label =
					theCluster.getDerivedLabelProbabilities().getDominantKeyInSet(predictLabels); // PERF redundant
			clusterPriors.put(theCluster, labelPriors.get(label));
			}
		Map<String, Set<String>> result = new HashMap<String, Set<String>>();
		result.put(predictLabelSets.keySet().iterator().next(), populatedTrainingLabels.elementSet());
		return result;
		}
*/

	/**
	 * Hack the prior probabilities using the number of clusters per training label.  TacoaDistanceMeasure takes the prior
	 * to be per label, not per cluster.   So, the "distance" between a sample and a cluster depends on how many clusters
	 * share the same training label.
	 */
	protected void preparePriors() //throws DistributionException
		{
		//normalizeClusterLabelProbabilities();
		try
			{
			Multiset<String> populatedTrainingLabels = new HashMultiset<String>();
			//int clustersWithTrainingLabel = 0;
			for (CentroidCluster<T> theCluster : theClusters)
				{
				try
					{
					// note this also insures that every cluster has a training label, otherwise it throws NoSuchElementException
					String label = theCluster.getWeightedLabels().getDominantKeyInSet(potentialTrainingBins);
					// could use theCluster.getDerivedLabelProbabilities() there except they're not normalized yet, and there's no need

					populatedTrainingLabels.add(label);
					//clustersWithTrainingLabel++;
					}
				catch (NoSuchElementException e)
					{
					logger.warn("Cluster has no training label: " + theCluster);
					}
				}

			logger.info("" + populatedTrainingLabels.size() + " of " + theClusters.size()
			            + " clusters have a training label; " + populatedTrainingLabels.entrySet().size()
			            + " labels were trained");


			clusterPriors = new HashMap<Cluster<T>, Double>();
			Multinomial<String> labelPriors = new Multinomial<String>(populatedTrainingLabels);
			for (CentroidCluster<T> theCluster : theClusters)
				{
				final String label =
						theCluster.getWeightedLabels().getDominantKeyInSet(potentialTrainingBins); // PERF redundant
				clusterPriors.put(theCluster, labelPriors.get(label));
				}
			}
		catch (DistributionException e)
			{
			logger.error("Error", e);
			throw new ClusterRuntimeException(e);
			}
		}


/*	protected void testOneSample(DissimilarityMeasure<String> intraLabelDistances, ClusteringTestResults tr,
	                             final Map<String, Set<String>> populatedPredictLabelSets, T frag)
		{
		WeightedSet<String> predictedLabelWeights = predictLabelWeights(tr, frag);
		testAgainstPredictionLabels(intraLabelDistances, tr, populatedPredictLabelSets, frag, predictedLabelWeights);
		}
*/

	/**
	 * allow an overriding clustering method to tweak the distances, set vote weights, etc.
	 *
	 * @param cluster
	 * @param distance
	 * @return
	 */
	protected ClusterMove<T, CentroidCluster<T>> makeClusterMove(CentroidCluster<T> cluster, double distance)
		{
		ClusterMove<T, CentroidCluster<T>> cm = new ClusterMove<T, CentroidCluster<T>>();
		cm.bestCluster = cluster;
		cm.voteWeight = distance;

		// ** hack: monotonic positive inversion to a distance-like metric (smaller better)
		cm.bestDistance = 1.0 / distance;
		return cm;
		}

	protected WeightedSet<String> predictLabelWeights(final ClusteringTestResults tr,
	                                                  final T frag) //, Set<String> populatedTrainingLabels)
		{
		//double secondToBestDistanceRatio = 0;

		//double bestDistance;
		//double bestVoteProportion;
		double secondToBestVoteRatio = 0;

		double voteProportion = 0;
		double bestVotes = 0;

		WeightedSet<String> labelWeights = null;
		//VotingResults votingResults = null;

		try
			{
			// make the prediction
			TreeMultimap<Double, ClusterMove<T, CentroidCluster<T>>> moves = scoredClusterMoves(frag);

			// consider up to maxNeighbors neighbors.  If fewer neighbors than that passed the unknown threshold, so be it.
			final VotingResults votingResults = addUpNeighborVotes(moves); //, populatedTrainingLabels);
			labelWeights = votingResults.getLabelVotes();

			BestLabelPair votingWinners = votingResults.getSubResults(potentialTrainingBins);

			// note the "votes" from each cluster may be fractional (probabilities) but we just summed them all up.

			// now pick the best one
			String predictedLabel = votingWinners.getBestLabel();
			bestVotes = labelWeights.get(predictedLabel);

			voteProportion = labelWeights.getNormalized(predictedLabel);

			// In TACOA, distance == votes, so we don't deal with them separately

			// check that there's not a (near) tie
			if (votingWinners.hasSecondBestLabel())
				{
				String secondBestLabel = votingWinners.getSecondBestLabel();

				double secondBestVotes = labelWeights.get(secondBestLabel);
				assert secondBestVotes <= bestVotes;

				// if the top two scores are too similar...
				secondToBestVoteRatio = secondBestVotes / bestVotes;
				if (secondToBestVoteRatio > bestScoreRatioThreshold)
					{
					throw new NoGoodClusterException();
					}
				}
			else
				{
				secondToBestVoteRatio =
						1e308; // Double.MAX_VALUE; triggers MySQL bug # 21497  // infinity really, but that causes jdbc problems
				}
			}
		catch (NoGoodClusterException e)
			{

			//bestDistance = UNKNOWN_DISTANCE;
			//secondToBestDistanceRatio = 1.0;
			bestVotes = 1e-9;
			voteProportion = 0;
			secondToBestVoteRatio = 1.0;

			tr.incrementUnknown();
			}

		// In TACOA, distance == inverse of votes, so we don't really need to record them separately
		// ** hack: monotonic positive inversion to a distance-like metric (smaller better)
		double bestDistance = 1.0 / bestVotes;
		double secondToBestDistanceRatio = 1.0 / secondToBestVoteRatio;
		tr.addClusterResult(bestDistance, secondToBestDistanceRatio, voteProportion, secondToBestVoteRatio);

		return labelWeights;
		}


/*
  private VotingResults addUpNeighborVotes(TreeMultimap<Double, ClusterMove<T, CentroidCluster<T>>> moves, Set<String> populatedTrainingLabels)
	  {
	  VotingResults result = new VotingResults();

	  for (ClusterMove<T, CentroidCluster<T>> cm : moves.values())
		  {
		  WeightedSet<String> labelsOnThisCluster = cm.bestCluster.getDerivedLabelProbabilities();

		  // in the usual case, labelsOnThisCluster contains a single label with weight 1.
		  // but it might have weights for all the ancestors too

		  // we actually want the vote to count in proportion to the computed "distance", which is really a score (bigger better):
		  result.addVotes(labelsOnThisCluster, cm.bestDistance);

		  //** dunno if this makes any sense here... OK, it allows computing weighted distances per label later
		  for (Map.Entry<String, Double> entry : labelsOnThisCluster.getItemNormalizedMap().entrySet())
			  {
			  final String label = entry.getKey();
			  final Double labelProbability = entry.getValue();

			  result.addContribution(cm, label, labelProbability);
			  }
		  }
	  result.finish(populatedTrainingLabels);
	  return result;
	  }*/
	}
