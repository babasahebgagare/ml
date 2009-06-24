package edu.berkeley.compbio.ml.cluster;

import com.davidsoergel.dsutils.DSArrayUtils;
import com.davidsoergel.dsutils.collections.DSCollectionUtils;
import com.davidsoergel.dsutils.tree.HierarchyNode;
import com.davidsoergel.runutils.HierarchicalTypedPropertyNode;
import com.google.common.base.Function;
import com.google.common.collect.MapMaker;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Encapsulates the probability histograms of test samples classified correctly, incorrectly, or not at all.
 *
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class ClusteringTestResults<L extends Comparable>
	{
// ------------------------------ FIELDS ------------------------------
	//	public Histogram1D correctProbabilities = new FixedWidthHistogram1D(0., 1., .01);		//	public Histogram1D wrongProbabilities = new FixedWidthHistogram1D(0., 1., .01);
	//public List<Double> correctDistances = new ArrayList<Double>();		//public List<Double> wrongDistances = new ArrayList<Double>();

	/**
	 * The computed distance between the sample and the predicted bin
	 */
	private List<Double> computedDistances = new ArrayList<Double>();

	/**
	 * The second-best distance as a proportion of the best distance; 1.0 = tie
	 */
	private List<Double> secondToBestDistanceRatios = new ArrayList<Double>();

	/**
	 * For voting-based classifiers, the proportion of votes that the best label got
	 */
	private final List<Double> bestVoteProportions = new ArrayList<Double>();

	/**
	 * For voting-based classifiers, the second-best number of votes as a proportion of the best votes; 1.0 = tie
	 */
	private List<Double> secondToBestVoteRatios = new ArrayList<Double>();


	//public double correct = 0;
	//public double wrong = 0;
	private int unknown = 0;
	private int numClusters = 0;
	private int shouldHaveBeenUnknown = 0;
	private int shouldNotHaveBeenUnknown = 0;
	private int testSamples;
	private double trainingSeconds;
	private double testingSeconds;
	private double totalTrainingMass = 0;

	private String info;
//	private MultiClassCrossValidationResults crossValidationResults;

	private Map<L, String> friendlyLabelMap;

	public ClusteringTestResults() //final Map<String, String> friendlyLabelMap)
		{
		//this.friendlyLabelMap = friendlyLabelMap;
		}

	public void setFriendlyLabelMap(final Map<L, String> friendlyLabelMap)
		{
		this.friendlyLabelMap = friendlyLabelMap;
		}

	public String getInfo()
		{
		return info;
		}

	public void setInfo(final String info)
		{
		this.info = info;
		}
// --------------------- GETTER / SETTER METHODS ---------------------

	public synchronized int getNumClusters()
		{
		return numClusters;
		}

	public synchronized void setNumClusters(final int numClusters)
		{
		this.numClusters = numClusters;
		}


	public int getShouldHaveBeenUnknown()
		{
		return shouldHaveBeenUnknown;
		}

	public int getShouldNotHaveBeenUnknown()
		{
		return shouldNotHaveBeenUnknown;
		}


	public synchronized int getTestSamples()
		{
		return testSamples;
		}

	public synchronized void setTestSamples(final int i)
		{
		testSamples = i;
		}

	public double getTestingSeconds()
		{
		return testingSeconds;
		}

	public void setTestingSeconds(final double testingSeconds)
		{
		this.testingSeconds = testingSeconds;
		}

	public double getTotalTrainingMass()
		{
		return totalTrainingMass;
		}

	public double getTrainingSeconds()
		{
		return trainingSeconds;
		}

	public void setTrainingSeconds(final double trainingSeconds)
		{
		this.trainingSeconds = trainingSeconds;
		}

	public int getUnknown()
		{
		return unknown;
		}

// -------------------------- OTHER METHODS --------------------------

	public synchronized void addClusterResult(final double bestDistance, final double secondToBestDistanceRatio,
	                                          final double bestVoteProportion, final double secondToBestVoteRatio)
		{
		assert !(Double.isNaN(bestDistance) || Double.isInfinite(bestDistance));
		assert !(Double.isNaN(secondToBestDistanceRatio) || Double.isInfinite(secondToBestDistanceRatio));
		assert !(Double.isNaN(secondToBestVoteRatio) || Double.isInfinite(secondToBestVoteRatio));

		computedDistances.add(bestDistance);
		secondToBestDistanceRatios.add(secondToBestDistanceRatio);
		bestVoteProportions.add(bestVoteProportion);
		secondToBestVoteRatios.add(secondToBestVoteRatio);
		}

	private final Map<String, DistanceBasedMultiClassCrossValidationResults<L>> cvResultMap =
			new MapMaker().makeComputingMap(new Function<String, DistanceBasedMultiClassCrossValidationResults<L>>()
			{
			public DistanceBasedMultiClassCrossValidationResults apply(@Nullable final String from)
				{
				return new DistanceBasedMultiClassCrossValidationResults();
				/*
				DistanceBasedMultiClassCrossValidationResults result = new DistanceBasedMultiClassCrossValidationResults();
				result.setFriendlyLabelMap(friendlyLabelMap);
				return result;*/
				}
			});

	public synchronized void addPredictionResult(final String predictionSetName, final String broadActualLabel,
	                                             final String predictedLabel, final double clusterProb,
	                                             final double broadWrongness, final double detailedWrongness)

		{
		assert !(Double.isNaN(broadWrongness) || Double.isInfinite(broadWrongness));
		assert !(Double.isNaN(detailedWrongness) || Double.isInfinite(detailedWrongness));
		assert !(Double.isNaN(clusterProb) || Double.isInfinite(clusterProb));

		final DistanceBasedMultiClassCrossValidationResults cvResults = cvResultMap.get(predictionSetName);

		cvResults.addSample(broadActualLabel, predictedLabel, clusterProb, broadWrongness, detailedWrongness);
		}

	//public int perfect = 0;

	/**
	 * Normalize the proportions to 1.  Useful for instance if the proportion fields are initially set to raw counts.
	 */		/*public void normalize()
		{
		double total = correct + wrong + unknown;
		correct /= total;
		wrong /= total;
		unknown /= total;
		}*/

	//	public double[] correctPercentages;		//	public double[] wrongPercentages;

	//	public double[] correctDistanceHistogram;		//	public double[] wrongDistanceHistogram;		//	public double[] distanceBinCenters;
	public synchronized void finish()
		{
		// keep track of whether any good predictions are ever made
		if (DSCollectionUtils.allElementsEqual(computedDistances, AbstractClusteringMethod.UNKNOWN_DISTANCE))
			{
			computedDistances = null;
			}

		if (DSCollectionUtils.allElementsEqual(secondToBestDistanceRatios, 0.0))
			{
			secondToBestDistanceRatios = null;
			}
		if (DSCollectionUtils.allElementsEqual(secondToBestVoteRatios, 0.0))
			{
			secondToBestVoteRatios = null;
			}
		if (DSCollectionUtils.allElementsEqual(computedDistances, 1.0))
			{
			computedDistances = null;
			}

		for (final DistanceBasedMultiClassCrossValidationResults cvResults : cvResultMap.values())
			{
			cvResults.finish();
			}

		/*		int[] correctCounts = correctProbabilities.getCounts();
		int[] wrongCounts = wrongProbabilities.getCounts();

		int correctTotal = DSArrayUtils.sum(correctCounts);
		int wrongTotal = DSArrayUtils.sum(wrongCounts);

		int total = correctTotal + wrongTotal;

		correctPercentages = DSArrayUtils.castToDouble(correctCounts);
		wrongPercentages = DSArrayUtils.castToDouble(wrongCounts);

		DSArrayUtils.multiplyBy(correctPercentages, 1. / total);
		DSArrayUtils.multiplyBy(wrongPercentages, 1. / total);

		double[] correctDistancesPrimitive =
				DSArrayUtils.toPrimitive((Double[]) correctDistances.toArray(new Double[0]));
		double[] wrongDistancesPrimitive =
				DSArrayUtils.toPrimitive((Double[]) wrongDistances.toArray(new Double[0]));

		double minDistance =
				Math.min(DSArrayUtils.min(correctDistancesPrimitive), DSArrayUtils.min(wrongDistancesPrimitive));
		double maxDistance =
				Math.max(DSArrayUtils.max(correctDistancesPrimitive), DSArrayUtils.max(wrongDistancesPrimitive));


		double binwidth = (maxDistance - minDistance) / 1000.;

		Histogram1D cHist =
				new FixedWidthHistogram1D(minDistance, maxDistance, binwidth, correctDistancesPrimitive);
		cHist.setTotalcounts(total);
		try
			{
			distanceBinCenters = cHist.getBinCenters();
			}
		catch (StatsException e)
			{
			logger.error(e);
			throw new Error(e);
			}

		correctDistanceHistogram = cHist.getCumulativeFractions();


		FixedWidthHistogram1D wHist =
				new FixedWidthHistogram1D(minDistance, maxDistance, binwidth, wrongDistancesPrimitive);
		wHist.setTotalcounts(total);
		wrongDistanceHistogram = wHist.getCumulativeFractions();
		*/
		}
/*
	public double getAccuracy()
		{
		if (predictionDistances != null && predictionDistances.size() != 0)
			{
			return (double) DSCollectionUtils.countMatches(predictionDistances, new EqualPredicate(0.0))
					/ (double) predictionDistances.size();
			}
		else
			{
			return 0;
			}
		}*/

	public synchronized Double[] getBestVoteProportionsArray()
		{
		return bestVoteProportions == null ? null : bestVoteProportions.toArray(DSArrayUtils.EMPTY_DOUBLE_OBJECT_ARRAY);
		}

	public synchronized Double[] getComputedDistancesArray()
		{
		return computedDistances == null ? null : computedDistances.toArray(DSArrayUtils.EMPTY_DOUBLE_OBJECT_ARRAY);
		}


	public synchronized Double[] getSecondToBestDistanceRatiosArray()
		{
		return secondToBestDistanceRatios == null ? null :
		       secondToBestDistanceRatios.toArray(DSArrayUtils.EMPTY_DOUBLE_OBJECT_ARRAY);
		}

	public synchronized Double[] getSecondToBestVoteRatiosArray()
		{
		return secondToBestVoteRatios == null ? null :
		       secondToBestVoteRatios.toArray(DSArrayUtils.EMPTY_DOUBLE_OBJECT_ARRAY);
		}

	public synchronized void incrementShouldHaveBeenUnknown()
		{
		shouldHaveBeenUnknown++;
		}

	public synchronized void incrementShouldNotHaveBeenUnknown()
		{
		shouldNotHaveBeenUnknown++;
		}


	public synchronized void incrementTotalTrainingMass(final double weightSum)
		{
		totalTrainingMass += weightSum;
		}

	public synchronized void incrementUnknown()
		{
		unknown++;
		}

	/*	public void setCrossValidationResults(MultiClassCrossValidationResults crossValidationResults)
		 {
		 this.crossValidationResults = crossValidationResults;
		 }
 */
	//@Transactional
	public void putResults(final HierarchyNode<HierarchicalTypedPropertyNode<String, Object>, ?> results,
	                       final String labelDistancesName)
		//, Map<String, String> friendlyLabelMap)
		{
		HierarchicalTypedPropertyNode<String, Object> resultsNode = results.getValue();

		resultsNode.addChild("numClusters", getNumClusters());
		//resultsNode.addChild("unknown", getUnknown());
		resultsNode.addChild("shouldHaveBeenUnknown", getShouldHaveBeenUnknown());
		resultsNode.addChild("shouldNotHaveBeenUnknown", getShouldNotHaveBeenUnknown());

		resultsNode.addChild("computedDistances", getComputedDistancesArray());
		resultsNode.addChild("secondToBestDistanceRatios", getSecondToBestDistanceRatiosArray());
		resultsNode.addChild("voteProportions", getBestVoteProportionsArray());
		resultsNode.addChild("secondToBestVoteRatios", getSecondToBestVoteRatiosArray());
//		resultsNode.addChild("labelWithinClusterProbabilities", getLabelWithinClusterProbabilitiesArray());

//		resultsNode.addChild("accuracy", getAccuracy()); // (double) perfect / (double) tr.labelDistances.size());

		resultsNode.addChild("trainingSeconds", getTrainingSeconds());
		resultsNode.addChild("testingSecondsPerSample", getTestingSeconds() / getTestSamples());


		/*		resultsNode.addChild("correctProbabilities", tr.correctPercentages);
				resultsNode.addChild("wrongProbabilities", tr.wrongPercentages);


				resultsNode.addChild("correctDistances", tr.correctDistanceHistogram);
				resultsNode.addChild("wrongDistances", tr.wrongDistanceHistogram);


				resultsNode.addChild("distanceBinCenters", tr.distanceBinCenters);*/

		resultsNode.addChild("unknownCluster", getUnknown());  // as opposed to unknownLabel
		resultsNode.addChild("totalTrainingMass", getTotalTrainingMass());

		resultsNode.addChild("modelInfo", getInfo());

		for (final Map.Entry<String, DistanceBasedMultiClassCrossValidationResults<L>> entry : cvResultMap.entrySet())
			{
			final String predictionLabelsName = entry.getKey();

			//	final List<String> keyPath = new ArrayList<String>();
			//keyPath.add("RESULTS");
			//	keyPath.add(predictionLabelsName);

			HierarchyNode<HierarchicalTypedPropertyNode<String, Object>, ?> childResults = results.newChild();
			childResults.setName(predictionLabelsName);
			HierarchicalTypedPropertyNode<String, Object> childResultsNode = childResults.getValue();
			entry.getValue().putResults(childResultsNode, labelDistancesName, friendlyLabelMap);
			}
		}
	}
