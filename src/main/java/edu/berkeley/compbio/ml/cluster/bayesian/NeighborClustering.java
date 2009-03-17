package edu.berkeley.compbio.ml.cluster.bayesian;

import com.davidsoergel.stats.DissimilarityMeasure;
import edu.berkeley.compbio.ml.cluster.AdditiveClusterable;
import edu.berkeley.compbio.ml.cluster.CentroidCluster;
import edu.berkeley.compbio.ml.cluster.ClusterException;
import edu.berkeley.compbio.ml.cluster.ClusterMove;
import edu.berkeley.compbio.ml.cluster.NoGoodClusterException;
import edu.berkeley.compbio.ml.cluster.OnlineClusteringMethod;

import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public abstract class NeighborClustering<T extends AdditiveClusterable<T>>
		extends OnlineClusteringMethod<T, CentroidCluster<T>>
	{

	protected double unknownDistanceThreshold;

	protected Map<CentroidCluster, Double> priors;
	protected boolean leaveOneOut;

	public NeighborClustering(DissimilarityMeasure<T> dm, double unknownDistanceThreshold, boolean leaveOneOut)
		{
		super(dm);
		this.unknownDistanceThreshold = unknownDistanceThreshold;
		this.leaveOneOut = leaveOneOut;
		}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean add(T p, List<Double> secondBestDistances) throws ClusterException, NoGoodClusterException
		{
		ClusterMove best = bestClusterMove(p);
		secondBestDistances.add(best.secondBestDistance);
		best.bestCluster.add(p);
		return true;
		}
	}
