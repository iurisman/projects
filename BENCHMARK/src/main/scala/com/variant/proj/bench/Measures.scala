package com.variant.proj.bench

import scala.collection.JavaConverters._
import scala.collection.concurrent.TrieMap
import java.util.concurrent.ConcurrentLinkedQueue
import scala.collection.mutable.ArrayBuffer
import scala.util.Sorting

/**
 * Accumulate measurement results in memory.
 * Results are accumulated in a concurrent list of 2-element int arrays,  keyed by the op name in a concurrent map.
 * The first elem contains local elapsed and the second elem contains remote elapsed. 
 * Op queues do not have to be pre-allocated: first insert into an operation creates an empty queue.
 * Clearly, this is a race condition, which we choose to tolerate as it's not likely to have a great
 * influence on the averages.
 */
class Measures() {
	
	val map = new TrieMap[String, ConcurrentLinkedQueue[Array[Int]]]()
	
	/**
	 * Add a result of a given operation
	 */
	def add(op: String, localElapsed: Int, remoteElapsed: Int) {
		map.get(op) match {
			case Some(list) => 
				list.add(Array(localElapsed, remoteElapsed))
			case None =>
				val list = new ConcurrentLinkedQueue[Array[Int]]()
				map.put(op, list)
				list.add(Array(localElapsed, remoteElapsed))
		}
	}
	
	/**
	 * Compute the results.
	 * Each element in the returned sequence is a double, whose first element
	 * is the name of the method and the second is a 5-element Int array with the interquartile measurements. Note that we ignore the largest
	 * number as it tends to be way off.
	 */
	def compute: Seq[(String, Array[Int], Array[Int])] = {
			val result = new ArrayBuffer[(String, Array[Int], Array[Int])]()
			
			for ( (op, list) <- map) {
				val localMeasures = list.asScala.map(_(0)).toArray
				val remoteMeasures = list.asScala.map(_(1)).toArray
				Sorting.quickSort(localMeasures)
				Sorting.quickSort(remoteMeasures)
				result.append(
						(op, 
							Array(
								localMeasures(0), 
								localMeasures(localMeasures.length / 4), 
								localMeasures(localMeasures.length / 2), 
								localMeasures(localMeasures.length * 3 / 4),
								// Ignore the largest number, as it tends to be misleadingly large.
								localMeasures(localMeasures.length - 2)
							),
							Array(
								remoteMeasures(0), 
								remoteMeasures(remoteMeasures.length / 4), 
								remoteMeasures(remoteMeasures.length / 2), 
								remoteMeasures(remoteMeasures.length * 3 / 4),
								// Ignore the largest number, as it tends to be misleadingly large.
								remoteMeasures(remoteMeasures.length - 2)
							)
						)
				)		

			}
			
			result
	}
	
}