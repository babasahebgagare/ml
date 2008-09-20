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

package edu.berkeley.compbio.ml.cluster.kohonen;

import edu.berkeley.compbio.ml.cluster.AbstractCluster;
import edu.berkeley.compbio.ml.cluster.AdditiveClusterable;


/**
 * @author <a href="mailto:dev.davidsoergel.com">David Soergel</a>
 * @version $Id$
 */

public class KohonenSOMCell<T extends AdditiveClusterable<T>> extends AbstractCluster<T>
	{
	public KohonenSOMCell(int id, T centroid)//DistanceMeasure<T> dm,
		{
		super(id, centroid);//dm
		}

	public boolean recenterByAdding(T point)
		{
		// we don't increment n here, because moving the centroid and actually assigning a sample to this cell are two different things
		centroid.incrementBy(point);
		return true;
		}


	public void recenterByAddingWeighted(T point, double motionFactor)
		{
		// REVIEW Note assumption of an additive statistical model for the centroids
		/*		if (!additiveModel)
		   {
		   centroid.multiplyBy(1 - motionFactor);
		   }*/

		// this is slow because point.times() requires an array copy, since we don't want to modify the original
		//centroid.incrementBy(point.times(motionFactor));

		centroid.incrementByWeighted(point, motionFactor);
		}


	public void recenterByRemovingWeighted(T point, double motionFactor)
		{
		// REVIEW Note assumption of an additive statistical model for the centroids
		/*		if (!additiveModel)
		   {
		   centroid.multiplyBy(1 - motionFactor);
		   }*/
		centroid.decrementByWeighted(point, motionFactor);
		}


	public boolean recenterByRemoving(T point)
		{
		centroid.decrementBy(point);


		return true;
		//throw new NotImplementedException();
		}
	}
