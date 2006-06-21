package edu.berkeley.compbio.ml.cluster;

import org.apache.log4j.Logger;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Date;

/**
 * @author lorax
 * @version 1.0
 */
public abstract class OnlineClusteringMethod<T extends Clusterable<T>>
	{
	private static Logger logger = Logger.getLogger(OnlineClusteringMethod.class);
	//private Iterator<T> theDataPointProvider;

	List<Cluster<T>> theClusters = new ArrayList<Cluster<T>>();

	/*public OnlineClustering(Iterator<T> vp)
		{
		theDataPointProvider = vp;
		}
*/

	public abstract void add(T v);
	public abstract void addAndRecenter(T v);
	//public abstract void reassign(T v);

	public void runOnce(Iterator<T> theDataPointProvider)
		{
		int c = 0;
		Date starttime = new Date();
		while (theDataPointProvider.hasNext())
			{
			addAndRecenter(theDataPointProvider.next());
			if(c++ % 10 == 0)
				{

				Date endtime = new Date();
				double realtime = (endtime.getTime() - starttime.getTime()) / (double) 1000;

				logger.info("Processed " + c + " data points in " + realtime + " seconds; average " + (c / realtime) + " points/sec");
				}
			}
		}

	public void run(Iterator<T> theDataPointProvider, int steps)
		{
		for (int i = 0; i < steps; i++)
			{
			if (!theDataPointProvider.hasNext())
				{
				rerunExisting(steps - i);
				return;
				}
			addAndRecenter(theDataPointProvider.next());
			}
		}


	public void reassignAll()
		{
			for (Cluster<T> c : theClusters)
				{
				for (T t : new HashSet<T>(c))
					{
					c.remove(t);
					add(t);
					}
				}

		}

	private void rerunExisting(int steps)
		{
		// going through these in cluster order sucks...
		while (steps > 0)
			{
			for (Cluster<T> c : theClusters)
				{
				for (T t : new HashSet<T>(c))
					{
					if (c.size() == 1)
						{
						steps--;
						continue;
						}
					c.removeAndRecenter(t);
					addAndRecenter(t);
					if (--steps < 0)
						{
						return;
						}
					}
				}
			}
		}

	public List<Cluster<T>> getClusters()
		{
		return theClusters;
		}

	public void writeTextToStream(OutputStream out)
		{

		PrintWriter p = new PrintWriter(out);

		for (Cluster<T> c : theClusters)
			{
			p.println("<cluster id=\"" + c.getId() + "\" centroid=\"" + c.getCentroid() + "\">");
			for (T t : c)
				{
				p.println("\t" + t);
				}
			p.println("</cluster>");
			}
		p.flush();
		}
	}
