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

package edu.berkeley.compbio.ml.cluster;

import com.davidsoergel.stats.DissimilarityMeasure;

import java.util.Collection;
import java.util.Set;


/**
 * A clustering method that needs the complete set of samples on hand in order to operate.
 *
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public abstract class BatchClusteringMethod<T extends Clusterable<T>> extends CentroidClusteringMethod<T>
	{


	protected BatchClusteringMethod(DissimilarityMeasure<T> dm, Set<String> potentialTrainingBins,
	                                Set<String> predictLabels, Set<String> leaveOneOutLabels, Set<String> testLabels)
		{
		super(dm, potentialTrainingBins, predictLabels, leaveOneOutLabels, testLabels);
		//	this.leaveOneOut = leaveOneOut;
		}
	// different implementations may prefer different data structures
	//Collection<Clusterable<T>> samples;

	/**
	 * Recompute a set of clusters from the stored samples.
	 */
	public abstract void performClustering();

	/**
	 * Add the given samples to the set to be clustered.
	 *
	 * @param samples a Collection of Clusterable objects.
	 */
	public abstract void addAll(Collection<Clusterable<T>> samples);
	}
