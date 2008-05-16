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

package edu.berkeley.compbio.ml.cluster.hierarchical;

import com.davidsoergel.dsutils.collections.CollectionUtils;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.TreeMultimap;
import edu.berkeley.compbio.ml.cluster.BatchTreeClusteringMethod;
import edu.berkeley.compbio.ml.cluster.Cluster;
import edu.berkeley.compbio.ml.cluster.ClusterException;
import edu.berkeley.compbio.ml.cluster.ClusterMove;
import edu.berkeley.compbio.ml.cluster.Clusterable;
import edu.berkeley.compbio.ml.cluster.HierarchicalCluster;
import edu.berkeley.compbio.ml.cluster.NoGoodClusterException;
import edu.berkeley.compbio.ml.distancemeasure.DistanceMeasure;
import edu.berkeley.compbio.phyloutils.BasicPhylogenyNode;
import edu.berkeley.compbio.phyloutils.LengthWeightHierarchyNode;
import org.apache.commons.lang.NotImplementedException;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * @Author David Soergel
 * @Version 1.0
 */
public class UPGMA<T extends Clusterable<T>> extends BatchTreeClusteringMethod<T>
	{

	private DistanceMeasure<T> distanceMeasure;
	private SymmetricPairwiseDistanceMatrix theActiveNodeDistanceMatrix = new SymmetricPairwiseDistanceMatrix();
	private LengthWeightHierarchyNode<T> theRoot;

	//	private SortedSet<ClusterPair<T>> theClusterPairs;

	public UPGMA(DistanceMeasure<T> distanceMeasure)
		{
		this.distanceMeasure = distanceMeasure;

		//		theClusterPairs = new TreeMap<ClusterPair<T>, Double>();
		}

	public LengthWeightHierarchyNode<T> getTree()
		{
		return theRoot;
		}

	/**
	 * Returns the best cluster without adding the point.  Since in this case every sample is a cluster, just returns the
	 * closest sample.
	 *
	 * @param p                   Point to find the best cluster of
	 * @param secondBestDistances List of second-best distances to add to (just for reporting purposes)
	 */
	public Cluster<T> getBestCluster(T p, List<Double> secondBestDistances)
			throws ClusterException, NoGoodClusterException
		{
		Cluster<T> best = null;
		double bestDistance = Double.MAX_VALUE;

		for (Cluster<T> theCluster : theClusters)
			{
			double distance = distanceMeasure.distanceFromTo(p, theCluster.getCentroid());
			if (distance < bestDistance)
				{
				best = theCluster;
				bestDistance = distance;
				}
			}

		return best;
		}

	/**
	 * Return a ClusterMove object describing the best way to reassign the given point to a new cluster.
	 *
	 * @param p
	 * @return
	 */
	public ClusterMove bestClusterMove(T p) throws NoGoodClusterException
		{
		throw new NotImplementedException("");
		}

	public void performClustering()
		{
		while (theActiveNodeDistanceMatrix.size() > 1)
			{
			// find shortest distance

			NodePair<T> pair = theActiveNodeDistanceMatrix.getClosestPair();// the map is sorted by distance
			Double distance = theActiveNodeDistanceMatrix.getDistance(pair) / 2;

			LengthWeightHierarchyNode<T> a = pair.getNode1();
			LengthWeightHierarchyNode<T> b = pair.getNode2();


			// set the branch lengths

			a.setLength(distance);
			b.setLength(distance);

			// remove the two merged clusters from consideration

			theActiveNodeDistanceMatrix.remove(a);
			theActiveNodeDistanceMatrix.remove(b);

			// create a composite node

			LengthWeightHierarchyNode<T> composite = new BasicPhylogenyNode();
			composite.addChild(a);
			composite.addChild(b);
			composite.setWeight(a.getWeight() + b.getWeight());

			// compute the distance from the composite node to each remaining cluster
			for (LengthWeightHierarchyNode<T> node : theActiveNodeDistanceMatrix.getActiveNodes())
				{
				distance = (a.getWeight() / composite.getWeight()) * theActiveNodeDistanceMatrix.getDistance(a, node)
						+ (b.getWeight() / composite.getWeight()) * theActiveNodeDistanceMatrix.getDistance(b, node);
				theActiveNodeDistanceMatrix.setDistance(node, composite, distance);
				}

			// add the composite node to the active list
			// no longer needed; automatic
			// theActiveNodeDistanceMatrix.add(composite);
			}

		theRoot = theActiveNodeDistanceMatrix.getActiveNodes().iterator().next();
		}

	public void addAll(Collection<Clusterable<T>> samples)
		{
		//theClusters.addAll(samples);
		for (Clusterable<T> sample : samples)
			{
			HierarchicalCluster c = new HierarchicalCluster(sample);
			theActiveNodeDistanceMatrix.addAndComputeDistances(c);
			}
		}

	private class SymmetricPairwiseDistanceMatrix
		{
		// really we wanted a SortedBiMultimap or something, but lacking that, we just store the inverse map explicitly.

		private TreeMultimap<Double, NodePair<T>> distanceToPair = new TreeMultimap<Double, NodePair<T>>();
		private Map<NodePair<T>, Double> pairToDistance = new HashMap<NodePair<T>, Double>();

		//private Set<LengthWeightHierarchyNode<T>> theActiveNodes = new HashSet<LengthWeightHierarchyNode<T>>();
		private Multimap<LengthWeightHierarchyNode<T>, NodePair<T>> nodeToPairs = Multimaps.newHashMultimap();


		/*SymmetricHashMap2D<LengthWeightHierarchyNode<T>, LengthWeightHierarchyNode<T>, Double> theDistanceMatrix =
				new SymmetricHashMap2D<LengthWeightHierarchyNode<T>, LengthWeightHierarchyNode<T>, Double>();*/


		void addAndComputeDistances(LengthWeightHierarchyNode<T> node)
			{
			Set<LengthWeightHierarchyNode<T>> activeNodes =
					new HashSet(nodeToPairs.keySet());// avoid ConcurrentModigfcationException
			for (LengthWeightHierarchyNode<T> theActiveNode : activeNodes)
				{
				Double d = distanceMeasure.distanceFromTo(node.getValue(), theActiveNode.getValue());
				NodePair<T> pair = getOrCreateNodePair(node, theActiveNode);

				distanceToPair.put(d, pair);
				pairToDistance.put(pair, d);
				}
			}

		void setDistance(LengthWeightHierarchyNode<T> node1, LengthWeightHierarchyNode<T> node2, double d)
			{
			setDistance(getOrCreateNodePair(node1, node2), d);
			}

		private NodePair getOrCreateNodePair(LengthWeightHierarchyNode<T> node1, LengthWeightHierarchyNode<T> node2)
			{
			NodePair<T> pair = getNodePair(node1, node2);
			if (pair == null)
				{
				pair = new NodePair(node1, node2);
				nodeToPairs.put(node1, pair);
				nodeToPairs.put(node2, pair);
				}
			return pair;
			}

		private NodePair<T> getNodePair(LengthWeightHierarchyNode<T> node1, LengthWeightHierarchyNode<T> node2)
			{
			return CollectionUtils.intersection(nodeToPairs.get(node1), nodeToPairs.get(node2)).iterator().next();
			}

		private void setDistance(NodePair nodePair, double d)
			{
			Double oldDistance = pairToDistance.get(nodePair);
			if (oldDistance != null)
				{
				pairToDistance.remove(nodePair);
				distanceToPair.remove(oldDistance, nodePair);
				}
			pairToDistance.put(nodePair, d);
			distanceToPair.put(d, nodePair);
			}

		public NodePair<T> getClosestPair()
			{
			return distanceToPair.get(distanceToPair.keySet().first()).first();// distanceToPair is sorted
			}

		public Double getDistance(LengthWeightHierarchyNode<T> node1, LengthWeightHierarchyNode<T> node2)
			{
			return getDistance(getNodePair(node1, node2));
			}

		private Double getDistance(NodePair nodePair)
			{
			return pairToDistance.get(nodePair);
			}

		public void remove(LengthWeightHierarchyNode<T> b)
			{
			for (NodePair<T> pair : nodeToPairs.get(b))
				{
				Double oldDistance = pairToDistance.remove(pair);
				distanceToPair.remove(oldDistance, pair);
				}
			nodeToPairs.removeAll(b);
			}

		public Set<LengthWeightHierarchyNode<T>> getActiveNodes()
			{
			return nodeToPairs.keySet();
			}

		public int size()
			{
			return nodeToPairs.keySet().size();
			}
		}

	/**
	 * Represent a pair of nodes, guaranteeing that node1 <= node2 for the sake of symmetry
	 */
	private class NodePair<T extends Clusterable<T>>
		{
		private LengthWeightHierarchyNode<T> node1;
		private LengthWeightHierarchyNode<T> node2;

		private NodePair(LengthWeightHierarchyNode<T> node1, LengthWeightHierarchyNode<T> node2)
			{
			if (node1.getValue().hashCode() <= node2.getValue().hashCode())
				//if (node1.getValue().compareTo(node2.getValue()) <= 0)
				{
				this.node1 = node1;
				this.node2 = node2;
				}
			else
				{
				this.node1 = node2;
				this.node2 = node1;
				}
			}

		public boolean equals(Object o)
			{
			if (this == o)
				{
				return true;
				}
			if (!(o instanceof NodePair))
				{
				return false;
				}

			NodePair nodePair = (NodePair) o;

			if (node1 != null ? !node1.equals(nodePair.node1) : nodePair.node1 != null)
				{
				return false;
				}
			if (node2 != null ? !node2.equals(nodePair.node2) : nodePair.node2 != null)
				{
				return false;
				}

			return true;
			}

		public int hashCode()
			{
			int result;
			result = (node1 != null ? node1.hashCode() : 0);
			result = 31 * result + (node2 != null ? node2.hashCode() : 0);
			return result;
			}

		public LengthWeightHierarchyNode<T> getNode1()
			{
			return node1;
			}

		public LengthWeightHierarchyNode<T> getNode2()
			{
			return node2;
			}
		}
	}